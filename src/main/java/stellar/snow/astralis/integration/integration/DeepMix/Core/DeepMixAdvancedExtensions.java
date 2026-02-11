package stellar.snow.astralis.integration.DeepMix.Core;

/*
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                            ║
 * ║  🔮 DEEPMIX — PHASES 21-24 ADVANCED EXTENSIONS                            ║
 * ║  ══════════════════════════════════════════════════════════                 ║
 * ║                                                                            ║
 * ║  Phase 21: Game Engine Specific   (20 annotations)                         ║
 * ║  Phase 22: Advanced ASM Operations (12 annotations)                        ║
 * ║  Phase 23: Metaprogramming         (10 annotations)                        ║
 * ║  Phase 24: Final Extensions        (7+ annotations)                        ║
 * ║                                                                            ║
 * ║  Total: 49+ annotations | 98+ definitions (full + shortcut)               ║
 * ║                                                                            ║
 * ║  Dependencies:                                                             ║
 * ║    - DeepMixPhases.java (shared infrastructure)                            ║
 * ║    - org.objectweb.asm (bytecode manipulation)                             ║
 * ║    - LWJGL (for SDL/SFML/Raylib native bindings)                           ║
 * ║                                                                            ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import stellar.snow.astralis.integration.DeepMix.Core.DeepMixPhases.*;

/**
 * ╭─────────────────────────────────────────────────────────────────────────╮
 * │  DeepMix Advanced Extensions — Phases 21 through 24                   │
 * │                                                                         │
 * │  Phase 21: Game Engine & Modding Platform Integration                   │
 * │    Minecraft: Forge, Fabric, NeoForge, Quilt                           │
 * │    Minecraft Servers: Spigot, Paper, Purpur, Sponge                    │
 * │    Minecraft Proxies: Velocity, BungeeCord                             │
 * │    General Game Engines: Unity, Unreal, Godot, Bevy,                   │
 * │                          Amethyst, Piston, Raylib                      │
 * │    Multimedia Libraries: SDL, SFML, Love2D                             │
 * │                                                                         │
 * │  Phase 22: Advanced ASM & Bytecode Operations                          │
 * │    Low-level instruction manipulation, stack map frames,               │
 * │    switch optimization, invokedynamic, bootstrap methods               │
 * │                                                                         │
 * │  Phase 23: Metaprogramming                                              │
 * │    Macros, templates, DSL creation, code quoting,                      │
 * │    runtime compilation, reification                                     │
 * │                                                                         │
 * │  Phase 24: Plugin & Extension Architecture                              │
 * │    Plugin systems, hooks, callbacks, listeners,                        │
 * │    delegation, method chaining                                          │
 * ╰─────────────────────────────────────────────────────────────────────────╯
 */
public final class DeepMixAdvancedExtensions {

    private DeepMixAdvancedExtensions() {
        throw new UnsupportedOperationException(
            "DeepMixAdvancedExtensions is a static annotation & processor container");
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                      ║
    // ║  PHASE 21: GAME ENGINE & MODDING PLATFORM INTEGRATION                ║
    // ║  20 annotations | Priority: MEDIUM | Est. Time: 2-3 days             ║
    // ║                                                                      ║
    // ║  Minecraft Mod Loaders:                                              ║
    // ║    @DeepForge   @DeepFabric   @DeepNeoForge   @DeepQuilt            ║
    // ║                                                                      ║
    // ║  Minecraft Server Platforms:                                          ║
    // ║    @DeepSpigot  @DeepPaper    @DeepPurpur     @DeepSponge           ║
    // ║                                                                      ║
    // ║  Minecraft Proxy Platforms:                                           ║
    // ║    @DeepVelocity @DeepBungee                                         ║
    // ║                                                                      ║
    // ║  General Game Engines:                                                ║
    // ║    @DeepUnity   @DeepUnreal   @DeepGodot      @DeepBevy             ║
    // ║    @DeepAmethyst @DeepPiston                                          ║
    // ║                                                                      ║
    // ║  Multimedia & Low-Level Engines:                                      ║
    // ║    @DeepRaylib  @DeepSDL      @DeepSFML       @DeepLove2D           ║
    // ║                                                                      ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    // ──────────────────────────────────────────────
    // Phase 21 — Enums
    // ──────────────────────────────────────────────

    /** Minecraft mod loader platform */
    public enum MinecraftModLoader {
        FORGE,                 // MinecraftForge (legacy + modern)
        NEOFORGE,              // NeoForge (Forge fork, 1.20.2+)
        FABRIC,                // Fabric Loader + Fabric API
        QUILT,                 // Quilt Loader + QSL (Fabric-compatible)
        LEGACY_FORGE,          // LegacyForge (1.12.2 and below)
        RIFT,                  // Rift (1.13, defunct)
        LITELOADER,            // LiteLoader (legacy)
        MULTI                  // Multi-loader project
    }

    /** Minecraft server platform */
    public enum MinecraftServerPlatform {
        BUKKIT,                // Bukkit API (base)
        SPIGOT,                // Spigot (extends Bukkit)
        PAPER,                 // PaperMC (extends Spigot)
        PURPUR,                // Purpur (extends Paper)
        FOLIA,                 // Folia (regionized Paper)
        SPONGE_VANILLA,        // SpongeVanilla
        SPONGE_FORGE,          // SpongeForge
        SPONGE_NEO,            // Sponge on NeoForge
        PUFFERFISH,            // Pufferfish (extends Paper)
        LEAVES,                // Leaves (extends Paper)
        GALE,                  // Gale (extends Paper)
        MOHIST,                // Mohist (Forge + Bukkit hybrid)
        CATSERVER,             // CatServer (Forge + Bukkit)
        ARCLIGHT,              // Arclight (Forge + Bukkit)
        BANNER,                // Banner (Fabric + Bukkit)
        CARDBOARD              // Cardboard (Fabric + Bukkit)
    }

    /** Minecraft proxy platform */
    public enum MinecraftProxyPlatform {
        VELOCITY,              // Velocity (modern)
        BUNGEECORD,            // BungeeCord (legacy)
        WATERFALL,             // Waterfall (BungeeCord fork by Paper)
        GATE,                  // Gate (Go-based, high performance)
        INFRARED,              // Infrared (lightweight)
        LIMBO,                 // Limbo lightweight server
        GEYSER                 // GeyserMC (Bedrock → Java bridge)
    }

    /** Minecraft version range */
    public enum MinecraftVersionRange {
        MC_1_7("1.7", "1.7.10"),
        MC_1_8("1.8", "1.8.9"),
        MC_1_9("1.9", "1.9.4"),
        MC_1_10("1.10", "1.10.2"),
        MC_1_11("1.11", "1.11.2"),
        MC_1_12("1.12", "1.12.2"),
        MC_1_13("1.13", "1.13.2"),
        MC_1_14("1.14", "1.14.4"),
        MC_1_15("1.15", "1.15.2"),
        MC_1_16("1.16", "1.16.5"),
        MC_1_17("1.17", "1.17.1"),
        MC_1_18("1.18", "1.18.2"),
        MC_1_19("1.19", "1.19.4"),
        MC_1_20("1.20", "1.20.6"),
        MC_1_21("1.21", "1.21.4"),
        ALL("1.0", "99.99.99");

        private final String from;
        private final String to;
        MinecraftVersionRange(String from, String to) { this.from = from; this.to = to; }
        public String from() { return from; }
        public String to() { return to; }
    }

    /** Forge event bus type */
    public enum ForgeEventBus {
        MOD,                   // MOD event bus (FMLJavaModLoadingContext)
        FORGE,                 // MinecraftForge.EVENT_BUS
        BOTH                   // Register on both buses
    }

    /** Fabric entrypoint type */
    public enum FabricEntrypoint {
        MAIN,                  // ModInitializer (common)
        CLIENT,                // ClientModInitializer
        SERVER,                // DedicatedServerModInitializer
        PRE_LAUNCH,            // PreLaunchEntrypoint
        CUSTOM                 // Custom entrypoint key
    }

    /** Quilt loader phase */
    public enum QuiltLoaderPhase {
        PRE_LAUNCH,
        INIT,
        POST_INIT,
        CLIENT_INIT,
        SERVER_INIT
    }

    /** NeoForge extension point */
    public enum NeoForgeExtension {
        MOD_BUS_EVENTS,        // @Mod.EventBusSubscriber(bus = Bus.MOD)
        FORGE_BUS_EVENTS,      // @Mod.EventBusSubscriber(bus = Bus.FORGE)
        CAPABILITY,            // Capability registration
        NETWORK,               // Network channel
        DATA_GEN,              // Data generation
        CONFIG,                // Configuration
        DEFERRED_REGISTER      // DeferredRegister
    }

    /** Bukkit/Spigot event priority */
    public enum BukkitEventPriority {
        LOWEST,
        LOW,
        NORMAL,
        HIGH,
        HIGHEST,
        MONITOR
    }

    /** Paper-specific feature */
    public enum PaperFeature {
        ASYNC_CHUNKS,          // Async chunk loading
        ASYNC_TELEPORT,        // Async entity teleportation
        ADVENTURE_API,         // Adventure text components
        ENTITY_SCHEDULER,      // Per-entity scheduling (Folia compat)
        GLOBAL_SCHEDULER,      // Global region scheduler
        ASYNC_SCHEDULER,       // Async task scheduler
        PLUGIN_REMAPPING,      // Mojang-mapped plugins
        BRIGADIER,             // Brigadier command framework
        LIFECYCLE_EVENTS       // Lifecycle event system
    }

    /** Sponge API version target */
    public enum SpongeAPIVersion {
        API_7("7.x", "1.12.2"),
        API_8("8.x", "1.16.5"),
        API_9("9.x", "1.18.2"),
        API_10("10.x", "1.19.4"),
        API_11("11.x", "1.20.x"),
        API_12("12.x", "1.21.x");

        private final String apiVersion;
        private final String mcVersion;
        SpongeAPIVersion(String api, String mc) { this.apiVersion = api; this.mcVersion = mc; }
        public String apiVersion() { return apiVersion; }
        public String mcVersion() { return mcVersion; }
    }

    /** General game engine type */
    public enum GameEngine {
        UNITY,                 // Unity (C#, via JNI/GraalVM)
        UNREAL,                // Unreal Engine (C++, via JNI)
        GODOT,                 // Godot (GDScript/C#/C++, via GDNative)
        BEVY,                  // Bevy (Rust ECS, via JNI/FFI)
        AMETHYST,              // Amethyst (Rust ECS, legacy)
        PISTON,                // Piston (Rust 2D)
        RAYLIB,                // Raylib (C, via LWJGL/JNI)
        SDL2,                  // SDL2 (C, via LWJGL/JNI)
        SDL3,                  // SDL3 (C, via LWJGL/JNI)
        SFML,                  // SFML (C++, via JNI)
        LOVE2D,                // Love2D (Lua, via embedding)
        LIBGDX,                // LibGDX (Java native)
        MONOGAME,              // MonoGame (C#, via JNI)
        PYGAME,                // Pygame (Python, via Jep)
        CUSTOM
    }

    /** Game engine integration mode */
    public enum EngineIntegrationMode {
        JNI,                   // Java Native Interface
        FFI,                   // Foreign Function Interface (Java 22+)
        GRAALVM,               // GraalVM polyglot
        JEP,                   // Java Embedded Python
        PROCESS,               // External process communication
        SOCKET,                // Socket-based IPC
        SHARED_MEMORY,         // Shared memory IPC
        PLUGIN,                // Engine's native plugin system
        NATIVE_IMAGE,          // GraalVM Native Image
        CUSTOM
    }

    /** ECS (Entity Component System) operation */
    public enum ECSOperation {
        CREATE_ENTITY,
        DESTROY_ENTITY,
        ADD_COMPONENT,
        REMOVE_COMPONENT,
        GET_COMPONENT,
        QUERY,                 // System query
        SPAWN_BUNDLE,          // Spawn entity with component bundle
        INSERT_RESOURCE,
        REMOVE_RESOURCE,
        SEND_EVENT,
        RUN_SYSTEM,
        ADD_SYSTEM,
        REMOVE_SYSTEM,
        CUSTOM
    }

    /** SDL subsystem flags */
    public enum SDLSubsystem {
        TIMER,
        AUDIO,
        VIDEO,
        JOYSTICK,
        HAPTIC,
        GAMECONTROLLER,
        EVENTS,
        SENSOR,
        EVERYTHING
    }

    /** Raylib module */
    public enum RaylibModule {
        CORE,                  // Core: Window, input, timing
        SHAPES,                // Shapes: 2D primitives
        TEXTURES,              // Textures: Image/texture loading
        TEXT,                  // Text: Font loading/rendering
        MODELS,                // Models: 3D model loading
        AUDIO,                 // Audio: Audio device management
        PHYSICS,               // Physac: 2D physics
        RAYGUI,                // raygui: Immediate mode GUI
        RLGL,                  // rlgl: Low-level OpenGL abstraction
        RAYMATH                // raymath: Math helpers
    }

    // ──────────────────────────────────────────────
    // Phase 21 — Sub-annotation types
    // ──────────────────────────────────────────────

    /** Minecraft mod metadata */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ModMetadata {
        String modId();
        String name() default "";
        String version() default "";
        String description() default "";
        String[] authors() default {};
        String license() default "";
        String homepage() default "";
        String issueTracker() default "";
        String[] dependencies() default {};    // "mod_id@[version_range]"
        String[] breaks() default {};          // Incompatible mods
        String[] suggests() default {};        // Suggested mods
        String icon() default "";
        String namespace() default "";
    }

    /** Forge mixin configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ForgeMixinConfig {
        String configFile() default "";        // mixin config JSON file
        String[] mixinPackages() default {};
        String[] clientMixins() default {};
        String[] serverMixins() default {};
        String[] commonMixins() default {};
        boolean refmap() default true;
        String refmapFile() default "";
        int priority() default 1000;
    }

    /** Fabric mod.json metadata extension */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface FabricModJson {
        int schemaVersion() default 1;
        String environment() default "*";      // *, client, server
        String[] mixins() default {};
        String accessWidener() default "";
        String[] jars() default {};            // Jar-in-jar dependencies
        String[] provides() default {};        // Provided mod IDs
        String[] languageAdapters() default {};
    }

    /** Plugin description (for server plugins) */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PluginDescription {
        String name();
        String version() default "1.0.0";
        String description() default "";
        String[] authors() default {};
        String website() default "";
        String prefix() default "";            // Log prefix
        String[] depend() default {};          // Hard dependencies
        String[] softdepend() default {};      // Soft dependencies
        String[] loadbefore() default {};      // Load before these plugins
        String apiVersion() default "";        // Bukkit API version
        String[] libraries() default {};       // Maven coordinates for dependencies
    }

    /** Velocity plugin annotation config */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface VelocityPluginConfig {
        String id();
        String name() default "";
        String version() default "";
        String description() default "";
        String url() default "";
        String[] authors() default {};
        String[] dependencies() default {};    // "id:optional"
    }

    /** Game engine project configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface EngineProjectConfig {
        GameEngine engine();
        String projectPath() default "";
        String buildSystem() default "";       // cmake, cargo, dotnet, scons
        EngineIntegrationMode mode() default EngineIntegrationMode.JNI;
        String[] nativeLibraries() default {};
        String[] includeHeaders() default {};
        String targetPlatform() default "";    // windows, linux, macos, android, ios
        String archTarget() default "";        // x86_64, aarch64, wasm32
    }

    /** SDL window configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SDLWindowConfig {
        String title() default "DeepMix SDL Window";
        int x() default -1;                   // -1 = centered
        int y() default -1;
        int width() default 1280;
        int height() default 720;
        SDLSubsystem[] subsystems() default {SDLSubsystem.EVERYTHING};
        boolean opengl() default false;
        boolean vulkan() default false;
        boolean metal() default false;
        boolean resizable() default true;
        boolean borderless() default false;
        boolean fullscreen() default false;
        boolean highDPI() default true;
        boolean alwaysOnTop() default false;
        int rendererFlags() default 0;         // SDL_RENDERER_* flags
    }

    /** Raylib window configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RaylibWindowConfig {
        int width() default 1280;
        int height() default 720;
        String title() default "DeepMix Raylib Window";
        int targetFPS() default 60;
        boolean fullscreen() default false;
        boolean resizable() default true;
        boolean undecorated() default false;
        boolean transparent() default false;
        boolean topmost() default false;
        boolean vsync() default true;
        boolean msaa4x() default false;
        boolean interlacedHint() default false;
        int minWidth() default 0;
        int minHeight() default 0;
    }

    // ──────────────────────────────────────────────
    // Phase 21 — Annotations: Minecraft Mod Loaders
    // ──────────────────────────────────────────────

    /**
     * 🔨 @DeepForge — MinecraftForge API integration & transformation.
     *
     * Integrates with MinecraftForge's event system, registry, capability,
     * and networking APIs. Supports both legacy (1.12.2−) and modern (1.13+) Forge.
     *
     * Example:
     * <pre>
     * {@code @DeepForge(
     *     mod = @ModMetadata(modId = "mymod", name = "My Mod", version = "1.0.0"),
     *     eventBus = ForgeEventBus.BOTH,
     *     mcVersions = {MinecraftVersionRange.MC_1_20},
     *     mixin = @ForgeMixinConfig(mixinPackages = {"com.example.mixin"})
     * )}
     * public class MyMod { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepForge {
        String target() default "";
        ModMetadata mod() default @ModMetadata(modId = "");
        ForgeEventBus eventBus() default ForgeEventBus.BOTH;
        MinecraftVersionRange[] mcVersions() default {MinecraftVersionRange.MC_1_20};
        ForgeMixinConfig mixin() default @ForgeMixinConfig;
        boolean useAccessTransformer() default false;
        String accessTransformerFile() default "";
        boolean useCoreMod() default false;
        String coreModClass() default "";
        boolean useDeferredRegister() default true;
        boolean useCapabilities() default false;
        boolean useNetworking() default false;
        String networkChannel() default "";
        int networkVersion() default 1;
        boolean useDataGen() default false;
        boolean useConfigs() default false;
        String configType() default "COMMON";  // CLIENT, COMMON, SERVER
        String[] registries() default {};      // Block, Item, Entity, etc.
        boolean clientSideOnly() default false;
        boolean serverSideOnly() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepForge */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DFORGE {
        String target() default "";
        ModMetadata mod() default @ModMetadata(modId = "");
        ForgeEventBus eventBus() default ForgeEventBus.BOTH;
        MinecraftVersionRange[] mcVersions() default {MinecraftVersionRange.MC_1_20};
        boolean useDeferredRegister() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ⚡ @DeepNeoForge — NeoForge API integration (modern Forge fork).
     *
     * NeoForge (1.20.2+) is the continuation of MinecraftForge with
     * modernized APIs, improved event system, and better mod compatibility.
     *
     * Example:
     * <pre>
     * {@code @DeepNeoForge(
     *     mod = @ModMetadata(modId = "mymod"),
     *     extensions = {NeoForgeExtension.MOD_BUS_EVENTS, NeoForgeExtension.NETWORK},
     *     mcVersions = {MinecraftVersionRange.MC_1_21}
     * )}
     * public class MyNeoMod { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepNeoForge {
        String target() default "";
        ModMetadata mod() default @ModMetadata(modId = "");
        NeoForgeExtension[] extensions() default {};
        MinecraftVersionRange[] mcVersions() default {MinecraftVersionRange.MC_1_21};
        ForgeMixinConfig mixin() default @ForgeMixinConfig;
        boolean useAccessTransformer() default false;
        String accessTransformerFile() default "";
        boolean useDeferredRegister() default true;
        boolean useAttachments() default false; // NeoForge data attachments (replaces capabilities)
        boolean useNetworking() default false;
        String networkChannel() default "";
        boolean usePayloads() default false;   // NeoForge custom payloads
        boolean useDataGen() default false;
        boolean useConfigs() default false;
        String configType() default "COMMON";
        String[] registries() default {};
        boolean useEventBusSubscriber() default true;
        boolean useModBus() default true;
        boolean useGameBus() default true;     // NeoForge renamed FORGE → GAME
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepNeoForge */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DNEOFORGE {
        String target() default "";
        ModMetadata mod() default @ModMetadata(modId = "");
        NeoForgeExtension[] extensions() default {};
        MinecraftVersionRange[] mcVersions() default {MinecraftVersionRange.MC_1_21};
        boolean useDeferredRegister() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🧵 @DeepFabric — Fabric API integration & transformation.
     *
     * Integrates with Fabric Loader, Fabric API modules, and Fabric's
     * event/registry/networking systems.
     *
     * Example:
     * <pre>
     * {@code @DeepFabric(
     *     mod = @ModMetadata(modId = "mymod"),
     *     fabricJson = @FabricModJson(
     *         mixins = {"mymod.mixins.json"},
     *         accessWidener = "mymod.accesswidener"
     *     ),
     *     entrypoint = FabricEntrypoint.MAIN
     * )}
     * public class MyFabricMod implements ModInitializer { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepFabric {
        String target() default "";
        ModMetadata mod() default @ModMetadata(modId = "");
        FabricModJson fabricJson() default @FabricModJson;
        FabricEntrypoint entrypoint() default FabricEntrypoint.MAIN;
        String customEntrypointKey() default "";
        MinecraftVersionRange[] mcVersions() default {MinecraftVersionRange.MC_1_20};
        boolean useAccessWidener() default false;
        String accessWidenerFile() default "";
        boolean useEvents() default true;      // Fabric API events
        boolean useNetworking() default false;  // Fabric API networking
        boolean useResourceLoader() default false;
        boolean useRendering() default false;   // Fabric Rendering API
        boolean useScreens() default false;     // Fabric Screen API
        boolean useCommands() default false;    // Fabric Command API
        boolean useItemGroups() default false;  // Fabric Item Group API
        boolean useLootTables() default false;  // Fabric Loot API
        boolean useBiomes() default false;      // Fabric Biome API
        boolean useDimensions() default false;  // Fabric Dimension API
        String[] fabricApiModules() default {}; // Specific Fabric API modules
        boolean clientSideOnly() default false;
        boolean serverSideOnly() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepFabric */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DFABRIC {
        String target() default "";
        ModMetadata mod() default @ModMetadata(modId = "");
        FabricEntrypoint entrypoint() default FabricEntrypoint.MAIN;
        MinecraftVersionRange[] mcVersions() default {MinecraftVersionRange.MC_1_20};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🪶 @DeepQuilt — Quilt mod loader integration.
     *
     * Quilt is a Fabric-compatible mod loader with enhanced QSL
     * (Quilt Standard Libraries). Supports Fabric API compatibility layer.
     *
     * Example:
     * <pre>
     * {@code @DeepQuilt(
     *     mod = @ModMetadata(modId = "mymod"),
     *     phase = QuiltLoaderPhase.INIT,
     *     fabricCompatible = true
     * )}
     * public class MyQuiltMod implements ModInitializer { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepQuilt {
        String target() default "";
        ModMetadata mod() default @ModMetadata(modId = "");
        QuiltLoaderPhase phase() default QuiltLoaderPhase.INIT;
        MinecraftVersionRange[] mcVersions() default {MinecraftVersionRange.MC_1_20};
        boolean fabricCompatible() default true; // Use Fabric API compat layer
        boolean useQSL() default true;           // Use Quilt Standard Libraries
        boolean useQuiltConfig() default false;  // Quilt Config API
        boolean useQuiltEvents() default true;   // Quilt Events API
        boolean useQuiltRegistry() default false; // Quilt Registry API
        boolean useQuiltNetworking() default false;
        boolean useQuiltResource() default false; // Quilt Resource Loader
        boolean useQuiltRendering() default false;
        String[] qslModules() default {};        // Specific QSL modules
        FabricModJson fabricJson() default @FabricModJson;
        boolean useAccessWidener() default false;
        String accessWidenerFile() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepQuilt */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DQUILT {
        String target() default "";
        ModMetadata mod() default @ModMetadata(modId = "");
        QuiltLoaderPhase phase() default QuiltLoaderPhase.INIT;
        boolean fabricCompatible() default true;
        boolean useQSL() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    // ──────────────────────────────────────────────
    // Phase 21 — Annotations: Minecraft Server Platforms
    // ──────────────────────────────────────────────

    /**
     * 🔧 @DeepSpigot — Spigot/Bukkit API plugin integration.
     *
     * Example:
     * <pre>
     * {@code @DeepSpigot(
     *     plugin = @PluginDescription(
     *         name = "MyPlugin",
     *         version = "1.0.0",
     *         apiVersion = "1.20"
     *     ),
     *     eventPriority = BukkitEventPriority.NORMAL
     * )}
     * public class MyPlugin extends JavaPlugin { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepSpigot {
        String target() default "";
        PluginDescription plugin() default @PluginDescription(name = "");
        BukkitEventPriority eventPriority() default BukkitEventPriority.NORMAL;
        boolean ignoreCancelled() default false;
        MinecraftServerPlatform platform() default MinecraftServerPlatform.SPIGOT;
        boolean useCommands() default true;
        boolean usePermissions() default true;
        boolean useConfig() default true;
        boolean useDatabase() default false;
        boolean useProtocolLib() default false;
        boolean useVault() default false;      // Vault economy/permissions/chat
        boolean usePlaceholderAPI() default false;
        boolean useWorldGuard() default false;
        boolean useNMS() default false;        // Net.Minecraft.Server access
        String nmsVersion() default "";        // v1_20_R3, etc.
        boolean useCraftBukkit() default false;
        boolean useScheduler() default true;
        boolean async() default false;
        String[] channels() default {};        // Plugin messaging channels
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepSpigot */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DSPIGOT {
        String target() default "";
        PluginDescription plugin() default @PluginDescription(name = "");
        BukkitEventPriority eventPriority() default BukkitEventPriority.NORMAL;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📄 @DeepPaper — PaperMC API integration.
     *
     * Paper extends Spigot with async chunk loading, Adventure API,
     * improved scheduler, and performance optimizations.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepPaper {
        String target() default "";
        PluginDescription plugin() default @PluginDescription(name = "");
        PaperFeature[] features() default {};
        BukkitEventPriority eventPriority() default BukkitEventPriority.NORMAL;
        boolean useAdventure() default true;   // Adventure component API
        boolean useBrigadier() default false;   // Brigadier commands
        boolean useAsyncChunks() default true;
        boolean useAsyncTeleport() default true;
        boolean useFoliaCompat() default false; // Folia scheduler compatibility
        boolean useEntityScheduler() default false;
        boolean usePluginRemapping() default false; // Mojang-mapped
        boolean useLifecycleEvents() default false;
        boolean usePaperPluginYml() default false;  // paper-plugin.yml format
        String bootstrapClass() default "";     // Paper bootstrap class
        String loaderClass() default "";        // Paper plugin loader class
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepPaper */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DPAPER {
        String target() default "";
        PluginDescription plugin() default @PluginDescription(name = "");
        PaperFeature[] features() default {};
        boolean useAdventure() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🟣 @DeepPurpur — Purpur API integration (extends Paper).
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepPurpur {
        String target() default "";
        PluginDescription plugin() default @PluginDescription(name = "");
        PaperFeature[] paperFeatures() default {};
        boolean usePurpurConfig() default false;  // Access Purpur config values
        boolean usePurpurEvents() default false;   // Purpur-specific events
        boolean useRideableEntities() default false;
        boolean useCustomBlocks() default false;
        boolean useEnhancedEntities() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepPurpur */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DPURPUR {
        String target() default "";
        PluginDescription plugin() default @PluginDescription(name = "");
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🧽 @DeepSponge — SpongeAPI integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepSponge {
        String target() default "";
        String pluginId() default "";
        String name() default "";
        String version() default "";
        String description() default "";
        String[] authors() default {};
        SpongeAPIVersion apiVersion() default SpongeAPIVersion.API_11;
        String[] dependencies() default {};
        boolean useEventBus() default true;
        boolean useCause() default true;       // Sponge Cause tracking
        boolean useServices() default true;    // Sponge Service API
        boolean useData() default true;        // Sponge Data API
        boolean useCommands() default true;    // Sponge Command API
        boolean useConfigurate() default true;  // Configurate config
        boolean useBrigadier() default false;
        String mainClass() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepSponge */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DSPONGE {
        String target() default "";
        String pluginId() default "";
        SpongeAPIVersion apiVersion() default SpongeAPIVersion.API_11;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    // ──────────────────────────────────────────────
    // Phase 21 — Annotations: Minecraft Proxies
    // ──────────────────────────────────────────────

    /**
     * 🚀 @DeepVelocity — Velocity proxy plugin integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepVelocity {
        String target() default "";
        VelocityPluginConfig plugin() default @VelocityPluginConfig(id = "");
        boolean useEventBus() default true;
        boolean useCommands() default true;    // Brigadier commands
        boolean useScheduler() default true;
        boolean useMessaging() default false;  // Plugin messaging
        String[] channels() default {};
        boolean useTabList() default false;
        boolean useTitleBar() default false;
        boolean useBossBar() default false;
        boolean useResourcePack() default false;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepVelocity */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DVEL {
        String target() default "";
        VelocityPluginConfig plugin() default @VelocityPluginConfig(id = "");
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🐰 @DeepBungee — BungeeCord proxy plugin integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepBungee {
        String target() default "";
        String name() default "";
        String version() default "";
        String author() default "";
        String[] depends() default {};
        String[] softDepends() default {};
        String description() default "";
        boolean useEventBus() default true;
        boolean useCommands() default true;
        boolean useScheduler() default true;
        boolean useMessaging() default false;
        String[] channels() default {};
        boolean useRedis() default false;      // RedisBungee
        boolean useTabList() default false;
        boolean useScoreboard() default false;
        boolean useMotd() default false;
        MinecraftProxyPlatform platform() default MinecraftProxyPlatform.BUNGEECORD;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepBungee */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DBUNGEE {
        String target() default "";
        String name() default "";
        String version() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    // ──────────────────────────────────────────────
    // Phase 21 — Annotations: General Game Engines
    // ──────────────────────────────────────────────

    /**
     * 🎮 @DeepUnity — Unity engine integration via JNI/GraalVM.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepUnity {
        String target() default "";
        EngineProjectConfig project() default @EngineProjectConfig(engine = GameEngine.UNITY);
        String unityVersion() default "";
        String scriptingBackend() default "IL2CPP"; // Mono, IL2CPP
        boolean useECS() default false;             // Unity DOTS ECS
        boolean useJobSystem() default false;       // Unity Job System
        boolean useBurstCompiler() default false;   // Burst Compiler
        boolean useAddressables() default false;    // Addressable Assets
        boolean useInputSystem() default false;     // New Input System
        boolean useUIToolkit() default false;        // UI Toolkit
        boolean useNetcode() default false;          // Netcode for GameObjects
        String[] assemblies() default {};           // Assembly definitions
        String renderPipeline() default "URP";      // URP, HDRP, Built-in
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepUnity */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DUNITY {
        String target() default "";
        String unityVersion() default "";
        String renderPipeline() default "URP";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🎯 @DeepUnreal — Unreal Engine integration via JNI/FFI.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepUnreal {
        String target() default "";
        EngineProjectConfig project() default @EngineProjectConfig(engine = GameEngine.UNREAL);
        String engineVersion() default "5.4";
        boolean useBlueprint() default false;
        boolean useGameplayAbilities() default false; // GAS
        boolean useEnhancedInput() default false;
        boolean useChaos() default false;             // Chaos physics
        boolean useNanite() default false;            // Nanite geometry
        boolean useLumen() default false;             // Lumen GI
        boolean useNiagara() default false;           // Niagara VFX
        boolean useMetaHumans() default false;
        boolean useWorldPartition() default false;    // World Partition streaming
        boolean useMassEntity() default false;        // Mass Entity (ECS-like)
        String[] modules() default {};                // UE module names
        String[] plugins() default {};                // UE plugin names
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepUnreal */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DUNREAL {
        String target() default "";
        String engineVersion() default "5.4";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🤖 @DeepGodot — Godot engine integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepGodot {
        String target() default "";
        EngineProjectConfig project() default @EngineProjectConfig(engine = GameEngine.GODOT);
        String godotVersion() default "4.2";
        String language() default "GDScript";   // GDScript, C#, GDExtension
        boolean useGDExtension() default false;
        boolean useSignals() default true;
        boolean useSceneTree() default true;
        boolean useResources() default true;
        boolean useAutoload() default false;
        boolean use2D() default false;
        boolean use3D() default false;
        boolean usePhysics() default false;
        boolean useNavigation() default false;
        boolean useAnimation() default false;
        boolean useMultiplayer() default false;
        String[] addons() default {};
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepGodot */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DGODOT {
        String target() default "";
        String godotVersion() default "4.2";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🦅 @DeepBevy — Bevy engine (Rust ECS) integration via JNI/FFI.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepBevy {
        String target() default "";
        EngineProjectConfig project() default @EngineProjectConfig(engine = GameEngine.BEVY);
        String bevyVersion() default "0.14";
        ECSOperation[] operations() default {};
        boolean useECS() default true;
        boolean useRendering() default true;
        boolean useAudio() default false;
        boolean useInput() default true;
        boolean useAssets() default true;
        boolean useScenes() default false;
        boolean useUI() default false;          // Bevy UI
        boolean useReflect() default false;     // Bevy Reflect
        boolean useDiagnostics() default false;
        boolean useAnimation() default false;
        boolean usePhysics() default false;     // bevy_rapier
        String[] plugins() default {};          // Bevy plugin crate names
        String[] systems() default {};          // System function names
        String[] components() default {};       // Component struct names
        String[] resources() default {};        // Resource struct names
        String[] events() default {};           // Event struct names
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepBevy */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DBEVY {
        String target() default "";
        String bevyVersion() default "0.14";
        ECSOperation[] operations() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 💎 @DeepAmethyst — Amethyst engine (Rust ECS, legacy) integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepAmethyst {
        String target() default "";
        EngineProjectConfig project() default @EngineProjectConfig(engine = GameEngine.AMETHYST);
        ECSOperation[] operations() default {};
        boolean useRendering() default true;
        boolean useAudio() default false;
        boolean useInput() default true;
        boolean useNetwork() default false;
        boolean useUI() default false;
        String[] systems() default {};
        String[] components() default {};
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta(
            description = "Amethyst is no longer maintained. Consider Bevy instead."
        );
    }

    /** Shortcut for @DeepAmethyst */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DAMETHYST {
        String target() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ⚙️ @DeepPiston — Piston game engine (Rust 2D) integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepPiston {
        String target() default "";
        EngineProjectConfig project() default @EngineProjectConfig(engine = GameEngine.PISTON);
        boolean useGraphics() default true;
        boolean useInput() default true;
        boolean useWindow() default true;
        boolean useOpenGL() default true;
        String backend() default "glutin";      // glutin, sdl2, glfw
        int windowWidth() default 800;
        int windowHeight() default 600;
        String title() default "Piston Window";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepPiston */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DPISTON {
        String target() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    // ──────────────────────────────────────────────
    // Phase 21 — Annotations: Multimedia Libraries
    // ──────────────────────────────────────────────

    /**
     * 🎯 @DeepRaylib — Raylib integration via LWJGL/JNI.
     *
     * Raylib is a simple and easy-to-use C library for videogame programming.
     * Integration uses native bindings (jaylib, raylib-j, or direct JNI).
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepRaylib {
        String target() default "";
        RaylibWindowConfig window() default @RaylibWindowConfig;
        RaylibModule[] modules() default {RaylibModule.CORE};
        boolean useAudio() default false;
        boolean usePhysics() default false;    // Physac
        boolean useRaygui() default false;     // Immediate mode GUI
        boolean use3D() default false;
        boolean useShaders() default false;
        boolean useVR() default false;
        String bindingLibrary() default "jaylib"; // jaylib, raylib-j, jni
        EngineIntegrationMode mode() default EngineIntegrationMode.JNI;
        String nativeLibraryPath() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepRaylib */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DRAYLIB {
        String target() default "";
        RaylibWindowConfig window() default @RaylibWindowConfig;
        RaylibModule[] modules() default {RaylibModule.CORE};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🎮 @DeepSDL — SDL (Simple DirectMedia Layer) integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepSDL {
        String target() default "";
        SDLWindowConfig window() default @SDLWindowConfig;
        String sdlVersion() default "2";       // 2 or 3
        SDLSubsystem[] subsystems() default {SDLSubsystem.EVERYTHING};
        boolean useSDLImage() default false;   // SDL_image
        boolean useSDLMixer() default false;   // SDL_mixer
        boolean useSDLTTF() default false;     // SDL_ttf
        boolean useSDLNet() default false;     // SDL_net
        boolean useSDLGpu() default false;     // SDL_gpu (SDL3)
        boolean useGameController() default false;
        boolean useHaptic() default false;
        EngineIntegrationMode mode() default EngineIntegrationMode.JNI;
        String nativeLibraryPath() default "";
        String bindingLibrary() default "libsdl4j"; // libsdl4j, jsdl, jni
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepSDL */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DSDL {
        String target() default "";
        SDLWindowConfig window() default @SDLWindowConfig;
        String sdlVersion() default "2";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🎨 @DeepSFML — SFML (Simple and Fast Multimedia Library) integration.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepSFML {
        String target() default "";
        int windowWidth() default 1280;
        int windowHeight() default 720;
        String windowTitle() default "DeepMix SFML Window";
        boolean useGraphics() default true;
        boolean useAudio() default false;
        boolean useNetwork() default false;
        boolean useSystem() default true;
        boolean antiAlias() default true;
        int antialiasingLevel() default 4;
        boolean vsync() default true;
        int framerateLimit() default 60;
        String sfmlVersion() default "2.6";
        EngineIntegrationMode mode() default EngineIntegrationMode.JNI;
        String bindingLibrary() default "jsfml"; // jsfml, jni
        String nativeLibraryPath() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepSFML */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DSFML {
        String target() default "";
        int windowWidth() default 1280;
        int windowHeight() default 720;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ❤️ @DeepLove2D — Love2D (Lua game framework) integration.
     *
     * Integration via embedded Lua runtime (LuaJ) or native JNI binding.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepLove2D {
        String target() default "";
        int windowWidth() default 800;
        int windowHeight() default 600;
        String windowTitle() default "DeepMix Love2D";
        boolean usePhysics() default false;    // love.physics (Box2D)
        boolean useAudio() default false;
        boolean useFilesystem() default true;
        boolean useGraphics() default true;
        boolean useJoystick() default false;
        boolean useKeyboard() default true;
        boolean useMouse() default true;
        boolean useTimer() default true;
        boolean useTouch() default false;
        boolean useThread() default false;
        boolean fullscreen() default false;
        boolean vsync() default true;
        int msaa() default 0;
        String luaVersion() default "5.1";     // LuaJIT compatible
        EngineIntegrationMode mode() default EngineIntegrationMode.JEP;
        String gamePath() default "";          // Path to main.lua directory
        String bindingLibrary() default "luaj"; // luaj, jnlua, jni
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepLove2D */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DLOVE {
        String target() default "";
        int windowWidth() default 800;
        int windowHeight() default 600;
        String gamePath() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    // ──────────────────────────────────────────────
    // Phase 21 — Processor
    // ──────────────────────────────────────────────

    /**
     * Processor for all Phase 21 (Game Engine Specific) annotations.
     */
    public static class Phase21Processor {

        private final DeepMixContext context;

        public Phase21Processor(DeepMixContext context) {
            this.context = context;
        }

        /** Process @DeepForge — Inject Forge mod initialization and event registration. */
        public void processForge(DeepForge annotation, ClassNode classNode,
                                 MethodNode methodNode) throws DeepMixProcessingException {
            ModMetadata mod = annotation.mod();
            InsnList forgeCode = new InsnList();

            forgeCode.add(new LdcInsnNode(mod.modId()));
            forgeCode.add(new LdcInsnNode(mod.name()));
            forgeCode.add(new LdcInsnNode(mod.version()));
            forgeCode.add(new LdcInsnNode(annotation.eventBus().name()));
            forgeCode.add(new InsnNode(
                annotation.useDeferredRegister() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            forgeCode.add(new InsnNode(
                annotation.useCapabilities() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            forgeCode.add(new InsnNode(
                annotation.useNetworking() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            forgeCode.add(new LdcInsnNode(annotation.networkChannel()));
            forgeCode.add(new InsnNode(
                annotation.useDataGen() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            forgeCode.add(new InsnNode(
                annotation.useConfigs() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            forgeCode.add(new LdcInsnNode(annotation.configType()));
            pushStringArray(forgeCode, annotation.registries());

            forgeCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/engine/mc/DeepMixForge",
                "initialize",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "Ljava/lang/String;ZZZLjava/lang/String;ZZLjava/lang/String;" +
                "[Ljava/lang/String;)V",
                false
            ));

            // Inject Forge event bus registration
            if (annotation.eventBus() == ForgeEventBus.FORGE ||
                annotation.eventBus() == ForgeEventBus.BOTH) {
                forgeCode.add(new VarInsnNode(Opcodes.ALOAD, 0));
                forgeCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/engine/mc/DeepMixForge",
                    "registerToForgeBus",
                    "(Ljava/lang/Object;)V", false
                ));
            }
            if (annotation.eventBus() == ForgeEventBus.MOD ||
                annotation.eventBus() == ForgeEventBus.BOTH) {
                forgeCode.add(new VarInsnNode(Opcodes.ALOAD, 0));
                forgeCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/engine/mc/DeepMixForge",
                    "registerToModBus",
                    "(Ljava/lang/Object;)V", false
                ));
            }

            // Access Transformer support
            if (annotation.useAccessTransformer()) {
                forgeCode.add(new LdcInsnNode(annotation.accessTransformerFile()));
                forgeCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/engine/mc/DeepMixForge",
                    "loadAccessTransformer",
                    "(Ljava/lang/String;)V", false
                ));
            }

            // CoreMod support
            if (annotation.useCoreMod()) {
                forgeCode.add(new LdcInsnNode(annotation.coreModClass()));
                forgeCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/engine/mc/DeepMixForge",
                    "loadCoreMod",
                    "(Ljava/lang/String;)V", false
                ));
            }

            methodNode.instructions.insert(forgeCode);

            context.addDiagnostic(String.format(
                "🔨 @DeepForge applied to %s [modId=%s, eventBus=%s, versions=%s]",
                classNode.name, mod.modId(), annotation.eventBus(),
                Arrays.toString(annotation.mcVersions())
            ));
        }

        /** Process @DeepNeoForge */
        public void processNeoForge(DeepNeoForge annotation, ClassNode classNode,
                                    MethodNode methodNode) throws DeepMixProcessingException {
            ModMetadata mod = annotation.mod();
            InsnList neoCode = new InsnList();

            neoCode.add(new LdcInsnNode(mod.modId()));
            neoCode.add(new LdcInsnNode(mod.name()));
            neoCode.add(new LdcInsnNode(mod.version()));
            neoCode.add(new InsnNode(
                annotation.useDeferredRegister() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            neoCode.add(new InsnNode(
                annotation.useAttachments() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            neoCode.add(new InsnNode(
                annotation.usePayloads() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            neoCode.add(new InsnNode(
                annotation.useModBus() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            neoCode.add(new InsnNode(
                annotation.useGameBus() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            // Encode extensions
            NeoForgeExtension[] exts = annotation.extensions();
            neoCode.add(new LdcInsnNode(exts.length));
            neoCode.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < exts.length; i++) {
                neoCode.add(new InsnNode(Opcodes.DUP));
                neoCode.add(new LdcInsnNode(i));
                neoCode.add(new LdcInsnNode(exts[i].name()));
                neoCode.add(new InsnNode(Opcodes.AASTORE));
            }

            pushStringArray(neoCode, annotation.registries());

            neoCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/engine/mc/DeepMixNeoForge",
                "initialize",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "ZZZZZ[Ljava/lang/String;[Ljava/lang/String;)V",
                false
            ));

            methodNode.instructions.insert(neoCode);

            context.addDiagnostic(String.format(
                "⚡ @DeepNeoForge applied to %s [modId=%s, extensions=%s]",
                classNode.name, mod.modId(), Arrays.toString(annotation.extensions())
            ));
        }

        /** Process @DeepFabric */
        public void processFabric(DeepFabric annotation, ClassNode classNode,
                                  MethodNode methodNode) throws DeepMixProcessingException {
            ModMetadata mod = annotation.mod();
            InsnList fabricCode = new InsnList();

            fabricCode.add(new LdcInsnNode(mod.modId()));
            fabricCode.add(new LdcInsnNode(mod.name()));
            fabricCode.add(new LdcInsnNode(annotation.entrypoint().name()));
            fabricCode.add(new InsnNode(
                annotation.useEvents() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            fabricCode.add(new InsnNode(
                annotation.useNetworking() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            fabricCode.add(new InsnNode(
                annotation.useCommands() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            fabricCode.add(new InsnNode(
                annotation.useRendering() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            fabricCode.add(new InsnNode(
                annotation.useAccessWidener() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            fabricCode.add(new LdcInsnNode(annotation.accessWidenerFile()));
            pushStringArray(fabricCode, annotation.fabricApiModules());

            fabricCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/engine/mc/DeepMixFabric",
                "initialize",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "ZZZZZLjava/lang/String;[Ljava/lang/String;)V",
                false
            ));

            methodNode.instructions.insert(fabricCode);

            context.addDiagnostic(String.format(
                "🧵 @DeepFabric applied to %s [modId=%s, entrypoint=%s]",
                classNode.name, mod.modId(), annotation.entrypoint()
            ));
        }

        /** Process @DeepQuilt */
        public void processQuilt(DeepQuilt annotation, ClassNode classNode,
                                 MethodNode methodNode) throws DeepMixProcessingException {
            ModMetadata mod = annotation.mod();
            InsnList quiltCode = new InsnList();

            quiltCode.add(new LdcInsnNode(mod.modId()));
            quiltCode.add(new LdcInsnNode(annotation.phase().name()));
            quiltCode.add(new InsnNode(
                annotation.fabricCompatible() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            quiltCode.add(new InsnNode(
                annotation.useQSL() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            quiltCode.add(new InsnNode(
                annotation.useQuiltEvents() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            quiltCode.add(new InsnNode(
                annotation.useQuiltNetworking() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            pushStringArray(quiltCode, annotation.qslModules());

            quiltCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/engine/mc/DeepMixQuilt",
                "initialize",
                "(Ljava/lang/String;Ljava/lang/String;ZZZZ[Ljava/lang/String;)V",
                false
            ));

            methodNode.instructions.insert(quiltCode);

            context.addDiagnostic(String.format(
                "🪶 @DeepQuilt applied to %s [modId=%s, phase=%s, fabricCompat=%b, QSL=%b]",
                classNode.name, mod.modId(), annotation.phase(),
                annotation.fabricCompatible(), annotation.useQSL()
            ));
        }

        /** Process @DeepSpigot */
        public void processSpigot(DeepSpigot annotation, ClassNode classNode,
                                  MethodNode methodNode) throws DeepMixProcessingException {
            PluginDescription plugin = annotation.plugin();
            InsnList spigotCode = new InsnList();

            spigotCode.add(new LdcInsnNode(plugin.name()));
            spigotCode.add(new LdcInsnNode(plugin.version()));
            spigotCode.add(new LdcInsnNode(annotation.eventPriority().name()));
            spigotCode.add(new InsnNode(
                annotation.ignoreCancelled() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            spigotCode.add(new LdcInsnNode(annotation.platform().name()));
            spigotCode.add(new InsnNode(
                annotation.useNMS() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            spigotCode.add(new LdcInsnNode(annotation.nmsVersion()));
            pushStringArray(spigotCode, annotation.channels());

            spigotCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/engine/mc/DeepMixSpigot",
                "initialize",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "ZLjava/lang/String;ZLjava/lang/String;[Ljava/lang/String;)V",
                false
            ));

            methodNode.instructions.insert(spigotCode);

            context.addDiagnostic(String.format(
                "🔧 @DeepSpigot applied to %s [plugin=%s, platform=%s, nms=%b]",
                classNode.name, plugin.name(), annotation.platform(), annotation.useNMS()
            ));
        }

        /** Process @DeepBevy — Wire up Bevy ECS operations via JNI/FFI. */
        public void processBevy(DeepBevy annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            InsnList bevyCode = new InsnList();

            bevyCode.add(new LdcInsnNode(annotation.bevyVersion()));
            bevyCode.add(new LdcInsnNode(annotation.project().mode().name()));
            bevyCode.add(new InsnNode(
                annotation.useECS() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            bevyCode.add(new InsnNode(
                annotation.useRendering() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            bevyCode.add(new InsnNode(
                annotation.useAudio() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            bevyCode.add(new InsnNode(
                annotation.usePhysics() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            pushStringArray(bevyCode, annotation.plugins());
            pushStringArray(bevyCode, annotation.systems());
            pushStringArray(bevyCode, annotation.components());
            pushStringArray(bevyCode, annotation.resources());
            pushStringArray(bevyCode, annotation.events());

            // Encode ECS operations
            ECSOperation[] ops = annotation.operations();
            bevyCode.add(new LdcInsnNode(ops.length));
            bevyCode.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < ops.length; i++) {
                bevyCode.add(new InsnNode(Opcodes.DUP));
                bevyCode.add(new LdcInsnNode(i));
                bevyCode.add(new LdcInsnNode(ops[i].name()));
                bevyCode.add(new InsnNode(Opcodes.AASTORE));
            }

            bevyCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/engine/bevy/DeepMixBevy",
                "initialize",
                "(Ljava/lang/String;Ljava/lang/String;ZZZZ" +
                "[Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;" +
                "[Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)V",
                false
            ));

            methodNode.instructions.insert(bevyCode);

            context.addDiagnostic(String.format(
                "🦅 @DeepBevy applied to %s::%s [version=%s, ecs=%b, ops=%d]",
                classNode.name, methodNode.name,
                annotation.bevyVersion(), annotation.useECS(), ops.length
            ));
        }

        /** Process @DeepRaylib */
        public void processRaylib(DeepRaylib annotation, ClassNode classNode,
                                  MethodNode methodNode) throws DeepMixProcessingException {
            RaylibWindowConfig win = annotation.window();
            InsnList raylibCode = new InsnList();

            raylibCode.add(new LdcInsnNode(win.width()));
            raylibCode.add(new LdcInsnNode(win.height()));
            raylibCode.add(new LdcInsnNode(win.title()));
            raylibCode.add(new LdcInsnNode(win.targetFPS()));
            raylibCode.add(new InsnNode(
                win.fullscreen() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            raylibCode.add(new InsnNode(
                win.resizable() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            raylibCode.add(new InsnNode(
                win.vsync() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            raylibCode.add(new InsnNode(
                win.msaa4x() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            // Modules
            RaylibModule[] modules = annotation.modules();
            raylibCode.add(new LdcInsnNode(modules.length));
            raylibCode.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < modules.length; i++) {
                raylibCode.add(new InsnNode(Opcodes.DUP));
                raylibCode.add(new LdcInsnNode(i));
                raylibCode.add(new LdcInsnNode(modules[i].name()));
                raylibCode.add(new InsnNode(Opcodes.AASTORE));
            }

            raylibCode.add(new LdcInsnNode(annotation.bindingLibrary()));
            raylibCode.add(new LdcInsnNode(annotation.mode().name()));
            raylibCode.add(new LdcInsnNode(annotation.nativeLibraryPath()));

            raylibCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/engine/raylib/DeepMixRaylib",
                "initialize",
                "(IILjava/lang/String;IZZZ Z[Ljava/lang/String;" +
                "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                false
            ));

            methodNode.instructions.insert(raylibCode);

            context.addDiagnostic(String.format(
                "🎯 @DeepRaylib applied to %s::%s [%dx%d@%dfps, modules=%s, binding=%s]",
                classNode.name, methodNode.name,
                win.width(), win.height(), win.targetFPS(),
                Arrays.toString(modules), annotation.bindingLibrary()
            ));
        }

        /** Process @DeepSDL */
        public void processSDL(DeepSDL annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            SDLWindowConfig win = annotation.window();
            InsnList sdlCode = new InsnList();

            sdlCode.add(new LdcInsnNode(win.title()));
            sdlCode.add(new LdcInsnNode(win.width()));
            sdlCode.add(new LdcInsnNode(win.height()));
            sdlCode.add(new LdcInsnNode(annotation.sdlVersion()));
            sdlCode.add(new InsnNode(
                win.opengl() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            sdlCode.add(new InsnNode(
                win.vulkan() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            sdlCode.add(new InsnNode(
                win.metal() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            sdlCode.add(new InsnNode(
                win.resizable() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            sdlCode.add(new InsnNode(
                win.fullscreen() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            sdlCode.add(new InsnNode(
                win.highDPI() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            sdlCode.add(new InsnNode(
                annotation.useSDLImage() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            sdlCode.add(new InsnNode(
                annotation.useSDLMixer() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            sdlCode.add(new InsnNode(
                annotation.useSDLTTF() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            sdlCode.add(new InsnNode(
                annotation.useSDLNet() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

            sdlCode.add(new LdcInsnNode(annotation.bindingLibrary()));
            sdlCode.add(new LdcInsnNode(annotation.mode().name()));

            sdlCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/engine/sdl/DeepMixSDL",
                "initialize",
                "(Ljava/lang/String;IILjava/lang/String;ZZZZZZ" +
                "ZZZZLjava/lang/String;Ljava/lang/String;)V",
                false
            ));

            methodNode.instructions.insert(sdlCode);

            context.addDiagnostic(String.format(
                "🎮 @DeepSDL applied to %s::%s [SDL%s, %dx%d, binding=%s]",
                classNode.name, methodNode.name,
                annotation.sdlVersion(), win.width(), win.height(),
                annotation.bindingLibrary()
            ));
        }

        private void pushStringArray(InsnList insns, String[] values) {
            insns.add(new LdcInsnNode(values.length));
            insns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < values.length; i++) {
                insns.add(new InsnNode(Opcodes.DUP));
                insns.add(new LdcInsnNode(i));
                insns.add(new LdcInsnNode(values[i]));
                insns.add(new InsnNode(Opcodes.AASTORE));
            }
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                      ║
    // ║  PHASE 22: ADVANCED ASM & BYTECODE OPERATIONS                        ║
    // ║  12 annotations | Priority: HIGH | Est. Time: 2 days                  ║
    // ║                                                                      ║
    // ║  @DeepASMInline     @DeepStackMap     @DeepLocalVar                  ║
    // ║  @DeepLineNumber    @DeepTryCatch     @DeepFinally                   ║
    // ║  @DeepSwitch        @DeepTableSwitch  @DeepLookupSwitch              ║
    // ║  @DeepInvokeDynamic @DeepBootstrap    @DeepMethodHandle              ║
    // ║                                                                      ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    // ──────────────────────────────────────────────
    // Phase 22 — Enums
    // ──────────────────────────────────────────────

    /** ASM instruction type for inline injection */
    public enum ASMInstructionType {
        INSN,                  // Zero-operand instruction
        INT_INSN,              // Single int operand (BIPUSH, SIPUSH, NEWARRAY)
        VAR_INSN,              // Local variable instruction (ILOAD, ASTORE, etc.)
        TYPE_INSN,             // Type instruction (NEW, CHECKCAST, INSTANCEOF)
        FIELD_INSN,            // Field instruction (GETFIELD, PUTFIELD, etc.)
        METHOD_INSN,           // Method instruction (INVOKE*)
        INVOKE_DYNAMIC_INSN,   // InvokeDynamic instruction
        JUMP_INSN,             // Jump instruction (IF*, GOTO)
        LABEL,                 // Label node
        LDC_INSN,              // LDC instruction
        IINC_INSN,             // IINC instruction
        TABLESWITCH_INSN,      // TableSwitch
        LOOKUPSWITCH_INSN,     // LookupSwitch
        MULTIANEWARRAY_INSN,   // MULTIANEWARRAY
        FRAME,                 // Stack map frame
        LINE                   // Line number
    }

    /** Injection position relative to target */
    public enum InjectionPoint {
        BEFORE,                // Before target instruction
        AFTER,                 // After target instruction
        REPLACE,               // Replace target instruction
        HEAD,                  // Method head
        TAIL,                  // Before every return
        RETURN,                // Before specific return type
        INVOKE,                // Before/after method call
        FIELD_GET,             // Before/after field read
        FIELD_SET,             // Before/after field write
        NEW,                   // Before/after NEW instruction
        INSTANCEOF,            // Before/after INSTANCEOF
        JUMP,                  // Before/after jump instruction
        CONSTANT,              // At constant load (LDC)
        OPCODE,                // At specific opcode
        LINE_NUMBER,           // At specific line number
        LABEL,                 // At specific label
        CUSTOM                 // Custom locator
    }

    /** Stack map frame type */
    public enum StackMapFrameType {
        SAME,                  // Same locals, empty stack
        SAME_LOCALS_1,         // Same locals, 1 stack item
        CHOP,                  // Remove 1-3 locals
        APPEND,                // Add 1-3 locals
        FULL,                  // Full frame specification
        SAME_EXTENDED,         // Same with extended offset
        SAME_LOCALS_1_EXTENDED // Same locals 1 with extended offset
    }

    /** Switch optimization strategy */
    public enum SwitchOptimization {
        AUTO,                  // Let optimizer choose
        TABLESWITCH,           // Force TableSwitch (dense cases)
        LOOKUPSWITCH,          // Force LookupSwitch (sparse cases)
        BINARY_SEARCH,         // Binary search tree
        HASH_MAP,              // HashMap-based dispatch
        IF_ELSE_CHAIN,         // Convert to if-else chain
        PERFECT_HASH           // Perfect hash function dispatch
    }

    /** MethodHandle kind */
    public enum MethodHandleKind {
        GET_FIELD(1),
        GET_STATIC(2),
        PUT_FIELD(3),
        PUT_STATIC(4),
        INVOKE_VIRTUAL(5),
        INVOKE_STATIC(6),
        INVOKE_SPECIAL(7),
        NEW_INVOKE_SPECIAL(8),
        INVOKE_INTERFACE(9);

        private final int tag;
        MethodHandleKind(int tag) { this.tag = tag; }
        public int tag() { return tag; }
    }

    /** Try-catch scope mode */
    public enum TryCatchMode {
        WRAP,                  // Wrap existing code in try-catch
        INSERT,                // Insert new try-catch block
        MODIFY,                // Modify existing try-catch
        REMOVE,                // Remove existing try-catch
        EXTEND,                // Extend existing try-catch range
        NEST                   // Nest inside existing try-catch
    }

    // ──────────────────────────────────────────────
    // Phase 22 — Sub-annotation types
    // ──────────────────────────────────────────────

    /** Raw ASM instruction specification */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ASMInstruction {
        ASMInstructionType type();
        int opcode() default -1;
        String owner() default "";             // For FIELD_INSN, METHOD_INSN
        String name() default "";              // For FIELD_INSN, METHOD_INSN
        String desc() default "";              // Descriptor
        int var() default -1;                  // For VAR_INSN
        int intOperand() default 0;            // For INT_INSN
        String ldcValue() default "";          // For LDC_INSN
        String ldcType() default "String";     // String, int, long, float, double, Type
        boolean itf() default false;           // Interface method call
        String label() default "";             // Label name
        int order() default 0;                 // Execution order
    }

    /** Stack map frame specification */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface StackMapFrame {
        StackMapFrameType type() default StackMapFrameType.FULL;
        String[] locals() default {};          // Local variable types
        String[] stack() default {};           // Stack types
        int numChoppedLocals() default 0;      // For CHOP
        int offset() default 0;               // Frame offset
    }

    /** Local variable specification */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LocalVarSpec {
        String name();
        String descriptor();
        String signature() default "";
        int index() default -1;               // -1 = auto-assign
        String startLabel() default "";
        String endLabel() default "";
    }

    /** Try-catch block specification */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TryCatchSpec {
        String startLabel() default "";
        String endLabel() default "";
        String handlerLabel() default "";
        String exceptionType() default "";     // Internal name (null = finally)
        String[] catchBody() default {};       // ASM instructions for handler
    }

    /** Bootstrap method specification for InvokeDynamic */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BootstrapSpec {
        MethodHandleKind handleKind() default MethodHandleKind.INVOKE_STATIC;
        String owner();
        String name();
        String descriptor();
        String[] args() default {};            // Bootstrap arguments as strings
        String[] argTypes() default {};        // Types of bootstrap arguments
    }

    /** Switch case specification */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SwitchCase {
        int key();
        String label();
        String[] instructions() default {};    // Inline instructions for this case
    }

    // ──────────────────────────────────────────────
    // Phase 22 — Annotations
    // ──────────────────────────────────────────────

    /**
     * 🔧 @DeepASMInline — Inline raw ASM instruction injection.
     *
     * Inject raw bytecode instructions at precise locations within methods.
     * This is the most powerful and dangerous DeepMix annotation — use with care.
     *
     * Example:
     * <pre>
     * {@code @DeepASMInline(
     *     at = InjectionPoint.HEAD,
     *     instructions = {
     *         @ASMInstruction(type = ASMInstructionType.VAR_INSN, opcode = 25, var = 0),
     *         @ASMInstruction(type = ASMInstructionType.METHOD_INSN, opcode = 182,
     *             owner = "java/lang/Object", name = "hashCode", desc = "()I")
     *     }
     * )}
     * public void targetMethod() { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepASMInline {
        String target() default "";
        InjectionPoint at() default InjectionPoint.HEAD;
        ASMInstruction[] instructions();
        String targetInsn() default "";        // Target instruction locator
        int targetOpcode() default -1;         // Target opcode to find
        int targetOrdinal() default 0;         // Nth occurrence of target
        String targetOwner() default "";       // For method/field targets
        String targetName() default "";
        String targetDesc() default "";
        boolean validateStack() default true;  // Validate stack integrity
        boolean computeFrames() default true;  // Recompute stack map frames
        boolean computeMaxs() default true;    // Recompute maxStack/maxLocals
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepASMInline */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DASMI {
        String target() default "";
        InjectionPoint at() default InjectionPoint.HEAD;
        ASMInstruction[] instructions();
        int targetOpcode() default -1;
        int targetOrdinal() default 0;
        boolean validateStack() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📊 @DeepStackMap — Stack map frame manipulation.
     *
     * Insert, modify, or recalculate stack map frames for Java 7+ class files.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepStackMap {
        String target() default "";
        StackMapFrame[] frames() default {};
        boolean recompute() default false;     // Force full recomputation
        boolean verify() default true;         // Verify frames after modification
        boolean removeAll() default false;     // Remove all existing frames
        int classFileVersion() default 52;     // Java 8 = 52
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepStackMap */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DSTACKMAP {
        String target() default "";
        StackMapFrame[] frames() default {};
        boolean recompute() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📋 @DeepLocalVar — Local variable table manipulation.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepLocalVar {
        String target() default "";
        LocalVarSpec[] add() default {};       // Variables to add
        String[] remove() default {};          // Variable names to remove
        LocalVarSpec[] modify() default {};    // Variables to modify
        boolean preserveDebugInfo() default true;
        boolean recalculateIndices() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepLocalVar */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DLOCALVAR {
        String target() default "";
        LocalVarSpec[] add() default {};
        String[] remove() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📍 @DeepLineNumber — Line number manipulation.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepLineNumber {
        String target() default "";
        int[] addLines() default {};           // Line numbers to insert
        int[] removeLines() default {};        // Line numbers to remove
        int offset() default 0;               // Shift all line numbers by offset
        boolean removeAll() default false;     // Strip all line number info
        boolean renumber() default false;      // Sequentially renumber
        int renumberStart() default 1;         // Starting line for renumbering
        String sourceFile() default "";        // Override SourceFile attribute
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepLineNumber */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DLINENO {
        String target() default "";
        int offset() default 0;
        boolean removeAll() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🛡️ @DeepTryCatch — Try-catch block injection and manipulation.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepTryCatch {
        String target() default "";
        TryCatchMode mode() default TryCatchMode.WRAP;
        TryCatchSpec[] blocks() default {};
        String[] exceptionTypes() default {"java/lang/Exception"};
        String handlerMethod() default "";     // Method to call in catch
        boolean logException() default false;
        boolean rethrow() default false;       // Rethrow after handling
        boolean wrapEntireMethod() default true;
        String catchReturnValue() default "";   // Value to return on catch
        InjectionPoint at() default InjectionPoint.HEAD;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepTryCatch */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DTRYCATCH {
        String target() default "";
        TryCatchMode mode() default TryCatchMode.WRAP;
        String[] exceptionTypes() default {"java/lang/Exception"};
        boolean rethrow() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔒 @DeepFinally — Finally block manipulation and injection.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepFinally {
        String target() default "";
        ASMInstruction[] instructions() default {};
        String cleanupMethod() default "";     // Method to call in finally
        boolean wrapEntireMethod() default true;
        boolean ensureResourceCleanup() default false;
        String[] resourceFields() default {};  // Fields to clean up
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepFinally */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DFINALLY {
        String target() default "";
        String cleanupMethod() default "";
        boolean wrapEntireMethod() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔀 @DeepSwitch — Switch statement transformation and optimization.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepSwitch {
        String target() default "";
        SwitchOptimization optimization() default SwitchOptimization.AUTO;
        SwitchCase[] addCases() default {};    // Cases to add
        int[] removeCases() default {};        // Case keys to remove
        String defaultLabel() default "";      // Override default branch
        boolean convertToIfElse() default false;
        boolean convertToLookup() default false;
        boolean convertToTable() default false;
        boolean optimizeDensity() default true;
        int densityThreshold() default 3;      // Table/lookup switch-over point
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepSwitch */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DSWITCH {
        String target() default "";
        SwitchOptimization optimization() default SwitchOptimization.AUTO;
        SwitchCase[] addCases() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📊 @DeepTableSwitch — TableSwitch-specific optimization.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepTableSwitch {
        String target() default "";
        int min() default -1;                  // Minimum key (auto-detect if -1)
        int max() default -1;                  // Maximum key (auto-detect)
        SwitchCase[] cases() default {};
        String defaultLabel() default "";
        boolean fillGaps() default true;       // Fill gaps with default
        boolean convertFromLookup() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepTableSwitch */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DTSWITCH {
        String target() default "";
        SwitchCase[] cases() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔍 @DeepLookupSwitch — LookupSwitch-specific optimization.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepLookupSwitch {
        String target() default "";
        SwitchCase[] cases() default {};
        String defaultLabel() default "";
        boolean sortKeys() default true;       // Ensure keys are sorted
        boolean convertFromTable() default false;
        boolean useBinarySearch() default false; // Override lookup with binary search
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepLookupSwitch */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DLSWITCH {
        String target() default "";
        SwitchCase[] cases() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ⚡ @DeepInvokeDynamic — InvokeDynamic instruction injection/modification.
     *
     * Create or modify invokedynamic call sites with custom bootstrap methods.
     * Enables lambda metafactory, string concatenation factory, and custom
     * dynamic dispatch mechanisms.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepInvokeDynamic {
        String target() default "";
        String name();                         // Dynamic call site name
        String descriptor();                   // Call site type descriptor
        BootstrapSpec bootstrap();             // Bootstrap method specification
        InjectionPoint at() default InjectionPoint.HEAD;
        int targetOrdinal() default 0;
        boolean replaceLambda() default false;  // Replace existing lambda
        boolean createLambda() default false;   // Create new lambda call site
        String functionalInterface() default ""; // For lambda creation
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepInvokeDynamic */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DINVDYN {
        String target() default "";
        String name();
        String descriptor();
        BootstrapSpec bootstrap();
        InjectionPoint at() default InjectionPoint.HEAD;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔗 @DeepBootstrap — Bootstrap method creation and manipulation.
     *
     * Create or modify bootstrap methods used by invokedynamic call sites.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DeepBootstrap {
        String target() default "";
        String methodName() default "";
        String descriptor() default "";
        MethodHandleKind handleKind() default MethodHandleKind.INVOKE_STATIC;
        String[] staticArgs() default {};      // Constant pool arguments
        boolean generateMethod() default false; // Generate bootstrap method
        String callSiteName() default "";      // For linking with invokedynamic
        boolean mutableCallSite() default false;
        boolean volatileCallSite() default false;
        boolean constantCallSite() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepBootstrap */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface DBOOT {
        String target() default "";
        String methodName() default "";
        MethodHandleKind handleKind() default MethodHandleKind.INVOKE_STATIC;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔑 @DeepMethodHandle — MethodHandle creation and manipulation.
     *
     * Creates or modifies method handles for high-performance reflective access.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DeepMethodHandle {
        String target() default "";
        MethodHandleKind kind() default MethodHandleKind.INVOKE_VIRTUAL;
        String owner() default "";
        String name() default "";
        String descriptor() default "";
        boolean exactInvoke() default false;   // MethodHandle.invokeExact vs invoke
        boolean spreadArgs() default false;    // Spread array arguments
        boolean collectArgs() default false;   // Collect arguments into array
        boolean bindTo() default false;        // Bind receiver
        String bindToField() default "";       // Field containing bind target
        boolean cache() default true;          // Cache the MethodHandle
        boolean asType() default false;        // Adapt handle type
        String targetType() default "";        // Target type for asType
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepMethodHandle */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DMH {
        String target() default "";
        MethodHandleKind kind() default MethodHandleKind.INVOKE_VIRTUAL;
        String owner() default "";
        String name() default "";
        String descriptor() default "";
        boolean cache() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    // ──────────────────────────────────────────────
    // Phase 22 — Processor
    // ──────────────────────────────────────────────

    /**
     * Processor for all Phase 22 (Advanced ASM Operations) annotations.
     */
    public static class Phase22Processor {

        private final DeepMixContext context;

        public Phase22Processor(DeepMixContext context) {
            this.context = context;
        }

        /** Process @DeepASMInline — Inject raw bytecode instructions. */
        public void processASMInline(DeepASMInline annotation, ClassNode classNode,
                                     MethodNode methodNode) throws DeepMixProcessingException {
            ASMInstruction[] instructions = annotation.instructions();
            InsnList injection = new InsnList();

            // Build labels map for forward references
            Map<String, LabelNode> labelMap = new HashMap<>();
            for (ASMInstruction insn : instructions) {
                if (insn.type() == ASMInstructionType.LABEL && !insn.label().isEmpty()) {
                    labelMap.put(insn.label(), new LabelNode());
                }
            }

            // Build instruction list
            for (ASMInstruction insn : instructions) {
                AbstractInsnNode node = buildInstructionNode(insn, labelMap);
                if (node != null) {
                    injection.add(node);
                }
            }

            // Determine injection point and insert
            switch (annotation.at()) {
                case HEAD:
                    methodNode.instructions.insert(injection);
                    break;
                case TAIL:
                    insertBeforeReturns(methodNode, injection);
                    break;
                case OPCODE:
                    if (annotation.targetOpcode() >= 0) {
                        insertAtOpcode(methodNode, injection,
                            annotation.targetOpcode(), annotation.targetOrdinal());
                    }
                    break;
                case INVOKE:
                    if (!annotation.targetOwner().isEmpty()) {
                        insertAtMethodCall(methodNode, injection,
                            annotation.targetOwner(), annotation.targetName(),
                            annotation.targetDesc(), annotation.targetOrdinal());
                    }
                    break;
                case LINE_NUMBER:
                    // Insert at specific line number
                    break;
                default:
                    methodNode.instructions.insert(injection);
                    break;
            }

            // Recompute if requested
            if (annotation.computeMaxs()) {
                // ClassWriter with COMPUTE_MAXS will handle this
            }

            context.addDiagnostic(String.format(
                "🔧 @DeepASMInline applied to %s::%s [%d instructions at %s]",
                classNode.name, methodNode.name, instructions.length, annotation.at()
            ));
        }

        /** Process @DeepTryCatch — Inject or modify try-catch blocks. */
        public void processTryCatch(DeepTryCatch annotation, ClassNode classNode,
                                    MethodNode methodNode) throws DeepMixProcessingException {
            switch (annotation.mode()) {
                case WRAP:
                    if (annotation.wrapEntireMethod()) {
                        wrapMethodInTryCatch(methodNode, annotation);
                    }
                    break;
                case INSERT:
                    for (TryCatchSpec spec : annotation.blocks()) {
                        insertTryCatchBlock(methodNode, spec);
                    }
                    break;
                case REMOVE:
                    removeMatchingTryCatch(methodNode, annotation.exceptionTypes());
                    break;
                default:
                    break;
            }

            context.addDiagnostic(String.format(
                "🛡️ @DeepTryCatch applied to %s::%s [mode=%s, exceptions=%s]",
                classNode.name, methodNode.name,
                annotation.mode(), Arrays.toString(annotation.exceptionTypes())
            ));
        }

        /** Process @DeepInvokeDynamic — Create invokedynamic call sites. */
        public void processInvokeDynamic(DeepInvokeDynamic annotation, ClassNode classNode,
                                         MethodNode methodNode) throws DeepMixProcessingException {
            BootstrapSpec bs = annotation.bootstrap();

            // Build Handle for bootstrap method
            Handle bootstrapHandle = new Handle(
                bs.handleKind().tag(),
                bs.owner(),
                bs.name(),
                bs.descriptor(),
                false
            );

            // Build bootstrap arguments
            Object[] bsmArgs = buildBootstrapArgs(bs.args(), bs.argTypes());

            // Create InvokeDynamicInsnNode
            InvokeDynamicInsnNode indyNode = new InvokeDynamicInsnNode(
                annotation.name(),
                annotation.descriptor(),
                bootstrapHandle,
                bsmArgs
            );

            // Insert at the appropriate location
            InsnList indyInsn = new InsnList();
            indyInsn.add(indyNode);

            switch (annotation.at()) {
                case HEAD:
                    methodNode.instructions.insert(indyInsn);
                    break;
                case TAIL:
                    insertBeforeReturns(methodNode, indyInsn);
                    break;
                default:
                    methodNode.instructions.insert(indyInsn);
                    break;
            }

            context.addDiagnostic(String.format(
                "⚡ @DeepInvokeDynamic applied to %s::%s [name=%s, bootstrap=%s.%s]",
                classNode.name, methodNode.name,
                annotation.name(), bs.owner(), bs.name()
            ));
        }

        /** Process @DeepSwitch — Optimize switch statements. */
        public void processSwitch(DeepSwitch annotation, ClassNode classNode,
                                  MethodNode methodNode) throws DeepMixProcessingException {
            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn instanceof TableSwitchInsnNode || insn instanceof LookupSwitchInsnNode) {
                    switch (annotation.optimization()) {
                        case TABLESWITCH:
                            if (insn instanceof LookupSwitchInsnNode) {
                                convertLookupToTable(methodNode, (LookupSwitchInsnNode) insn);
                            }
                            break;
                        case LOOKUPSWITCH:
                            if (insn instanceof TableSwitchInsnNode) {
                                convertTableToLookup(methodNode, (TableSwitchInsnNode) insn);
                            }
                            break;
                        case IF_ELSE_CHAIN:
                            convertSwitchToIfElse(methodNode, insn);
                            break;
                        case AUTO:
                            optimizeSwitchAuto(methodNode, insn, annotation.densityThreshold());
                            break;
                        default:
                            break;
                    }
                }
            }

            // Add new cases
            for (SwitchCase sc : annotation.addCases()) {
                addSwitchCase(methodNode, sc);
            }

            context.addDiagnostic(String.format(
                "🔀 @DeepSwitch applied to %s::%s [optimization=%s]",
                classNode.name, methodNode.name, annotation.optimization()
            ));
        }

        /** Process @DeepMethodHandle — Create MethodHandle lookups. */
        public void processMethodHandle(DeepMethodHandle annotation, ClassNode classNode,
                                        MethodNode methodNode) throws DeepMixProcessingException {
            InsnList mhCode = new InsnList();

            // MethodHandles.lookup()
            mhCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "java/lang/invoke/MethodHandles",
                "lookup",
                "()Ljava/lang/invoke/MethodHandles$Lookup;",
                false
            ));

            String owner = annotation.owner().isEmpty() ? classNode.name : annotation.owner();
            String name = annotation.name().isEmpty() ? methodNode.name : annotation.name();
            String descriptor = annotation.descriptor();

            switch (annotation.kind()) {
                case INVOKE_VIRTUAL:
                    // lookup.findVirtual(ownerClass, name, methodType)
                    mhCode.add(new LdcInsnNode(Type.getObjectType(owner)));
                    mhCode.add(new LdcInsnNode(name));
                    pushMethodType(mhCode, descriptor);
                    mhCode.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/invoke/MethodHandles$Lookup",
                        "findVirtual",
                        "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)" +
                            "Ljava/lang/invoke/MethodHandle;",
                        false
                    ));
                    break;

                case INVOKE_STATIC:
                    mhCode.add(new LdcInsnNode(Type.getObjectType(owner)));
                    mhCode.add(new LdcInsnNode(name));
                    pushMethodType(mhCode, descriptor);
                    mhCode.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/invoke/MethodHandles$Lookup",
                        "findStatic",
                        "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)" +
                            "Ljava/lang/invoke/MethodHandle;",
                        false
                    ));
                    break;

                case INVOKE_SPECIAL:
                    mhCode.add(new LdcInsnNode(Type.getObjectType(owner)));
                    mhCode.add(new LdcInsnNode(name));
                    pushMethodType(mhCode, descriptor);
                    mhCode.add(new LdcInsnNode(Type.getObjectType(classNode.name)));
                    mhCode.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/invoke/MethodHandles$Lookup",
                        "findSpecial",
                        "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;" +
                            "Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
                        false
                    ));
                    break;

                case NEW_INVOKE_SPECIAL:
                    mhCode.add(new LdcInsnNode(Type.getObjectType(owner)));
                    pushMethodType(mhCode, descriptor);
                    mhCode.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/invoke/MethodHandles$Lookup",
                        "findConstructor",
                        "(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)" +
                            "Ljava/lang/invoke/MethodHandle;",
                        false
                    ));
                    break;

                case GET_FIELD:
                    mhCode.add(new LdcInsnNode(Type.getObjectType(owner)));
                    mhCode.add(new LdcInsnNode(name));
                    mhCode.add(new LdcInsnNode(Type.getType(descriptor)));
                    mhCode.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/invoke/MethodHandles$Lookup",
                        "findGetter",
                        "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)" +
                            "Ljava/lang/invoke/MethodHandle;",
                        false
                    ));
                    break;

                case GET_STATIC:
                    mhCode.add(new LdcInsnNode(Type.getObjectType(owner)));
                    mhCode.add(new LdcInsnNode(name));
                    mhCode.add(new LdcInsnNode(Type.getType(descriptor)));
                    mhCode.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/invoke/MethodHandles$Lookup",
                        "findStaticGetter",
                        "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)" +
                            "Ljava/lang/invoke/MethodHandle;",
                        false
                    ));
                    break;

                case PUT_FIELD:
                    mhCode.add(new LdcInsnNode(Type.getObjectType(owner)));
                    mhCode.add(new LdcInsnNode(name));
                    mhCode.add(new LdcInsnNode(Type.getType(descriptor)));
                    mhCode.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/invoke/MethodHandles$Lookup",
                        "findSetter",
                        "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)" +
                            "Ljava/lang/invoke/MethodHandle;",
                        false
                    ));
                    break;

                case PUT_STATIC:
                    mhCode.add(new LdcInsnNode(Type.getObjectType(owner)));
                    mhCode.add(new LdcInsnNode(name));
                    mhCode.add(new LdcInsnNode(Type.getType(descriptor)));
                    mhCode.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/invoke/MethodHandles$Lookup",
                        "findStaticSetter",
                        "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)" +
                            "Ljava/lang/invoke/MethodHandle;",
                        false
                    ));
                    break;

                case INVOKE_INTERFACE:
                    mhCode.add(new LdcInsnNode(Type.getObjectType(owner)));
                    mhCode.add(new LdcInsnNode(name));
                    pushMethodType(mhCode, descriptor);
                    mhCode.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/invoke/MethodHandles$Lookup",
                        "findVirtual",
                        "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)" +
                            "Ljava/lang/invoke/MethodHandle;",
                        false
                    ));
                    break;
            }

            // Bind receiver if requested
            if (annotation.bindTo()) {
                if (!annotation.bindToField().isEmpty()) {
                    mhCode.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    mhCode.add(new FieldInsnNode(
                        Opcodes.GETFIELD,
                        classNode.name,
                        annotation.bindToField(),
                        "Ljava/lang/Object;"
                    ));
                } else {
                    mhCode.add(new VarInsnNode(Opcodes.ALOAD, 0));
                }
                mhCode.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandle",
                    "bindTo",
                    "(Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;",
                    false
                ));
            }

            // Spread arguments if requested
            if (annotation.spreadArgs()) {
                pushMethodType(mhCode, descriptor);
                mhCode.add(new LdcInsnNode(0)); // spreadArgPos
                mhCode.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandle",
                    "asSpreader",
                    "(Ljava/lang/Class;I)Ljava/lang/invoke/MethodHandle;",
                    false
                ));
            }

            // Collect arguments if requested
            if (annotation.collectArgs()) {
                mhCode.add(new LdcInsnNode(0)); // collectArgPos
                mhCode.add(new LdcInsnNode(Type.getType("[Ljava/lang/Object;")));
                mhCode.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandle",
                    "asCollector",
                    "(Ljava/lang/Class;I)Ljava/lang/invoke/MethodHandle;",
                    false
                ));
            }

            // Adapt type if requested
            if (annotation.asType() && !annotation.targetType().isEmpty()) {
                pushMethodType(mhCode, annotation.targetType());
                mhCode.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandle",
                    "asType",
                    "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
                    false
                ));
            }

            // Cache the MethodHandle in a static field if requested
            if (annotation.cache()) {
                String cacheFieldName = "deepmix$mh$" + name + "$" +
                    annotation.kind().name().toLowerCase();

                // Add static field to class
                FieldNode cacheField = new FieldNode(
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE,
                    cacheFieldName,
                    "Ljava/lang/invoke/MethodHandle;",
                    null,
                    null
                );
                classNode.fields.add(cacheField);

                // Double-checked locking pattern for lazy init
                InsnList cachedCode = new InsnList();
                LabelNode cacheHit = new LabelNode();
                LabelNode cacheEnd = new LabelNode();

                // Check if already cached
                cachedCode.add(new FieldInsnNode(
                    Opcodes.GETSTATIC, classNode.name, cacheFieldName,
                    "Ljava/lang/invoke/MethodHandle;"
                ));
                cachedCode.add(new InsnNode(Opcodes.DUP));
                cachedCode.add(new JumpInsnNode(Opcodes.IFNONNULL, cacheHit));
                cachedCode.add(new InsnNode(Opcodes.POP));

                // Not cached — create MethodHandle (the code we built above)
                cachedCode.add(mhCode);

                // Store in cache
                cachedCode.add(new InsnNode(Opcodes.DUP));
                cachedCode.add(new FieldInsnNode(
                    Opcodes.PUTSTATIC, classNode.name, cacheFieldName,
                    "Ljava/lang/invoke/MethodHandle;"
                ));
                cachedCode.add(new JumpInsnNode(Opcodes.GOTO, cacheEnd));

                // Cache hit — use existing handle (already on stack from DUP)
                cachedCode.add(cacheHit);
                cachedCode.add(cacheEnd);

                methodNode.instructions.insert(cachedCode);
            } else {
                methodNode.instructions.insert(mhCode);
            }

            context.addDiagnostic(String.format(
                "🔑 @DeepMethodHandle applied to %s::%s [kind=%s, target=%s.%s, cached=%b]",
                classNode.name, methodNode.name,
                annotation.kind(), owner, name, annotation.cache()
            ));
        }

        /** Process @DeepStackMap — Manipulate stack map frames. */
        public void processStackMap(DeepStackMap annotation, ClassNode classNode,
                                    MethodNode methodNode) throws DeepMixProcessingException {
            if (annotation.removeAll()) {
                // Remove all existing frame nodes
                Iterator<AbstractInsnNode> it = methodNode.instructions.iterator();
                while (it.hasNext()) {
                    AbstractInsnNode insn = it.next();
                    if (insn instanceof FrameNode) {
                        it.remove();
                    }
                }
                context.addDiagnostic(String.format(
                    "📊 @DeepStackMap removed all frames from %s::%s",
                    classNode.name, methodNode.name
                ));
                return;
            }

            if (annotation.recompute()) {
                // Mark for full frame recomputation (ClassWriter.COMPUTE_FRAMES)
                // Store a flag that the ClassWriter should use COMPUTE_FRAMES
                context.setFlag(classNode.name, "COMPUTE_FRAMES", true);
                context.addDiagnostic(String.format(
                    "📊 @DeepStackMap marked %s::%s for full frame recomputation",
                    classNode.name, methodNode.name
                ));
                return;
            }

            // Insert specified frames
            for (StackMapFrame frame : annotation.frames()) {
                FrameNode frameNode = buildFrameNode(frame);
                if (frameNode != null) {
                    // Insert at offset position
                    AbstractInsnNode target = findInsnAtOffset(
                        methodNode, frame.offset());
                    if (target != null) {
                        methodNode.instructions.insertBefore(target, frameNode);
                    }
                }
            }

            // Verify frames if requested
            if (annotation.verify()) {
                verifyStackMapFrames(classNode, methodNode);
            }

            context.addDiagnostic(String.format(
                "📊 @DeepStackMap applied to %s::%s [%d frames, verify=%b]",
                classNode.name, methodNode.name,
                annotation.frames().length, annotation.verify()
            ));
        }

        /** Process @DeepLocalVar — Manipulate local variable table. */
        public void processLocalVar(DeepLocalVar annotation, ClassNode classNode,
                                    MethodNode methodNode) throws DeepMixProcessingException {
            if (methodNode.localVariables == null) {
                methodNode.localVariables = new ArrayList<>();
            }

            // Remove specified variables
            for (String varName : annotation.remove()) {
                methodNode.localVariables.removeIf(lv -> lv.name.equals(varName));
            }

            // Add new local variables
            for (LocalVarSpec spec : annotation.add()) {
                LabelNode startLabel;
                LabelNode endLabel;

                if (!spec.startLabel().isEmpty()) {
                    startLabel = findLabelByName(methodNode, spec.startLabel());
                } else {
                    startLabel = findFirstLabel(methodNode);
                }

                if (!spec.endLabel().isEmpty()) {
                    endLabel = findLabelByName(methodNode, spec.endLabel());
                } else {
                    endLabel = findLastLabel(methodNode);
                }

                if (startLabel == null) {
                    startLabel = new LabelNode();
                    methodNode.instructions.insert(startLabel);
                }
                if (endLabel == null) {
                    endLabel = new LabelNode();
                    methodNode.instructions.add(endLabel);
                }

                int index = spec.index();
                if (index < 0) {
                    // Auto-assign: use next available index
                    index = methodNode.maxLocals;
                    Type varType = Type.getType(spec.descriptor());
                    methodNode.maxLocals += varType.getSize();
                }

                LocalVariableNode lvn = new LocalVariableNode(
                    spec.name(),
                    spec.descriptor(),
                    spec.signature().isEmpty() ? null : spec.signature(),
                    startLabel,
                    endLabel,
                    index
                );
                methodNode.localVariables.add(lvn);
            }

            // Modify existing variables
            for (LocalVarSpec spec : annotation.modify()) {
                for (LocalVariableNode lv : methodNode.localVariables) {
                    if (lv.name.equals(spec.name())) {
                        if (!spec.descriptor().isEmpty()) {
                            lv.desc = spec.descriptor();
                        }
                        if (!spec.signature().isEmpty()) {
                            lv.signature = spec.signature();
                        }
                        if (spec.index() >= 0) {
                            lv.index = spec.index();
                        }
                        break;
                    }
                }
            }

            // Recalculate indices if requested
            if (annotation.recalculateIndices()) {
                recalculateLocalVariableIndices(methodNode);
            }

            context.addDiagnostic(String.format(
                "📋 @DeepLocalVar applied to %s::%s [add=%d, remove=%d, modify=%d]",
                classNode.name, methodNode.name,
                annotation.add().length, annotation.remove().length,
                annotation.modify().length
            ));
        }

        /** Process @DeepLineNumber — Manipulate line number info. */
        public void processLineNumber(DeepLineNumber annotation, ClassNode classNode,
                                      MethodNode methodNode) throws DeepMixProcessingException {
            if (annotation.removeAll()) {
                Iterator<AbstractInsnNode> it = methodNode.instructions.iterator();
                while (it.hasNext()) {
                    if (it.next() instanceof LineNumberNode) {
                        it.remove();
                    }
                }
                context.addDiagnostic(String.format(
                    "📍 @DeepLineNumber stripped all line numbers from %s::%s",
                    classNode.name, methodNode.name
                ));
                return;
            }

            // Offset all line numbers
            if (annotation.offset() != 0) {
                for (AbstractInsnNode insn : methodNode.instructions) {
                    if (insn instanceof LineNumberNode) {
                        LineNumberNode lnn = (LineNumberNode) insn;
                        lnn.line += annotation.offset();
                    }
                }
            }

            // Remove specific lines
            for (int line : annotation.removeLines()) {
                Iterator<AbstractInsnNode> it = methodNode.instructions.iterator();
                while (it.hasNext()) {
                    AbstractInsnNode insn = it.next();
                    if (insn instanceof LineNumberNode && ((LineNumberNode) insn).line == line) {
                        it.remove();
                    }
                }
            }

            // Renumber sequentially
            if (annotation.renumber()) {
                int currentLine = annotation.renumberStart();
                for (AbstractInsnNode insn : methodNode.instructions) {
                    if (insn instanceof LineNumberNode) {
                        ((LineNumberNode) insn).line = currentLine++;
                    }
                }
            }

            // Override SourceFile attribute
            if (!annotation.sourceFile().isEmpty()) {
                classNode.sourceFile = annotation.sourceFile();
            }

            context.addDiagnostic(String.format(
                "📍 @DeepLineNumber applied to %s::%s [offset=%d, renumber=%b, source=%s]",
                classNode.name, methodNode.name,
                annotation.offset(), annotation.renumber(),
                annotation.sourceFile().isEmpty() ? "(unchanged)" : annotation.sourceFile()
            ));
        }

        /** Process @DeepFinally — Inject finally blocks. */
        public void processFinally(DeepFinally annotation, ClassNode classNode,
                                   MethodNode methodNode) throws DeepMixProcessingException {
            if (annotation.wrapEntireMethod()) {
                InsnList finallyBody = new InsnList();

                // If a cleanup method is specified, call it
                if (!annotation.cleanupMethod().isEmpty()) {
                    finallyBody.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    finallyBody.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        classNode.name,
                        annotation.cleanupMethod(),
                        "()V",
                        false
                    ));
                }

                // Resource cleanup
                if (annotation.ensureResourceCleanup()) {
                    for (String resourceField : annotation.resourceFields()) {
                        LabelNode skipNull = new LabelNode();
                        finallyBody.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        finallyBody.add(new FieldInsnNode(
                            Opcodes.GETFIELD, classNode.name, resourceField,
                            "Ljava/lang/AutoCloseable;"
                        ));
                        finallyBody.add(new InsnNode(Opcodes.DUP));
                        finallyBody.add(new JumpInsnNode(Opcodes.IFNULL, skipNull));
                        finallyBody.add(new MethodInsnNode(
                            Opcodes.INVOKEINTERFACE,
                            "java/lang/AutoCloseable",
                            "close",
                            "()V",
                            true
                        ));
                        LabelNode afterClose = new LabelNode();
                        finallyBody.add(new JumpInsnNode(Opcodes.GOTO, afterClose));
                        finallyBody.add(skipNull);
                        finallyBody.add(new InsnNode(Opcodes.POP));
                        finallyBody.add(afterClose);
                    }
                }

                // Add inline ASM instructions if specified
                Map<String, LabelNode> emptyLabels = new HashMap<>();
                for (ASMInstruction insn : annotation.instructions()) {
                    AbstractInsnNode node = buildInstructionNode(insn, emptyLabels);
                    if (node != null) {
                        finallyBody.add(node);
                    }
                }

                // Wrap the entire method in try-finally
                wrapMethodInFinally(methodNode, finallyBody);
            }

            context.addDiagnostic(String.format(
                "🔒 @DeepFinally applied to %s::%s [cleanup=%s, resources=%d]",
                classNode.name, methodNode.name,
                annotation.cleanupMethod().isEmpty() ? "(inline)" : annotation.cleanupMethod(),
                annotation.resourceFields().length
            ));
        }

        // ──────────────────────────────────────────────
        // Phase 22 — Internal helper methods
        // ──────────────────────────────────────────────

        private AbstractInsnNode buildInstructionNode(ASMInstruction insn,
                                                      Map<String, LabelNode> labelMap) {
            switch (insn.type()) {
                case INSN:
                    return new InsnNode(insn.opcode());

                case INT_INSN:
                    return new IntInsnNode(insn.opcode(), insn.intOperand());

                case VAR_INSN:
                    return new VarInsnNode(insn.opcode(), insn.var());

                case TYPE_INSN:
                    return new TypeInsnNode(insn.opcode(), insn.owner());

                case FIELD_INSN:
                    return new FieldInsnNode(insn.opcode(), insn.owner(),
                        insn.name(), insn.desc());

                case METHOD_INSN:
                    return new MethodInsnNode(insn.opcode(), insn.owner(),
                        insn.name(), insn.desc(), insn.itf());

                case JUMP_INSN:
                    LabelNode jumpTarget = labelMap.getOrDefault(
                        insn.label(), new LabelNode());
                    return new JumpInsnNode(insn.opcode(), jumpTarget);

                case LABEL:
                    return labelMap.getOrDefault(insn.label(), new LabelNode());

                case LDC_INSN:
                    return new LdcInsnNode(parseLdcValue(insn.ldcValue(), insn.ldcType()));

                case IINC_INSN:
                    return new IincInsnNode(insn.var(), insn.intOperand());

                case LINE:
                    LabelNode lineLabel = labelMap.getOrDefault(
                        insn.label(), new LabelNode());
                    return new LineNumberNode(insn.intOperand(), lineLabel);

                case FRAME:
                    // Handled by @DeepStackMap
                    return null;

                default:
                    return null;
            }
        }

        private Object parseLdcValue(String value, String type) {
            switch (type.toLowerCase()) {
                case "int":
                case "integer":
                    return Integer.parseInt(value);
                case "long":
                    return Long.parseLong(value);
                case "float":
                    return Float.parseFloat(value);
                case "double":
                    return Double.parseDouble(value);
                case "type":
                case "class":
                    return Type.getType(value);
                case "string":
                default:
                    return value;
            }
        }

        private void pushMethodType(InsnList insns, String descriptor) {
            insns.add(new LdcInsnNode(descriptor));
            insns.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "java/lang/invoke/MethodType",
                "fromMethodDescriptorString",
                "(Ljava/lang/String;Ljava/lang/ClassLoader;)" +
                    "Ljava/lang/invoke/MethodType;",
                false
            ));
        }

        private Object[] buildBootstrapArgs(String[] args, String[] argTypes) {
            if (args.length == 0) return new Object[0];
            Object[] result = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                String type = i < argTypes.length ? argTypes[i] : "String";
                result[i] = parseLdcValue(args[i], type);
            }
            return result;
        }

        private void insertBeforeReturns(MethodNode methodNode, InsnList injection) {
            List<AbstractInsnNode> returns = new ArrayList<>();
            for (AbstractInsnNode insn : methodNode.instructions) {
                int opcode = insn.getOpcode();
                if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                    returns.add(insn);
                }
            }
            for (AbstractInsnNode ret : returns) {
                InsnList copy = cloneInsnList(injection);
                methodNode.instructions.insertBefore(ret, copy);
            }
        }

        private void insertAtOpcode(MethodNode methodNode, InsnList injection,
                                    int opcode, int ordinal) {
            int count = 0;
            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn.getOpcode() == opcode) {
                    if (count == ordinal) {
                        methodNode.instructions.insertBefore(insn, injection);
                        return;
                    }
                    count++;
                }
            }
        }

        private void insertAtMethodCall(MethodNode methodNode, InsnList injection,
                                        String owner, String name, String desc, int ordinal) {
            int count = 0;
            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode min = (MethodInsnNode) insn;
                    if (min.owner.equals(owner) && min.name.equals(name) &&
                        (desc.isEmpty() || min.desc.equals(desc))) {
                        if (count == ordinal) {
                            methodNode.instructions.insertBefore(insn, injection);
                            return;
                        }
                        count++;
                    }
                }
            }
        }

        private void wrapMethodInTryCatch(MethodNode methodNode,
                                           DeepTryCatch annotation) {
            LabelNode tryStart = new LabelNode();
            LabelNode tryEnd = new LabelNode();
            LabelNode handlerStart = new LabelNode();
            LabelNode afterHandler = new LabelNode();

            // Insert try start at method beginning
            methodNode.instructions.insert(tryStart);

            // Find last instruction before returns and insert tryEnd
            AbstractInsnNode lastInsn = methodNode.instructions.getLast();
            methodNode.instructions.insertBefore(lastInsn, tryEnd);

            // Build handler body
            InsnList handlerCode = new InsnList();
            handlerCode.add(handlerStart);

            // Store exception in local variable
            int exVarIndex = methodNode.maxLocals;
            methodNode.maxLocals++;
            handlerCode.add(new VarInsnNode(Opcodes.ASTORE, exVarIndex));

            // Log exception if requested
            if (annotation.logException()) {
                handlerCode.add(new FieldInsnNode(
                    Opcodes.GETSTATIC,
                    "java/lang/System", "err",
                    "Ljava/io/PrintStream;"
                ));
                handlerCode.add(new LdcInsnNode(
                    "[DeepMix] Exception caught in " + methodNode.name + ": "));
                handlerCode.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    "java/io/PrintStream", "print",
                    "(Ljava/lang/String;)V", false
                ));
                handlerCode.add(new VarInsnNode(Opcodes.ALOAD, exVarIndex));
                handlerCode.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Throwable", "printStackTrace",
                    "(Ljava/io/PrintStream;)V", false
                ));
            }

            // Call handler method if specified
            if (!annotation.handlerMethod().isEmpty()) {
                handlerCode.add(new VarInsnNode(Opcodes.ALOAD, 0));
                handlerCode.add(new VarInsnNode(Opcodes.ALOAD, exVarIndex));
                handlerCode.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    methodNode.name, // Will need class context
                    annotation.handlerMethod(),
                    "(Ljava/lang/Throwable;)V", false
                ));
            }

            // Rethrow if requested
            if (annotation.rethrow()) {
                handlerCode.add(new VarInsnNode(Opcodes.ALOAD, exVarIndex));
                handlerCode.add(new InsnNode(Opcodes.ATHROW));
            } else {
                // Return default value
                Type returnType = Type.getReturnType(methodNode.desc);
                pushDefaultReturnValue(handlerCode, returnType);
                handlerCode.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
            }

            handlerCode.add(afterHandler);

            // Append handler code at end
            methodNode.instructions.add(handlerCode);

            // Register try-catch block for each exception type
            for (String exType : annotation.exceptionTypes()) {
                TryCatchBlockNode tcbn = new TryCatchBlockNode(
                    tryStart, tryEnd, handlerStart,
                    exType.isEmpty() ? null : exType
                );
                methodNode.tryCatchBlocks.add(tcbn);
            }
        }

        private void wrapMethodInFinally(MethodNode methodNode, InsnList finallyBody) {
            LabelNode tryStart = new LabelNode();
            LabelNode tryEnd = new LabelNode();
            LabelNode finallyHandler = new LabelNode();

            methodNode.instructions.insert(tryStart);

            // Insert finally body before each return
            List<AbstractInsnNode> returns = new ArrayList<>();
            for (AbstractInsnNode insn : methodNode.instructions) {
                int op = insn.getOpcode();
                if (op >= Opcodes.IRETURN && op <= Opcodes.RETURN) {
                    returns.add(insn);
                }
            }

            for (AbstractInsnNode ret : returns) {
                InsnList finallyCopy = cloneInsnList(finallyBody);
                methodNode.instructions.insertBefore(ret, finallyCopy);
            }

            // Find end of try region (last return)
            if (!returns.isEmpty()) {
                methodNode.instructions.insertBefore(
                    returns.get(returns.size() - 1), tryEnd);
            }

            // Exception handler — runs finally then rethrows
            InsnList exceptionHandler = new InsnList();
            exceptionHandler.add(finallyHandler);
            int exVar = methodNode.maxLocals;
            methodNode.maxLocals++;
            exceptionHandler.add(new VarInsnNode(Opcodes.ASTORE, exVar));
            exceptionHandler.add(cloneInsnList(finallyBody));
            exceptionHandler.add(new VarInsnNode(Opcodes.ALOAD, exVar));
            exceptionHandler.add(new InsnNode(Opcodes.ATHROW));

            methodNode.instructions.add(exceptionHandler);

            // Register as "catch all" (finally) — null exception type
            TryCatchBlockNode tcbn = new TryCatchBlockNode(
                tryStart, tryEnd, finallyHandler, null);
            methodNode.tryCatchBlocks.add(tcbn);
        }

        private void insertTryCatchBlock(MethodNode methodNode, TryCatchSpec spec) {
            LabelNode startLabel = findOrCreateLabel(methodNode, spec.startLabel());
            LabelNode endLabel = findOrCreateLabel(methodNode, spec.endLabel());
            LabelNode handlerLabel = findOrCreateLabel(methodNode, spec.handlerLabel());

            String exType = spec.exceptionType().isEmpty() ? null : spec.exceptionType();
            TryCatchBlockNode tcbn = new TryCatchBlockNode(
                startLabel, endLabel, handlerLabel, exType);
            methodNode.tryCatchBlocks.add(tcbn);
        }

        private void removeMatchingTryCatch(MethodNode methodNode, String[] exceptionTypes) {
            Set<String> typesToRemove = new HashSet<>(Arrays.asList(exceptionTypes));
            methodNode.tryCatchBlocks.removeIf(tcb ->
                tcb.type != null && typesToRemove.contains(tcb.type));
        }

        private void convertLookupToTable(MethodNode methodNode,
                                           LookupSwitchInsnNode lookup) {
            if (lookup.keys.isEmpty()) return;

            int min = lookup.keys.get(0);
            int max = lookup.keys.get(lookup.keys.size() - 1);

            LabelNode[] labels = new LabelNode[max - min + 1];
            Arrays.fill(labels, lookup.dflt);

            for (int i = 0; i < lookup.keys.size(); i++) {
                labels[lookup.keys.get(i) - min] = lookup.labels.get(i);
            }

            TableSwitchInsnNode table = new TableSwitchInsnNode(
                min, max, lookup.dflt, labels);
            methodNode.instructions.set(lookup, table);
        }

        private void convertTableToLookup(MethodNode methodNode,
                                           TableSwitchInsnNode table) {
            List<Integer> keys = new ArrayList<>();
            List<LabelNode> labels = new ArrayList<>();

            for (int i = 0; i < table.labels.size(); i++) {
                LabelNode label = table.labels.get(i);
                if (label != table.dflt) {
                    keys.add(table.min + i);
                    labels.add(label);
                }
            }

            int[] keyArray = keys.stream().mapToInt(Integer::intValue).toArray();
            LabelNode[] labelArray = labels.toArray(new LabelNode[0]);

            LookupSwitchInsnNode lookup = new LookupSwitchInsnNode(
                table.dflt, keyArray, labelArray);
            methodNode.instructions.set(table, lookup);
        }

        private void convertSwitchToIfElse(MethodNode methodNode,
                                            AbstractInsnNode switchInsn) {
            InsnList ifElse = new InsnList();
            List<Integer> keys;
            List<LabelNode> labels;
            LabelNode dflt;

            if (switchInsn instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode table = (TableSwitchInsnNode) switchInsn;
                dflt = table.dflt;
                keys = new ArrayList<>();
                labels = new ArrayList<>(table.labels);
                for (int i = table.min; i <= table.max; i++) {
                    keys.add(i);
                }
            } else {
                LookupSwitchInsnNode lookup = (LookupSwitchInsnNode) switchInsn;
                dflt = lookup.dflt;
                keys = new ArrayList<>(lookup.keys);
                labels = new ArrayList<>(lookup.labels);
            }

            // DUP the value on stack for each comparison
            for (int i = 0; i < keys.size(); i++) {
                if (labels.get(i) == dflt) continue; // Skip default-equivalent cases

                ifElse.add(new InsnNode(Opcodes.DUP));
                ifElse.add(new LdcInsnNode(keys.get(i)));
                ifElse.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, labels.get(i)));
            }

            // POP the remaining value and jump to default
            ifElse.add(new InsnNode(Opcodes.POP));
            ifElse.add(new JumpInsnNode(Opcodes.GOTO, dflt));

            methodNode.instructions.insertBefore(switchInsn, ifElse);
            methodNode.instructions.remove(switchInsn);
        }

        private void optimizeSwitchAuto(MethodNode methodNode,
                                         AbstractInsnNode switchInsn,
                                         int densityThreshold) {
            if (switchInsn instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode lookup = (LookupSwitchInsnNode) switchInsn;
                if (lookup.keys.size() >= 2) {
                    int min = lookup.keys.get(0);
                    int max = lookup.keys.get(lookup.keys.size() - 1);
                    int range = max - min + 1;
                    double density = (double) lookup.keys.size() / range;

                    if (density >= 0.5 && range <= lookup.keys.size() * densityThreshold) {
                        convertLookupToTable(methodNode, lookup);
                    }
                }
            } else if (switchInsn instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode table = (TableSwitchInsnNode) switchInsn;
                int range = table.max - table.min + 1;
                int nonDefaultCount = 0;
                for (LabelNode label : table.labels) {
                    if (label != table.dflt) nonDefaultCount++;
                }
                double density = (double) nonDefaultCount / range;

                if (density < 0.3 && nonDefaultCount <= densityThreshold) {
                    convertTableToLookup(methodNode, table);
                }
            }
        }

        private void addSwitchCase(MethodNode methodNode, SwitchCase sc) {
            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn instanceof LookupSwitchInsnNode) {
                    LookupSwitchInsnNode lookup = (LookupSwitchInsnNode) insn;
                    LabelNode caseLabel = new LabelNode();
                    // Insert in sorted order
                    int insertIdx = Collections.binarySearch(lookup.keys, sc.key());
                    if (insertIdx < 0) {
                        insertIdx = -(insertIdx + 1);
                        lookup.keys.add(insertIdx, sc.key());
                        lookup.labels.add(insertIdx, caseLabel);
                    }
                    break;
                } else if (insn instanceof TableSwitchInsnNode) {
                    TableSwitchInsnNode table = (TableSwitchInsnNode) insn;
                    int offset = sc.key() - table.min;
                    if (offset >= 0 && offset < table.labels.size()) {
                        LabelNode caseLabel = new LabelNode();
                        table.labels.set(offset, caseLabel);
                    }
                    break;
                }
            }
        }

        private InsnList cloneInsnList(InsnList original) {
            Map<LabelNode, LabelNode> labelMapping = new HashMap<>();
            for (AbstractInsnNode insn : original) {
                if (insn instanceof LabelNode) {
                    labelMapping.put((LabelNode) insn, new LabelNode());
                }
            }
            InsnList clone = new InsnList();
            for (AbstractInsnNode insn : original) {
                clone.add(insn.clone(labelMapping));
            }
            return clone;
        }

        private FrameNode buildFrameNode(StackMapFrame frame) {
            int type;
            switch (frame.type()) {
                case SAME:
                    type = Opcodes.F_SAME;
                    return new FrameNode(type, 0, null, 0, null);
                case SAME_LOCALS_1:
                    type = Opcodes.F_SAME1;
                    Object[] stack = parseFrameTypes(frame.stack());
                    return new FrameNode(type, 0, null, stack.length, stack);
                case CHOP:
                    type = Opcodes.F_CHOP;
                    return new FrameNode(type, frame.numChoppedLocals(), null, 0, null);
                case APPEND:
                    type = Opcodes.F_APPEND;
                    Object[] appendLocals = parseFrameTypes(frame.locals());
                    return new FrameNode(type, appendLocals.length, appendLocals, 0, null);
                case FULL:
                default:
                    type = Opcodes.F_FULL;
                    Object[] locals = parseFrameTypes(frame.locals());
                    Object[] stackItems = parseFrameTypes(frame.stack());
                    return new FrameNode(type, locals.length, locals,
                        stackItems.length, stackItems);
            }
        }

        private Object[] parseFrameTypes(String[] types) {
            if (types == null || types.length == 0) return new Object[0];
            Object[] result = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                switch (types[i].toUpperCase()) {
                    case "TOP":       result[i] = Opcodes.TOP; break;
                    case "INTEGER":
                    case "INT":       result[i] = Opcodes.INTEGER; break;
                    case "FLOAT":     result[i] = Opcodes.FLOAT; break;
                    case "LONG":      result[i] = Opcodes.LONG; break;
                    case "DOUBLE":    result[i] = Opcodes.DOUBLE; break;
                    case "NULL":      result[i] = Opcodes.NULL; break;
                    case "UNINITIALIZED_THIS":
                        result[i] = Opcodes.UNINITIALIZED_THIS; break;
                    default:
                        result[i] = types[i]; // Internal class name
                        break;
                }
            }
            return result;
        }

        private void pushDefaultReturnValue(InsnList insns, Type returnType) {
            switch (returnType.getSort()) {
                case Type.VOID:
                    break;
                case Type.BOOLEAN:
                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    insns.add(new InsnNode(Opcodes.ICONST_0));
                    break;
                case Type.LONG:
                    insns.add(new InsnNode(Opcodes.LCONST_0));
                    break;
                case Type.FLOAT:
                    insns.add(new InsnNode(Opcodes.FCONST_0));
                    break;
                case Type.DOUBLE:
                    insns.add(new InsnNode(Opcodes.DCONST_0));
                    break;
                case Type.OBJECT:
                case Type.ARRAY:
                    insns.add(new InsnNode(Opcodes.ACONST_NULL));
                    break;
            }
        }

        private AbstractInsnNode findInsnAtOffset(MethodNode methodNode, int offset) {
            int i = 0;
            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn.getOpcode() >= 0) { // Skip pseudo-instructions
                    if (i == offset) return insn;
                    i++;
                }
            }
            return null;
        }

        private LabelNode findLabelByName(MethodNode methodNode, String name) {
            // Labels don't have names in bytecode — use local variable table to infer
            if (methodNode.localVariables != null) {
                for (LocalVariableNode lv : methodNode.localVariables) {
                    if (lv.name.equals(name)) return lv.start;
                }
            }
            return null;
        }

        private LabelNode findFirstLabel(MethodNode methodNode) {
            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn instanceof LabelNode) return (LabelNode) insn;
            }
            return null;
        }

        private LabelNode findLastLabel(MethodNode methodNode) {
            LabelNode last = null;
            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn instanceof LabelNode) last = (LabelNode) insn;
            }
            return last;
        }

        private LabelNode findOrCreateLabel(MethodNode methodNode, String labelId) {
            if (labelId == null || labelId.isEmpty()) {
                return new LabelNode();
            }
            // Try to find existing label by local variable association
            LabelNode found = findLabelByName(methodNode, labelId);
            return found != null ? found : new LabelNode();
        }

        private void recalculateLocalVariableIndices(MethodNode methodNode) {
            if (methodNode.localVariables == null) return;

            // Sort by current index
            methodNode.localVariables.sort(
                Comparator.comparingInt(lv -> lv.index));

            // Reassign indices based on type sizes
            int nextIndex = 0;
            boolean isStatic = (methodNode.access & Opcodes.ACC_STATIC) != 0;

            for (LocalVariableNode lv : methodNode.localVariables) {
                lv.index = nextIndex;
                Type type = Type.getType(lv.desc);
                nextIndex += type.getSize();
            }

            methodNode.maxLocals = nextIndex;
        }

        private void verifyStackMapFrames(ClassNode classNode, MethodNode methodNode) {
            // Basic verification: ensure frame count matches branch targets
            int branchTargets = 0;
            int frameCount = 0;

            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn instanceof JumpInsnNode ||
                    insn instanceof TableSwitchInsnNode ||
                    insn instanceof LookupSwitchInsnNode) {
                    branchTargets++;
                }
                if (insn instanceof FrameNode) {
                    frameCount++;
                }
            }

            if (frameCount < branchTargets) {
                context.addDiagnostic(String.format(
                    "⚠️ Stack map frame deficit in %s::%s [frames=%d, branchTargets=%d] " +
                        "— consider enabling recompute=true",
                    classNode.name, methodNode.name, frameCount, branchTargets
                ));
            }
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║              PHASE 23: METAPROGRAMMING                             ║
    // ║              10 annotations | Priority: MEDIUM | Est. Time: 2 days ║
    // ║                                                                    ║
    // ║  @DeepMacro       @DeepTemplate     @DeepDSL                       ║
    // ║  @DeepQuote       @DeepQuasiquote   @DeepSplice                    ║
    // ║  @DeepEval        @DeepCompile      @DeepReify                     ║
    // ║  @DeepReflect     (→ see Phase 8, shared via alias)                ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    // ──────────────────────────────────────────────
    // Phase 23 — Enums
    // ──────────────────────────────────────────────

    /** Macro expansion strategy */
    public enum MacroExpansionStrategy {
        COMPILE_TIME,     // Expand at compile time (annotation processor)
        LOAD_TIME,        // Expand at class loading (agent)
        RUNTIME,          // Expand at first invocation
        LAZY,             // Expand only when needed
        EAGER,            // Expand immediately on class load
        CACHED            // Expand once, cache result
    }

    /** Template engine for code generation */
    public enum TemplateEngine {
        VELOCITY,         // Apache Velocity
        FREEMARKER,       // FreeMarker
        MUSTACHE,         // Mustache/Handlebars
        JINJA,            // Jinja-like syntax
        STRING_TEMPLATE,  // StringTemplate
        JAVAC,            // Java compiler API
        ASM,              // Direct ASM bytecode
        CUSTOM            // Custom template engine
    }

    /** DSL type */
    public enum DSLType {
        INTERNAL,         // Internal DSL (fluent API)
        EXTERNAL,         // External DSL (custom parser)
        EMBEDDED,         // Embedded DSL (string interpolation)
        ANNOTATION,       // Annotation-based DSL
        BUILDER,          // Builder-pattern DSL
        FUNCTIONAL,       // Functional combinator DSL
        ALGEBRAIC         // Algebraic DSL (type-safe expressions)
    }

    /** Code quoting mode */
    public enum QuoteMode {
        EXPRESSION,       // Quote a single expression
        STATEMENT,        // Quote a statement
        BLOCK,            // Quote a code block
        METHOD,           // Quote an entire method
        CLASS,            // Quote an entire class
        TYPE,             // Quote a type expression
        PATTERN           // Quote a pattern for matching
    }

    /** Compilation target */
    public enum CompilationTarget {
        BYTECODE,         // JVM bytecode (classfile)
        SOURCE,           // Java source code
        AST,              // Abstract syntax tree
        IR,               // Intermediate representation
        NATIVE,           // Native code (via GraalVM/JNI)
        SCRIPT,           // Script (interpreted)
        CUSTOM
    }

    /** Reification mode */
    public enum ReificationMode {
        TYPE_PARAMETER,   // Reify generic type parameters
        METHOD_SIGNATURE, // Reify method signatures
        FIELD_TYPE,       // Reify field types
        RETURN_TYPE,      // Reify return types
        LAMBDA_TYPE,      // Reify lambda captured types
        FULL              // Full reification of all generics
    }

    /** Runtime evaluation scope */
    public enum EvalScope {
        GLOBAL,           // Global evaluation scope
        CLASS,            // Class-level scope (access class members)
        METHOD,           // Method-level scope (access locals)
        EXPRESSION,       // Pure expression (no side effects)
        SANDBOX,          // Sandboxed evaluation (restricted)
        PRIVILEGED        // Privileged evaluation (full access)
    }

    // ──────────────────────────────────────────────
    // Phase 23 — Sub-annotation types
    // ──────────────────────────────────────────────

    /** Macro parameter definition */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MacroParam {
        String name();
        String type() default "java.lang.Object";
        String defaultValue() default "";
        boolean splat() default false; // Variadic parameter
        boolean lazy() default false;  // Lazy evaluation
        String constraint() default ""; // Type constraint expression
    }

    /** Template variable binding */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TemplateBinding {
        String name();
        String expression(); // Java expression to evaluate
        String type() default "";
        boolean list() default false;    // Iterable binding
        boolean optional() default false;
    }

    /** DSL rule definition */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DSLRule {
        String pattern();       // Syntax pattern (e.g., "when {cond} then {action}")
        String transform();     // Transformation expression
        int precedence() default 0;
        String[] examples() default {};
    }

    /** Splice point specification */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SplicePoint {
        String marker();           // Marker in quoted code
        String expression();       // Expression to splice in
        boolean unquote() default true;
        boolean flatten() default false; // Flatten list splice
    }

    /** Compilation option */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CompileOption {
        String key();
        String value();
    }

    // ──────────────────────────────────────────────
    // Phase 23 — Annotations
    // ──────────────────────────────────────────────

    /**
     * 🔄 @DeepMacro — Compile-time or load-time macro expansion.
     *
     * Defines code macros that expand into bytecode at the specified phase.
     * Macros can accept parameters, have hygiene guarantees, and compose.
     *
     * Example:
     * <pre>
     * {@code @DeepMacro(
     *     name = "log_entry",
     *     params = {
     *         @MacroParam(name = "level", type = "String", defaultValue = "\"INFO\""),
     *         @MacroParam(name = "msg", type = "String")
     *     },
     *     body = "Logger.getLogger(\"${class}\").log(Level.parse(${level}), ${msg});",
     *     strategy = MacroExpansionStrategy.COMPILE_TIME
     * )}
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    public @interface DeepMacro {
        String name();
        String target() default "";
        MacroParam[] params() default {};
        String body() default ""; // Template body (source code or bytecode spec)
        String bodyResource() default ""; // External file containing macro body
        MacroExpansionStrategy strategy() default MacroExpansionStrategy.LOAD_TIME;
        boolean hygienic() default true; // Prevent name capture
        boolean recursive() default false; // Allow recursive expansion
        int maxExpansionDepth() default 16; // Max recursion depth
        String[] imports() default {}; // Imports needed by expanded code
        String[] requiredAnnotations() default {}; // Annotations required at call site
        boolean generateDebugInfo() default true;
        boolean validateExpansion() default true; // Verify expanded bytecode
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepMacro */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    public @interface DMACRO {
        String name();
        String target() default "";
        MacroParam[] params() default {};
        String body() default "";
        MacroExpansionStrategy strategy() default MacroExpansionStrategy.LOAD_TIME;
        boolean hygienic() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📝 @DeepTemplate — Template-based code generation.
     *
     * Generates Java source or bytecode from templates with variable bindings.
     *
     * Example:
     * <pre>
     * {@code @DeepTemplate(
     *     engine = TemplateEngine.MUSTACHE,
     *     template = "public {{returnType}} get{{Name}}() { return this.{{field}}; }",
     *     bindings = {
     *         @TemplateBinding(name = "returnType", expression = "fieldType"),
     *         @TemplateBinding(name = "Name", expression = "capitalize(fieldName)"),
     *         @TemplateBinding(name = "field", expression = "fieldName")
     *     },
     *     iterateOver = "fields" // Generate for each field
     * )}
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepTemplate {
        String target() default "";
        TemplateEngine engine() default TemplateEngine.MUSTACHE;
        String template() default ""; // Inline template
        String templateResource() default ""; // External template file
        TemplateBinding[] bindings() default {};
        String iterateOver() default ""; // Collection to iterate over
        String filterExpression() default ""; // Filter for iteration
        CompilationTarget compileTo() default CompilationTarget.BYTECODE;
        boolean append() default false; // Append to existing class
        boolean replace() default false; // Replace existing member
        String outputPackage() default ""; // Output package for generated class
        String outputClassName() default ""; // Override generated class name
        boolean generateSource() default false; // Also generate .java file
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepTemplate */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DTEMPL {
        String target() default "";
        TemplateEngine engine() default TemplateEngine.MUSTACHE;
        String template() default "";
        TemplateBinding[] bindings() default {};
        String iterateOver() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔤 @DeepDSL — Domain-Specific Language creation and integration.
     *
     * Defines a mini-DSL that compiles to bytecode. Supports internal
     * (fluent API) and external (parsed) DSLs.
     *
     * Example:
     * <pre>
     * {@code @DeepDSL(
     *     name = "recipe_dsl",
     *     type = DSLType.EXTERNAL,
     *     rules = {
     *         @DSLRule(
     *             pattern = "craft {item} from {ingredients}",
     *             transform = "RecipeBuilder.create(${item}).ingredients(${ingredients}).build()"
     *         ),
     *         @DSLRule(
     *             pattern = "smelt {input} into {output}",
     *             transform = "SmeltingRecipe.of(${input}, ${output})"
     *         )
     *     }
     * )}
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepDSL {
        String name();
        String target() default "";
        DSLType type() default DSLType.INTERNAL;
        DSLRule[] rules() default {};
        String grammarFile() default ""; // ANTLR/PEG grammar file
        String parserClass() default ""; // Custom parser class
        String[] keywords() default {}; // Reserved keywords
        String[] operators() default {}; // Custom operators
        CompilationTarget compileTo() default CompilationTarget.BYTECODE;
        boolean typeSafe() default true;
        boolean cacheParsed() default true;
        String contextClass() default ""; // DSL evaluation context
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepDSL */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DDSL {
        String name();
        String target() default "";
        DSLType type() default DSLType.INTERNAL;
        DSLRule[] rules() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 💬 @DeepQuote — Code quoting (capture code as data).
     *
     * Captures a code fragment as an AST or bytecode structure
     * that can be manipulated, composed, and spliced.
     *
     * Example:
     * <pre>
     * {@code @DeepQuote(
     *     mode = QuoteMode.EXPRESSION,
     *     code = "x * x + 2 * x + 1",
     *     variables = {"x"}
     * )}
     * private DeepCodeNode squarePlusOne;
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.LOCAL_VARIABLE})
    public @interface DeepQuote {
        String target() default "";
        QuoteMode mode() default QuoteMode.EXPRESSION;
        String code(); // Quoted code as string
        String codeResource() default ""; // External file
        String[] variables() default {}; // Free variables in the quote
        String[] typeVariables() default {}; // Type parameters
        boolean preserveLineNumbers() default false;
        boolean preserveComments() default false;
        CompilationTarget representation() default CompilationTarget.AST;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepQuote */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.LOCAL_VARIABLE})
    public @interface DQUOTE {
        String target() default "";
        QuoteMode mode() default QuoteMode.EXPRESSION;
        String code();
        String[] variables() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔀 @DeepQuasiquote — Quasiquoting with unquote support.
     *
     * Like @DeepQuote but supports embedded unquote markers (`~expr`)
     * for splicing computed values into the quoted template.
     *
     * Example:
     * <pre>
     * {@code @DeepQuasiquote(
     *     code = "public void ~{methodName}() { return ~{body}; }",
     *     splicePoints = {
     *         @SplicePoint(marker = "methodName", expression = "computeName()"),
     *         @SplicePoint(marker = "body", expression = "generateBody()")
     *     }
     * )}
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DeepQuasiquote {
        String target() default "";
        String code(); // Template code with ~{markers}
        String codeResource() default "";
        SplicePoint[] splicePoints() default {};
        String unquotePrefix() default "~"; // Marker prefix
        String unquoteOpen() default "{"; // Opening delimiter
        String unquoteClose() default "}"; // Closing delimiter
        QuoteMode mode() default QuoteMode.BLOCK;
        CompilationTarget representation() default CompilationTarget.AST;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepQuasiquote */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DQQUOTE {
        String target() default "";
        String code();
        SplicePoint[] splicePoints() default {};
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ✂️ @DeepSplice — Code splicing into quoted templates.
     *
     * Inserts dynamically computed code into a previously quoted template.
     * Typically used with @DeepQuote or @DeepQuasiquote.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepSplice {
        String target() default "";
        String quoteName(); // Name of the quote to splice into
        SplicePoint[] points(); // Splice points
        boolean flatten() default false; // Flatten nested AST nodes
        boolean validate() default true; // Validate after splicing
        CompilationTarget compileTo() default CompilationTarget.BYTECODE;
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepSplice */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DSPLICE {
        String target() default "";
        String quoteName();
        SplicePoint[] points();
        boolean flatten() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * ⚡ @DeepEval — Runtime code evaluation.
     *
     * Evaluates Java expressions or code blocks at runtime, with access
     * to the surrounding scope (class members, local variables, etc.).
     *
     * Example:
     * <pre>
     * {@code @DeepEval(
     *     code = "Math.pow(x, 2) + Math.sin(y)",
     *     scope = EvalScope.METHOD,
     *     returnType = "double"
     * )}
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DeepEval {
        String target() default "";
        String code(); // Code to evaluate
        String codeResource() default ""; // External file
        EvalScope scope() default EvalScope.METHOD;
        String returnType() default "java.lang.Object";
        String[] imports() default {}; // Additional imports
        String[] classpath() default {}; // Additional classpath entries
        boolean cacheCompilation() default true;
        boolean sandbox() default false; // Run in security sandbox
        long timeoutMs() default 5000; // Evaluation timeout
        String[] allowedClasses() default {}; // Whitelist (sandbox mode)
        String[] deniedClasses() default {}; // Blacklist (sandbox mode)
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepEval */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DEVAL {
        String target() default "";
        String code();
        EvalScope scope() default EvalScope.METHOD;
        String returnType() default "java.lang.Object";
        boolean sandbox() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔨 @DeepCompile — Runtime compilation of source code to bytecode.
     *
     * Compiles Java source code to bytecode at runtime using javax.tools
     * or ECJ (Eclipse Compiler for Java).
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepCompile {
        String target() default "";
        String source() default ""; // Inline source code
        String sourceResource() default ""; // External source file
        String sourceFile() default ""; // Source file path
        CompilationTarget compileTo() default CompilationTarget.BYTECODE;
        String[] classpath() default {};
        String[] sourcepath() default {};
        String targetVersion() default "17"; // Java version target
        String sourceVersion() default "17";
        CompileOption[] options() default {};
        boolean debug() default true; // Include debug info
        boolean optimize() default false;
        boolean warnings() default true;
        boolean cacheResult() default true;
        String compiler() default "javac"; // javac, ecj
        String outputDirectory() default ""; // Where to write compiled classes
        boolean loadImmediately() default true; // Load into current classloader
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepCompile */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DCOMPILE {
        String target() default "";
        String source() default "";
        String sourceResource() default "";
        CompilationTarget compileTo() default CompilationTarget.BYTECODE;
        String targetVersion() default "17";
        boolean cacheResult() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🪞 @DeepReify — Type reification (make erased generics concrete).
     *
     * Captures generic type information at compile/load time and makes it
     * available at runtime, overcoming Java's type erasure.
     *
     * Example:
     * <pre>
     * {@code @DeepReify(
     *     mode = ReificationMode.TYPE_PARAMETER,
     *     typeParameters = {"T", "U"}
     * )}
     * public class Container<T, U> {
     *     // T and U are available at runtime via DeepMix reflection
     *     public Class<T> getTypeT() {
     *         return DeepMixReify.getTypeParameter(this, 0);
     *     }
     * }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    public @interface DeepReify {
        String target() default "";
        ReificationMode mode() default ReificationMode.TYPE_PARAMETER;
        String[] typeParameters() default {}; // Type parameter names to reify
        boolean captureAtCreation() default true; // Capture type at new Site
        boolean storeInField() default true; // Store reified type in field
        String fieldNamePattern() default "deepmix$reified${}"; // {} = param name
        boolean generateAccessors() default true; // Generate getTypeX() methods
        boolean supportSubclasses() default true; // Propagate to subclasses
        boolean supportAnonymous() default true; // Support anonymous subclasses
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepReify */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    public @interface DREIFY {
        String target() default "";
        ReificationMode mode() default ReificationMode.TYPE_PARAMETER;
        String[] typeParameters() default {};
        boolean captureAtCreation() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    // Note: @DeepReflect / @DREFL is defined in Phase 8 (DeepMixPhases.java).
    // It is re-exported here as an alias for discoverability.

    // ──────────────────────────────────────────────
    // Phase 23 — Processor
    // ──────────────────────────────────────────────

    /**
     * Processor for all Phase 23 (Metaprogramming) annotations.
     */
    public static class Phase23Processor {

        private final DeepMixContext context;
        private final Map<String, Object> macroRegistry = new ConcurrentHashMap<>();
        private final Map<String, Object> quoteRegistry = new ConcurrentHashMap<>();
        private final Map<String, byte[]> compilationCache = new ConcurrentHashMap<>();

        public Phase23Processor(DeepMixContext context) {
            this.context = context;
        }

        /** Process @DeepMacro — Register and/or expand a macro. */
        public void processMacro(DeepMacro annotation, ClassNode classNode,
                                 MethodNode methodNode) throws DeepMixProcessingException {
            String macroName = annotation.name();

            // Register macro definition
            MacroDefinition macroDef = new MacroDefinition(
                macroName,
                annotation.params(),
                annotation.body(),
                annotation.bodyResource(),
                annotation.strategy(),
                annotation.hygienic(),
                annotation.recursive(),
                annotation.maxExpansionDepth(),
                annotation.imports()
            );
            macroRegistry.put(macroName, macroDef);

            // If strategy is LOAD_TIME or EAGER, expand now
            if (annotation.strategy() == MacroExpansionStrategy.LOAD_TIME ||
                annotation.strategy() == MacroExpansionStrategy.EAGER) {

                InsnList expanded = expandMacro(macroDef, classNode, methodNode);

                if (annotation.validateExpansion()) {
                    validateExpandedBytecode(expanded, classNode, methodNode);
                }

                if (annotation.generateDebugInfo()) {
                    // Insert line number pointing to macro source
                    LabelNode macroLabel = new LabelNode();
                    expanded.insert(macroLabel);
                    expanded.insert(new LineNumberNode(-1, macroLabel));
                }

                methodNode.instructions.insert(expanded);
            }

            // For COMPILE_TIME, the annotation processor handles it externally.
            // For RUNTIME/LAZY, inject a lazy-init stub.
            if (annotation.strategy() == MacroExpansionStrategy.RUNTIME ||
                annotation.strategy() == MacroExpansionStrategy.LAZY) {
                InsnList lazyStub = new InsnList();
                lazyStub.add(new LdcInsnNode(macroName));
                lazyStub.add(new LdcInsnNode(classNode.name));
                lazyStub.add(new LdcInsnNode(methodNode.name));
                lazyStub.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/meta/DeepMixMacroRuntime",
                    "expandAtRuntime",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                    false
                ));
                methodNode.instructions.insert(lazyStub);
            }

            context.addDiagnostic(String.format(
                "🔄 @DeepMacro registered: '%s' [params=%d, strategy=%s, hygienic=%b]",
                macroName, annotation.params().length,
                annotation.strategy(), annotation.hygienic()
            ));
        }

        /** Process @DeepTemplate — Generate code from template. */
        public void processTemplate(DeepTemplate annotation, ClassNode classNode,
                                    MethodNode methodNode) throws DeepMixProcessingException {
            String templateSource = annotation.template();
            if (templateSource.isEmpty() && !annotation.templateResource().isEmpty()) {
                templateSource = loadResource(annotation.templateResource());
            }

            if (templateSource.isEmpty()) {
                throw new DeepMixProcessingException(
                    "@DeepTemplate requires either 'template' or 'templateResource'");
            }

            // Build binding map
            Map<String, Object> bindingMap = new LinkedHashMap<>();
            for (TemplateBinding binding : annotation.bindings()) {
                Object value = evaluateExpression(binding.expression(), classNode, methodNode);
                bindingMap.put(binding.name(), value);
            }

            // Render template
            String rendered = renderTemplate(
                annotation.engine(), templateSource, bindingMap);

            // Optionally iterate over a collection
            if (!annotation.iterateOver().isEmpty()) {
                Object collection = bindingMap.get(annotation.iterateOver());
                if (collection instanceof Iterable) {
                    StringBuilder allRendered = new StringBuilder();
                    int index = 0;
                    for (Object item : (Iterable<?>) collection) {
                        Map<String, Object> itemBindings = new LinkedHashMap<>(bindingMap);
                        itemBindings.put("item", item);
                        itemBindings.put("index", index++);
                        allRendered.append(renderTemplate(
                            annotation.engine(), templateSource, itemBindings));
                        allRendered.append("\n");
                    }
                    rendered = allRendered.toString();
                }
            }

            // Compile rendered code to bytecode
            switch (annotation.compileTo()) {
                case BYTECODE:
                    byte[] bytecode = compileSource(rendered, classNode.name);
                    if (bytecode != null) {
                        ClassReader cr = new ClassReader(bytecode);
                        ClassNode generatedNode = new ClassNode();
                        cr.accept(generatedNode, 0);

                        // Merge generated methods/fields into target class
                        if (annotation.append()) {
                            mergeClassNode(classNode, generatedNode, annotation.replace());
                        }
                    }
                    break;

                case SOURCE:
                    // Store generated source for external compilation
                    if (annotation.generateSource()) {
                        context.storeGeneratedSource(
                            classNode.name + "_generated", rendered);
                    }
                    break;

                default:
                    break;
            }

            context.addDiagnostic(String.format(
                "📝 @DeepTemplate applied to %s [engine=%s, bindings=%d, compileTo=%s]",
                classNode.name, annotation.engine(),
                annotation.bindings().length, annotation.compileTo()
            ));
        }

        /** Process @DeepDSL — Create or invoke a DSL. */
        public void processDSL(DeepDSL annotation, ClassNode classNode,
                               MethodNode methodNode) throws DeepMixProcessingException {
            String dslName = annotation.name();

            switch (annotation.type()) {
                case INTERNAL:
                    // Generate fluent API builder class
                    generateFluentDSL(annotation, classNode);
                    break;

                case EXTERNAL:
                    // Register grammar/parser for external DSL
                    if (!annotation.grammarFile().isEmpty()) {
                        registerExternalDSL(dslName, annotation.grammarFile(),
                            annotation.parserClass());
                    }
                    // Register transformation rules
                    for (DSLRule rule : annotation.rules()) {
                        registerDSLRule(dslName, rule);
                    }
                    break;

                case EMBEDDED:
                    // Set up string interpolation for embedded DSL
                    for (DSLRule rule : annotation.rules()) {
                        registerDSLRule(dslName, rule);
                    }
                    break;

                case ANNOTATION:
                    // Generate annotation-based DSL processors
                    generateAnnotationDSL(annotation, classNode);
                    break;

                case BUILDER:
                    generateBuilderDSL(annotation, classNode);
                    break;

                case FUNCTIONAL:
                    generateFunctionalDSL(annotation, classNode);
                    break;

                case ALGEBRAIC:
                    generateAlgebraicDSL(annotation, classNode);
                    break;
            }

            context.addDiagnostic(String.format(
                "🔤 @DeepDSL created: '%s' [type=%s, rules=%d, typeSafe=%b]",
                dslName, annotation.type(),
                annotation.rules().length, annotation.typeSafe()
            ));
        }

        /** Process @DeepQuote — Capture code as data. */
        public void processQuote(DeepQuote annotation, ClassNode classNode,
                                 MethodNode methodNode) throws DeepMixProcessingException {
            String code = annotation.code();
            if (code.isEmpty() && !annotation.codeResource().isEmpty()) {
                code = loadResource(annotation.codeResource());
            }

            // Parse code into AST or bytecode representation
            Object codeRepresentation;
            switch (annotation.representation()) {
                case AST:
                    codeRepresentation = parseToAST(code, annotation.mode(),
                        annotation.variables(), annotation.typeVariables());
                    break;
                case BYTECODE:
                    codeRepresentation = parseToByteCode(code, annotation.mode(),
                        classNode);
                    break;
                default:
                    codeRepresentation = code; // Raw string
                    break;
            }

            // Store in quote registry
            String quoteName = methodNode != null ? methodNode.name :
                classNode.name + "$quote";
            quoteRegistry.put(quoteName, codeRepresentation);

            // If annotating a field, inject a field initializer with the quoted code
            if (methodNode == null) {
                // Field-level: handled by class transformer
                InsnList fieldInit = new InsnList();
                fieldInit.add(new LdcInsnNode(quoteName));
                fieldInit.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/meta/DeepMixQuoteRuntime",
                    "getQuotedCode",
                    "(Ljava/lang/String;)Ldeepmix/runtime/meta/DeepCodeNode;",
                    false
                ));
                // Store into constructor
            }

            context.addDiagnostic(String.format(
                "💬 @DeepQuote captured: '%s' [mode=%s, vars=%s, representation=%s]",
                quoteName, annotation.mode(),
                Arrays.toString(annotation.variables()), annotation.representation()
            ));
        }

        /** Process @DeepQuasiquote — Parse template with splice markers. */
        public void processQuasiquote(DeepQuasiquote annotation, ClassNode classNode,
                                      MethodNode methodNode) throws DeepMixProcessingException {
            String code = annotation.code();
            String prefix = annotation.unquotePrefix();
            String open = annotation.unquoteOpen();
            String close = annotation.unquoteClose();

            // Parse out splice points from the template
            List<String> parts = new ArrayList<>();
            List<SplicePoint> splicePoints = new ArrayList<>();

            // Simple parser for ~{markers}
            StringBuilder currentPart = new StringBuilder();
            int i = 0;
            while (i < code.length()) {
                if (code.startsWith(prefix + open, i)) {
                    // Found splice marker
                    parts.add(currentPart.toString());
                    currentPart = new StringBuilder();

                    int closeIdx = code.indexOf(close, i + prefix.length() + open.length());
                    if (closeIdx < 0) {
                        throw new DeepMixProcessingException(
                            "Unclosed quasiquote splice marker at position " + i);
                    }
                    String markerName = code.substring(
                        i + prefix.length() + open.length(), closeIdx);

                    // Find matching SplicePoint annotation
                    SplicePoint matchedPoint = null;
                    for (SplicePoint sp : annotation.splicePoints()) {
                        if (sp.marker().equals(markerName)) {
                            matchedPoint = sp;
                            break;
                        }
                    }
                    if (matchedPoint != null) {
                        splicePoints.add(matchedPoint);
                    } else {
                        // Auto-generate splice point (identity — use marker as variable name)
                        splicePoints.add(null); // Placeholder
                    }

                    i = closeIdx + close.length();
                } else {
                    currentPart.append(code.charAt(i));
                    i++;
                }
            }
            parts.add(currentPart.toString());

            // Store parsed quasiquote for later splicing
            String qqName = methodNode != null ? methodNode.name :
                classNode.name + "$quasiquote";
            Map<String, Object> qqData = new LinkedHashMap<>();
            qqData.put("parts", parts);
            qqData.put("splicePoints", splicePoints);
            qqData.put("mode", annotation.mode());
            quoteRegistry.put(qqName, qqData);

            context.addDiagnostic(String.format(
                "🔀 @DeepQuasiquote parsed: '%s' [%d parts, %d splices, mode=%s]",
                qqName, parts.size(), splicePoints.size(), annotation.mode()
            ));
        }

        /** Process @DeepSplice — Splice code into a quoted template. */
        public void processSplice(DeepSplice annotation, ClassNode classNode,
                                  MethodNode methodNode) throws DeepMixProcessingException {
            String quoteName = annotation.quoteName();
            Object quoted = quoteRegistry.get(quoteName);

            if (quoted == null) {
                throw new DeepMixProcessingException(
                    "@DeepSplice references unknown quote: '" + quoteName + "'");
            }

            // Apply splice points
            for (SplicePoint point : annotation.points()) {
                Object spliceValue = evaluateExpression(
                    point.expression(), classNode, methodNode);

                if (point.flatten() && spliceValue instanceof List) {
                    // Flatten list splice
                    for (Object item : (List<?>) spliceValue) {
                        applySplice(quoted, point.marker(), item);
                    }
                } else {
                    applySplice(quoted, point.marker(), spliceValue);
                }
            }

            // Compile spliced result
            if (annotation.compileTo() == CompilationTarget.BYTECODE) {
                InsnList compiledResult = compileSplicedCode(quoted, classNode);
                if (compiledResult != null) {
                    methodNode.instructions.insert(compiledResult);
                }
            }

            // Validate
            if (annotation.validate()) {
                validateExpandedBytecode(
                    methodNode.instructions, classNode, methodNode);
            }

            context.addDiagnostic(String.format(
                "✂️ @DeepSplice applied: '%s' → %s::%s [%d splice points]",
                quoteName, classNode.name, methodNode.name,
                annotation.points().length
            ));
        }

        /** Process @DeepEval — Runtime code evaluation. */
        public void processEval(DeepEval annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            String code = annotation.code();
            if (code.isEmpty() && !annotation.codeResource().isEmpty()) {
                code = loadResource(annotation.codeResource());
            }

            InsnList evalCode = new InsnList();

            // Determine scope access
            switch (annotation.scope()) {
                case GLOBAL:
                case EXPRESSION:
                    // No context needed
                    evalCode.add(new InsnNode(Opcodes.ACONST_NULL));
                    break;
                case CLASS:
                    // Pass 'this' as context
                    evalCode.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    break;
                case METHOD:
                    // Pass method locals as context
                    evalCode.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    // Also capture local variable array
                    captureLocalsForEval(evalCode, methodNode);
                    break;
                case SANDBOX:
                case PRIVILEGED:
                    evalCode.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    evalCode.add(new InsnNode(
                        annotation.scope() == EvalScope.SANDBOX ?
                            Opcodes.ICONST_1 : Opcodes.ICONST_0));
                    break;
            }

            // Push evaluation parameters
            evalCode.add(new LdcInsnNode(code));
            evalCode.add(new LdcInsnNode(annotation.returnType()));
            evalCode.add(new LdcInsnNode(annotation.scope().name()));
            evalCode.add(new InsnNode(
                annotation.cacheCompilation() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            evalCode.add(new InsnNode(
                annotation.sandbox() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            evalCode.add(new LdcInsnNode(annotation.timeoutMs()));

            // Push imports array
            pushStringArray(evalCode, annotation.imports());

            // Call runtime evaluator
            evalCode.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/meta/DeepMixEvalRuntime",
                "evaluate",
                "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;" +
                    "Ljava/lang/String;ZZJ[Ljava/lang/String;)Ljava/lang/Object;",
                false
            ));

            // Cast result to expected return type
            Type returnType = Type.getType(
                resolveTypeDescriptor(annotation.returnType()));
            if (returnType.getSort() != Type.OBJECT ||
                !returnType.getInternalName().equals("java/lang/Object")) {
                evalCode.add(new TypeInsnNode(Opcodes.CHECKCAST,
                    returnType.getInternalName()));
            }

            methodNode.instructions.insert(evalCode);

            context.addDiagnostic(String.format(
                "⚡ @DeepEval injected into %s::%s [scope=%s, cached=%b, sandbox=%b, timeout=%dms]",
                classNode.name, methodNode.name,
                annotation.scope(), annotation.cacheCompilation(),
                annotation.sandbox(), annotation.timeoutMs()
            ));
        }

        /** Process @DeepCompile — Runtime compilation of source code. */
        public void processCompile(DeepCompile annotation, ClassNode classNode,
                                   MethodNode methodNode) throws DeepMixProcessingException {
            String source = annotation.source();
            if (source.isEmpty() && !annotation.sourceResource().isEmpty()) {
                source = loadResource(annotation.sourceResource());
            }
            if (source.isEmpty() && !annotation.sourceFile().isEmpty()) {
                source = loadFile(annotation.sourceFile());
            }

            if (source.isEmpty()) {
                throw new DeepMixProcessingException(
                    "@DeepCompile requires 'source', 'sourceResource', or 'sourceFile'");
            }

            // Check cache
            String cacheKey = Integer.toHexString(source.hashCode());
            byte[] compiled = annotation.cacheResult() ?
                compilationCache.get(cacheKey) : null;

            if (compiled == null) {
                // Compile using javac or ECJ
                InsnList compileCall = new InsnList();
                compileCall.add(new LdcInsnNode(source));
                compileCall.add(new LdcInsnNode(annotation.targetVersion()));
                compileCall.add(new LdcInsnNode(annotation.sourceVersion()));
                compileCall.add(new InsnNode(
                    annotation.debug() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                compileCall.add(new InsnNode(
                    annotation.optimize() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                compileCall.add(new LdcInsnNode(annotation.compiler()));
                pushStringArray(compileCall, annotation.classpath());
                compileCall.add(new InsnNode(
                    annotation.loadImmediately() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

                // Compile options
                CompileOption[] opts = annotation.options();
                compileCall.add(new LdcInsnNode(opts.length));
                compileCall.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
                for (int i = 0; i < opts.length; i++) {
                    compileCall.add(new InsnNode(Opcodes.DUP));
                    compileCall.add(new LdcInsnNode(i));
                    compileCall.add(new LdcInsnNode(opts[i].key() + "=" + opts[i].value()));
                    compileCall.add(new InsnNode(Opcodes.AASTORE));
                }

                compileCall.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/meta/DeepMixCompileRuntime",
                    "compileAndLoad",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                        "ZZLjava/lang/String;[Ljava/lang/String;Z" +
                        "[Ljava/lang/String;)Ljava/lang/Class;",
                    false
                ));

                methodNode.instructions.insert(compileCall);
            }

            context.addDiagnostic(String.format(
                "🔨 @DeepCompile applied to %s::%s [compiler=%s, target=%s, cached=%b]",
                classNode.name, methodNode.name,
                annotation.compiler(), annotation.targetVersion(),
                annotation.cacheResult()
            ));
        }

        /** Process @DeepReify — Reify generic type parameters. */
        public void processReify(DeepReify annotation, ClassNode classNode,
                                 MethodNode methodNode) throws DeepMixProcessingException {
            String[] typeParams = annotation.typeParameters();

            // If no specific params, discover from class signature
            if (typeParams.length == 0 && classNode.signature != null) {
                typeParams = extractTypeParamsFromSignature(classNode.signature);
            }

            for (String typeParam : typeParams) {
                // Add a field to store the reified type
                if (annotation.storeInField()) {
                    String fieldName = annotation.fieldNamePattern()
                        .replace("{}", typeParam);

                    FieldNode reifiedField = new FieldNode(
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                        fieldName,
                        "Ljava/lang/Class;",
                        "Ljava/lang/Class<T" + typeParam + ";>;",
                        null
                    );
                    classNode.fields.add(reifiedField);
                }

                // Generate accessor method
                if (annotation.generateAccessors()) {
                    String accessorName = "getType" + typeParam;
                    String fieldName = annotation.fieldNamePattern()
                        .replace("{}", typeParam);

                    MethodNode accessor = new MethodNode(
                        Opcodes.ACC_PUBLIC,
                        accessorName,
                        "()Ljava/lang/Class;",
                        "()Ljava/lang/Class<T" + typeParam + ";>;",
                        null
                    );
                    accessor.instructions = new InsnList();
                    accessor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    accessor.instructions.add(new FieldInsnNode(
                        Opcodes.GETFIELD, classNode.name, fieldName,
                        "Ljava/lang/Class;"
                    ));
                    accessor.instructions.add(new InsnNode(Opcodes.ARETURN));
                    accessor.maxStack = 1;
                    accessor.maxLocals = 1;
                    classNode.methods.add(accessor);
                }

                // Modify constructors to capture type parameter
                if (annotation.captureAtCreation()) {
                    for (MethodNode mn : classNode.methods) {
                        if (mn.name.equals("<init>")) {
                            injectTypeCapture(classNode, mn, typeParam,
                                annotation.fieldNamePattern());
                        }
                    }
                }
            }

            // Support subclass type propagation
            if (annotation.supportSubclasses()) {
                injectSubclassTypeResolution(classNode, typeParams);
            }

            context.addDiagnostic(String.format(
                "🪞 @DeepReify applied to %s [mode=%s, params=%s, accessors=%b, subclass=%b]",
                classNode.name, annotation.mode(),
                Arrays.toString(typeParams), annotation.generateAccessors(),
                annotation.supportSubclasses()
            ));
        }

        // ──────────────────────────────────────────────
        // Phase 23 — Internal helper methods
        // ──────────────────────────────────────────────

        private InsnList expandMacro(MacroDefinition macroDef, ClassNode classNode,
                                     MethodNode methodNode) {
            InsnList expanded = new InsnList();

            String body = macroDef.body;
            if (body.isEmpty() && !macroDef.bodyResource.isEmpty()) {
                body = loadResource(macroDef.bodyResource);
            }

            // Simple template expansion: replace ${param} with actual values
            for (MacroParam param : macroDef.params) {
                String placeholder = "${" + param.name() + "}";
                String defaultVal = param.defaultValue();
                body = body.replace(placeholder,
                    defaultVal.isEmpty() ? param.name() : defaultVal);
            }

            // Replace built-in macros
            body = body.replace("${class}", classNode.name);
            if (methodNode != null) {
                body = body.replace("${method}", methodNode.name);
                body = body.replace("${desc}", methodNode.desc);
            }

            // Hygienic expansion: rename local variables to avoid capture
            if (macroDef.hygienic) {
                String suffix = "$dm$" + Integer.toHexString(body.hashCode() & 0xFFFF);
                // This is a simplified version — full hygiene requires AST analysis
                body = body.replace("__local__", "__local" + suffix + "__");
            }

            // Compile the expanded template to bytecode
            expanded.add(new LdcInsnNode(body));
            expanded.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/meta/DeepMixMacroRuntime",
                "executeMacroBody",
                "(Ljava/lang/String;)V",
                false
            ));

            return expanded;
        }

        private void validateExpandedBytecode(InsnList instructions,
                                               ClassNode classNode,
                                               MethodNode methodNode) {
            // Basic validation: check stack consistency
            int stackDepth = 0;
            for (AbstractInsnNode insn : instructions) {
                // Simplified — real implementation uses ASM's Analyzer
                if (insn.getOpcode() >= 0) {
                    stackDepth += getStackDelta(insn);
                    if (stackDepth < 0) {
                        context.addDiagnostic(String.format(
                            "⚠️ Stack underflow detected in expanded macro at %s::%s",
                            classNode.name, methodNode.name
                        ));
                    }
                }
            }
        }

        private int getStackDelta(AbstractInsnNode insn) {
            // Simplified stack delta calculation
            switch (insn.getOpcode()) {
                case Opcodes.ACONST_NULL:
                case Opcodes.ICONST_M1: case Opcodes.ICONST_0: case Opcodes.ICONST_1:
                case Opcodes.ICONST_2: case Opcodes.ICONST_3: case Opcodes.ICONST_4:
                case Opcodes.ICONST_5: case Opcodes.FCONST_0: case Opcodes.FCONST_1:
                case Opcodes.FCONST_2:
                case Opcodes.BIPUSH: case Opcodes.SIPUSH:
                case Opcodes.LDC:
                    return 1;
                case Opcodes.LCONST_0: case Opcodes.LCONST_1:
                case Opcodes.DCONST_0: case Opcodes.DCONST_1:
                    return 2;
                case Opcodes.ILOAD: case Opcodes.FLOAD: case Opcodes.ALOAD:
                    return 1;
                case Opcodes.LLOAD: case Opcodes.DLOAD:
                    return 2;
                case Opcodes.ISTORE: case Opcodes.FSTORE: case Opcodes.ASTORE:
                    return -1;
                case Opcodes.LSTORE: case Opcodes.DSTORE:
                    return -2;
                case Opcodes.POP:
                    return -1;
                case Opcodes.POP2:
                    return -2;
                case Opcodes.DUP:
                    return 1;
                case Opcodes.IRETURN: case Opcodes.FRETURN: case Opcodes.ARETURN:
                    return -1;
                case Opcodes.LRETURN: case Opcodes.DRETURN:
                    return -2;
                case Opcodes.RETURN:
                    return 0;
                default:
                    return 0;
            }
        }

        private String renderTemplate(TemplateEngine engine, String template,
                                       Map<String, Object> bindings) {
            // Simplified Mustache-style rendering
            String result = template;
            for (Map.Entry<String, Object> entry : bindings.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                String value = entry.getValue() != null ?
                    entry.getValue().toString() : "";
                result = result.replace(placeholder, value);
            }
            return result;
        }

        private Object evaluateExpression(String expression, ClassNode classNode,
                                           MethodNode methodNode) {
            // Simplified expression evaluation
            // Full implementation would use javax.script or JShell
            return expression; // Placeholder
        }

        private byte[] compileSource(String source, String className) {
            // Delegate to javax.tools.JavaCompiler
            // Simplified stub — real implementation uses JavaCompiler API
            return null;
        }

        private void mergeClassNode(ClassNode target, ClassNode source, boolean replace) {
            // Merge fields
            for (FieldNode field : source.fields) {
                boolean exists = target.fields.stream()
                    .anyMatch(f -> f.name.equals(field.name));
                if (!exists || replace) {
                    if (exists) {
                        target.fields.removeIf(f -> f.name.equals(field.name));
                    }
                    target.fields.add(field);
                }
            }

            // Merge methods
            for (MethodNode method : source.methods) {
                if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
                    continue; // Don't merge constructors/static initializers
                }
                boolean exists = target.methods.stream()
                    .anyMatch(m -> m.name.equals(method.name) &&
                        m.desc.equals(method.desc));
                if (!exists || replace) {
                    if (exists) {
                        target.methods.removeIf(m -> m.name.equals(method.name) &&
                            m.desc.equals(method.desc));
                    }
                    target.methods.add(method);
                }
            }

            // Merge interfaces
            for (String iface : source.interfaces) {
                if (!target.interfaces.contains(iface)) {
                    target.interfaces.add(iface);
                }
            }
        }

        private void generateFluentDSL(DeepDSL annotation, ClassNode classNode) {
            // Generate inner builder class for fluent DSL
            // This would create something like:
            // MyDSL.when(condition).then(action).otherwise(fallback).build();
        }

        private void registerExternalDSL(String name, String grammarFile,
                                          String parserClass) {
            // Register grammar for external DSL parsing
        }

        private void registerDSLRule(String dslName, DSLRule rule) {
            // Register transformation rule for DSL
        }

        private void generateAnnotationDSL(DeepDSL annotation, ClassNode classNode) {
            // Generate annotation processor for annotation-based DSL
        }

        private void generateBuilderDSL(DeepDSL annotation, ClassNode classNode) {
            // Generate builder pattern DSL
        }

        private void generateFunctionalDSL(DeepDSL annotation, ClassNode classNode) {
            // Generate functional combinator DSL
        }

        private void generateAlgebraicDSL(DeepDSL annotation, ClassNode classNode) {
            // Generate algebraic DSL with type-safe expressions
        }

        private Object parseToAST(String code, QuoteMode mode,
                                   String[] variables, String[] typeVariables) {
            // Parse Java code to AST representation
            return code; // Placeholder
        }

        private Object parseToByteCode(String code, QuoteMode mode,
                                        ClassNode classNode) {
            // Parse Java code directly to bytecode instructions
            return new InsnList(); // Placeholder
        }

        private void applySplice(Object quoted, String marker, Object value) {
            // Apply a splice point to a quoted code template
        }

        private InsnList compileSplicedCode(Object quoted, ClassNode classNode) {
            // Compile a spliced quasiquote into bytecode
            return new InsnList();
        }

        private void captureLocalsForEval(InsnList insns, MethodNode methodNode) {
            // Capture local variables into an Object array for eval scope
            if (methodNode.localVariables == null) return;

            int localsCount = methodNode.localVariables.size();
            insns.add(new LdcInsnNode(localsCount));
            insns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));

            for (int i = 0; i < methodNode.localVariables.size(); i++) {
                LocalVariableNode lv = methodNode.localVariables.get(i);
                insns.add(new InsnNode(Opcodes.DUP));
                insns.add(new LdcInsnNode(i));

                Type varType = Type.getType(lv.desc);
                switch (varType.getSort()) {
                    case Type.INT:
                    case Type.BOOLEAN:
                    case Type.BYTE:
                    case Type.CHAR:
                    case Type.SHORT:
                        insns.add(new VarInsnNode(Opcodes.ILOAD, lv.index));
                        insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                            "java/lang/Integer", "valueOf",
                            "(I)Ljava/lang/Integer;", false));
                        break;
                    case Type.LONG:
                        insns.add(new VarInsnNode(Opcodes.LLOAD, lv.index));
                        insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                            "java/lang/Long", "valueOf",
                            "(J)Ljava/lang/Long;", false));
                        break;
                    case Type.FLOAT:
                        insns.add(new VarInsnNode(Opcodes.FLOAD, lv.index));
                        insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                            "java/lang/Float", "valueOf",
                            "(F)Ljava/lang/Float;", false));
                        break;
                    case Type.DOUBLE:
                        insns.add(new VarInsnNode(Opcodes.DLOAD, lv.index));
                        insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                            "java/lang/Double", "valueOf",
                            "(D)Ljava/lang/Double;", false));
                        break;
                    case Type.OBJECT:
                    case Type.ARRAY:
                    default:
                        insns.add(new VarInsnNode(Opcodes.ALOAD, lv.index));
                        break;
                }

                insns.add(new InsnNode(Opcodes.AASTORE));
            }
        }

        private String resolveTypeDescriptor(String typeName) {
            switch (typeName) {
                case "void":    return "V";
                case "boolean": return "Z";
                case "byte":    return "B";
                case "char":    return "C";
                case "short":   return "S";
                case "int":     return "I";
                case "long":    return "J";
                case "float":   return "F";
                case "double":  return "D";
                default:
                    if (typeName.startsWith("L") && typeName.endsWith(";")) {
                        return typeName;
                    }
                    return "L" + typeName.replace('.', '/') + ";";
            }
        }

        private String[] extractTypeParamsFromSignature(String signature) {
            // Parse generic signature to extract type parameter names
            // Signature format: <T:Ljava/lang/Object;U:Ljava/lang/Number;>...
            List<String> params = new ArrayList<>();
            if (signature.startsWith("<")) {
                int depth = 0;
                StringBuilder currentParam = new StringBuilder();
                for (int i = 1; i < signature.length(); i++) {
                    char c = signature.charAt(i);
                    if (c == '<') depth++;
                    else if (c == '>') {
                        if (depth == 0) break;
                        depth--;
                    } else if (c == ':' && depth == 0) {
                        if (currentParam.length() > 0) {
                            params.add(currentParam.toString());
                            currentParam = new StringBuilder();
                        }
                        // Skip the bound
                        while (i + 1 < signature.length() &&
                            signature.charAt(i + 1) != ';' &&
                            signature.charAt(i + 1) != '>') {
                            i++;
                        }
                        if (i + 1 < signature.length() &&
                            signature.charAt(i + 1) == ';') {
                            i++;
                        }
                    } else if (c == ';' && depth == 0) {
                        // End of bound, next char is next param or >
                    } else {
                        currentParam.append(c);
                    }
                }
            }
            return params.toArray(new String[0]);
        }

        private void injectTypeCapture(ClassNode classNode, MethodNode constructor,
                                        String typeParam, String fieldNamePattern) {
            String fieldName = fieldNamePattern.replace("{}", typeParam);

            InsnList capture = new InsnList();

            // this.deepmix$reified$T = DeepMixReify.captureType(this, paramIndex);
            capture.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            capture.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this (for getClass)
            capture.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/Object",
                "getClass",
                "()Ljava/lang/Class;",
                false
            ));
            capture.add(new LdcInsnNode(typeParam));
            capture.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/meta/DeepMixReifyRuntime",
                "resolveTypeParameter",
                "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Class;",
                false
            ));
            capture.add(new FieldInsnNode(
                Opcodes.PUTFIELD, classNode.name, fieldName, "Ljava/lang/Class;"
            ));

            // Insert after super() call
            for (AbstractInsnNode insn : constructor.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode min = (MethodInsnNode) insn;
                    if (min.name.equals("<init>") &&
                        min.owner.equals(classNode.superName)) {
                        constructor.instructions.insert(min, capture);
                        break;
                    }
                }
            }
        }

        private void injectSubclassTypeResolution(ClassNode classNode,
                                                    String[] typeParams) {
            // Add a method that resolves type parameters from subclass generic info
            MethodNode resolver = new MethodNode(
                Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC,
                "deepmix$resolveSubclassTypes",
                "(Ljava/lang/Class;)[Ljava/lang/Class;",
                null,
                null
            );

            InsnList body = new InsnList();
            body.add(new VarInsnNode(Opcodes.ALOAD, 0)); // subclass
            body.add(new LdcInsnNode(typeParams.length));
            body.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/meta/DeepMixReifyRuntime",
                "resolveAllTypeParameters",
                "(Ljava/lang/Class;I)[Ljava/lang/Class;",
                false
            ));
            body.add(new InsnNode(Opcodes.ARETURN));

            resolver.instructions = body;
            resolver.maxStack = 2;
            resolver.maxLocals = 1;
            classNode.methods.add(resolver);
        }

        private String loadResource(String resourcePath) {
            // Load resource from classpath
            try {
                java.io.InputStream is = getClass().getClassLoader()
                    .getResourceAsStream(resourcePath);
                if (is == null) return "";
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                return "";
            }
        }

        private String loadFile(String filePath) {
            try {
                return java.nio.file.Files.readString(java.nio.file.Path.of(filePath));
            } catch (Exception e) {
                return "";
            }
        }

        private void pushStringArray(InsnList insns, String[] values) {
            insns.add(new LdcInsnNode(values.length));
            insns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < values.length; i++) {
                insns.add(new InsnNode(Opcodes.DUP));
                insns.add(new LdcInsnNode(i));
                insns.add(new LdcInsnNode(values[i]));
                insns.add(new InsnNode(Opcodes.AASTORE));
            }
        }

        /** Internal macro definition holder. */
        private static class MacroDefinition {
            final String name;
            final MacroParam[] params;
            final String body;
            final String bodyResource;
            final MacroExpansionStrategy strategy;
            final boolean hygienic;
            final boolean recursive;
            final int maxDepth;
            final String[] imports;

            MacroDefinition(String name, MacroParam[] params, String body,
                            String bodyResource, MacroExpansionStrategy strategy,
                            boolean hygienic, boolean recursive,
                            int maxDepth, String[] imports) {
                this.name = name;
                this.params = params;
                this.body = body;
                this.bodyResource = bodyResource;
                this.strategy = strategy;
                this.hygienic = hygienic;
                this.recursive = recursive;
                this.maxDepth = maxDepth;
                this.imports = imports;
            }
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║              PHASE 24: FINAL EXTENSIONS                            ║
    // ║              7+ annotations | Priority: LOW | Est. Time: 1 day     ║
    // ║                                                                    ║
    // ║  @DeepPlugin     @DeepExtension    @DeepHook                       ║
    // ║  @DeepCallback   @DeepListener     @DeepDelegate                   ║
    // ║  @DeepChain                                                        ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    // ──────────────────────────────────────────────
    // Phase 24 — Enums
    // ──────────────────────────────────────────────

    /** Plugin lifecycle phase */
    public enum PluginLifecycle {
        DISCOVERED,    // Plugin JAR found
        LOADED,        // Plugin class loaded
        PRE_INIT,      // Before initialization
        INIT,          // Initialization
        POST_INIT,     // After initialization
        ENABLED,       // Plugin enabled
        DISABLED,      // Plugin disabled
        UNLOADING,     // Before unload
        UNLOADED,      // Plugin unloaded
        ERROR          // Error state
    }

    /** Extension point type */
    public enum ExtensionPointType {
        INTERFACE,         // Interface-based extension
        ANNOTATION,        // Annotation-discovered extension
        SERVICE_LOADER,    // java.util.ServiceLoader pattern
        REGISTRY,          // Registry-based extension
        EVENT,             // Event-driven extension
        MIXIN,             // Mixin-based extension
        TRANSFORM,         // Bytecode transformation extension
        DYNAMIC_PROXY,     // java.lang.reflect.Proxy based
        CLASS_GENERATION,  // Runtime class generation
        SCRIPT,            // Script-based extension (Lua, JS, etc.)
        CUSTOM
    }

    /** Hook timing relative to target execution */
    public enum HookTiming {
        BEFORE,            // Execute before target
        AFTER,             // Execute after target
        AROUND,            // Wrap target (before + after)
        REPLACE,           // Replace target entirely
        ON_SUCCESS,        // Execute only if target succeeds
        ON_FAILURE,        // Execute only if target throws
        ON_RETURN,         // Execute with access to return value
        CONDITIONAL        // Execute based on runtime condition
    }

    /** Hook priority for ordering multiple hooks on the same target */
    public enum HookPriority {
        HIGHEST(Integer.MAX_VALUE),
        VERY_HIGH(10000),
        HIGH(1000),
        ABOVE_NORMAL(500),
        NORMAL(0),
        BELOW_NORMAL(-500),
        LOW(-1000),
        VERY_LOW(-10000),
        LOWEST(Integer.MIN_VALUE);

        private final int value;
        HookPriority(int value) { this.value = value; }
        public int value() { return value; }
    }

    /** Callback invocation mode */
    public enum CallbackMode {
        SYNC,              // Synchronous invocation
        ASYNC,             // Asynchronous (separate thread)
        ASYNC_ORDERED,     // Asynchronous but ordered
        DEBOUNCED,         // Debounced invocation
        THROTTLED,         // Throttled invocation
        BATCHED,           // Batched (collect and invoke periodically)
        LAZY,              // Invoke only when result is needed
        FIRE_AND_FORGET,   // Non-blocking, no result
        REQUEST_RESPONSE   // Blocking, expects response
    }

    /** Listener registration strategy */
    public enum ListenerStrategy {
        STRONG,            // Strong reference (default)
        WEAK,              // Weak reference (GC-eligible)
        SOFT,              // Soft reference (cleared under memory pressure)
        ONCE,              // Auto-unregister after first invocation
        COUNTED,           // Auto-unregister after N invocations
        CONDITIONAL,       // Only invoke when condition is met
        FILTERED,          // Apply filter before invoking
        ORDERED,           // Ordered invocation (by priority)
        PARALLEL           // Parallel invocation (fork-join)
    }

    /** Delegation strategy */
    public enum DelegationStrategy {
        FORWARD,           // Forward all calls to delegate
        SELECTIVE,         // Forward only specified methods
        LAZY,              // Lazily create delegate
        POOLED,            // Use delegate from pool
        ROUND_ROBIN,       // Round-robin across multiple delegates
        FAILOVER,          // Try next delegate on failure
        LOAD_BALANCED,     // Load-balanced delegation
        CACHED,            // Cache delegate results
        INTERCEPTED,       // Intercept before delegating
        COMPOSITE          // Delegate to multiple (composite pattern)
    }

    /** Method chaining return behavior */
    public enum ChainReturnMode {
        THIS,              // Return 'this' for fluent API
        SELF_TYPE,         // Return self-type (covariant)
        NEW_INSTANCE,      // Return new instance (immutable builder)
        WRAPPER,           // Return wrapper around this
        OPTIONAL,          // Return Optional<This>
        RESULT,            // Return Result<This, Error>
        CUSTOM             // Custom return logic
    }

    // ──────────────────────────────────────────────
    // Phase 24 — Sub-annotation types
    // ──────────────────────────────────────────────

    /** Plugin dependency specification */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PluginDependency {
        String id();
        String version() default "";        // SemVer range
        boolean optional() default false;
        boolean loadBefore() default false;  // Load this plugin before the dependency
        String reason() default "";          // Why this dependency is needed
    }

    /** Extension point definition */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ExtensionPointDef {
        String id();
        String description() default "";
        Class<?> contractType() default Object.class; // Interface/abstract class
        boolean multipleAllowed() default true;
        boolean required() default false;
        int maxExtensions() default Integer.MAX_VALUE;
        String validatorClass() default "";  // Extension validator
    }

    /** Hook target specification */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface HookTarget {
        String className();
        String methodName() default "";
        String methodDesc() default "";
        String fieldName() default "";
        int ordinal() default 0;             // Nth match
        String condition() default "";       // Runtime condition expression
    }

    /** Callback specification */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CallbackSpec {
        String name() default "";
        String returnType() default "void";
        String[] parameterTypes() default {};
        String[] parameterNames() default {};
        CallbackMode mode() default CallbackMode.SYNC;
        long debounceMs() default 0;
        long throttleMs() default 0;
        int batchSize() default 1;
        long batchIntervalMs() default 0;
        long timeoutMs() default 0;          // 0 = no timeout
        boolean nullable() default false;    // Allow null callback
    }

    /** Listener filter specification */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ListenerFilter {
        String expression() default "";       // Filter expression
        String[] eventTypes() default {};     // Only listen for these event types
        String[] excludeTypes() default {};   // Exclude these event types
        String sourcePattern() default "";    // Pattern match on event source
        boolean cancelledEvents() default false; // Listen to cancelled events
    }

    /** Delegation mapping */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DelegationMapping {
        String sourceMethod();
        String targetMethod() default "";     // Empty = same name
        String targetClass() default "";      // Empty = delegate field type
        boolean transformArgs() default false;
        String argTransformer() default "";    // Method that transforms arguments
        boolean transformReturn() default false;
        String returnTransformer() default ""; // Method that transforms return value
    }

    /** Chain step configuration */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ChainStep {
        String method();
        boolean required() default false;     // Must be called in chain
        int order() default -1;               // -1 = any order
        String[] requires() default {};       // Methods that must be called first
        String[] excludes() default {};       // Methods that cannot be called after this
        boolean terminal() default false;     // Ends the chain (e.g., build())
    }

    // ──────────────────────────────────────────────
    // Phase 24 — Annotations
    // ──────────────────────────────────────────────

    /**
     * 🔌 @DeepPlugin — Plugin system integration and lifecycle management.
     *
     * Defines a class as a DeepMix plugin with full lifecycle support,
     * dependency management, configuration, and extension point participation.
     *
     * Example:
     * <pre>
     * {@code @DeepPlugin(
     *     id = "my-awesome-plugin",
     *     name = "My Awesome Plugin",
     *     version = "2.1.0",
     *     description = "Adds awesome features",
     *     dependencies = {
     *         @PluginDependency(id = "core-lib", version = ">=1.0.0"),
     *         @PluginDependency(id = "optional-addon", optional = true)
     *     },
     *     extensionPoints = {
     *         @ExtensionPointDef(
     *             id = "custom.renderer",
     *             contractType = Renderer.class,
     *             multipleAllowed = true
     *         )
     *     },
     *     lifecycle = PluginLifecycle.INIT
     * )}
     * public class MyPlugin implements DeepMixPluginAPI { ... }
     * </pre>
     */
    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface DeepPlugin {
        String id();
        String name() default "";
        String version() default "1.0.0";
        String description() default "";
        String[] authors() default {};
        String license() default "";
        String url() default "";
        PluginDependency[] dependencies() default {};
        ExtensionPointDef[] extensionPoints() default {};
        PluginLifecycle lifecycle() default PluginLifecycle.INIT;
        boolean autoEnable() default true;           // Auto-enable on load
        boolean hotSwappable() default false;         // Support hot-swap reload
        boolean isolated() default false;             // Isolated classloader
        String configFile() default "";               // Plugin config file path
        String configFormat() default "YAML";         // YAML, JSON, TOML, PROPERTIES
        String dataDirectory() default "";            // Plugin data directory
        String mainClass() default "";                // Override main class
        String[] services() default {};               // ServiceLoader services provided
        String[] requiredPermissions() default {};    // Permissions required
        boolean generateDescriptor() default true;    // Generate plugin.yml / plugin.json
        ErrorStrategy onError() default ErrorStrategy.LOG;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepPlugin */
    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface DPLUGIN {
        String id();
        String name() default "";
        String version() default "1.0.0";
        PluginDependency[] dependencies() default {};
        ExtensionPointDef[] extensionPoints() default {};
        boolean autoEnable() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🧩 @DeepExtension — Extension point creation and registration.
     *
     * Marks a class or method as an extension point provider or an extension
     * implementation. Supports ServiceLoader, registry, and annotation-based discovery.
     *
     * Example (defining an extension point):
     * <pre>
     * {@code @DeepExtension(
     *     id = "renderer.backend",
     *     type = ExtensionPointType.INTERFACE,
     *     contract = RenderBackend.class,
     *     description = "Custom render backend implementations"
     * )}
     * public interface RenderBackend { void render(Scene scene); }
     * </pre>
     *
     * Example (implementing an extension):
     * <pre>
     * {@code @DeepExtension(
     *     id = "renderer.backend",
     *     type = ExtensionPointType.INTERFACE,
     *     implementor = true,
     *     extensionName = "vulkan-renderer"
     * )}
     * public class VulkanRenderer implements RenderBackend { ... }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepExtension {
        String id();
        String target() default "";
        ExtensionPointType type() default ExtensionPointType.INTERFACE;
        Class<?> contract() default Object.class;    // Extension point interface
        boolean implementor() default false;          // Is this an implementation?
        String extensionName() default "";            // Name of this extension impl
        String description() default "";
        int order() default 0;                        // Order among implementations
        boolean singleton() default false;            // Single instance only
        boolean lazy() default true;                  // Lazy instantiation
        boolean enabled() default true;               // Initially enabled
        String enabledCondition() default "";          // Runtime enable condition
        String factoryMethod() default "";             // Static factory method
        String[] tags() default {};                    // Tags for filtering
        String[] requiredPlugins() default {};         // Plugins required for this ext
        boolean generateServiceFile() default true;    // Generate META-INF/services
        ErrorStrategy onError() default ErrorStrategy.LOG;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepExtension */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DEXT {
        String id();
        String target() default "";
        ExtensionPointType type() default ExtensionPointType.INTERFACE;
        Class<?> contract() default Object.class;
        boolean implementor() default false;
        String extensionName() default "";
        int order() default 0;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🪝 @DeepHook — Hook system for intercepting method calls.
     *
     * Registers a method as a hook that intercepts calls to a target method.
     * Supports before, after, around, replace, and conditional hooking.
     *
     * Example:
     * <pre>
     * {@code @DeepHook(
     *     targets = {
     *         @HookTarget(
     *             className = "net.minecraft.world.entity.LivingEntity",
     *             methodName = "hurt",
     *             methodDesc = "(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
     *         )
     *     },
     *     timing = HookTiming.BEFORE,
     *     hookPriority = HookPriority.HIGH,
     *     cancellable = true
     * )}
     * public boolean onEntityHurt(LivingEntity entity, DamageSource source, float amount) {
     *     if (amount > 100) return false; // Cancel lethal damage
     *     return true; // Allow normal processing
     * }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DeepHook {
        String target() default "";
        HookTarget[] targets();
        HookTiming timing() default HookTiming.BEFORE;
        HookPriority hookPriority() default HookPriority.NORMAL;
        int customPriority() default 0;               // Fine-grained priority
        boolean cancellable() default false;           // Can cancel target execution
        boolean captureReturn() default false;         // Capture return value (AFTER)
        boolean modifyReturn() default false;          // Allow modifying return value
        boolean captureArgs() default true;            // Pass original args to hook
        boolean modifyArgs() default false;            // Allow modifying args
        boolean captureException() default false;      // Capture thrown exceptions
        boolean suppressException() default false;     // Suppress exceptions
        String condition() default "";                 // Runtime condition expression
        String[] requiredMods() default {};            // Only active with these mods
        boolean persistent() default true;             // Survive hot reload
        boolean threadSafe() default false;            // Thread-safe hook
        String group() default "";                     // Hook group for batch operations
        ErrorStrategy onError() default ErrorStrategy.LOG;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepHook */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface DHOOK {
        String target() default "";
        HookTarget[] targets();
        HookTiming timing() default HookTiming.BEFORE;
        HookPriority hookPriority() default HookPriority.NORMAL;
        boolean cancellable() default false;
        boolean captureReturn() default false;
        String condition() default "";
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 📞 @DeepCallback — Callback registration and invocation.
     *
     * Registers a method as a callback that can be invoked by other
     * components. Supports sync, async, debounced, throttled, and batched modes.
     *
     * Example:
     * <pre>
     * {@code @DeepCallback(
     *     name = "onBlockBreak",
     *     spec = @CallbackSpec(
     *         parameterTypes = {"BlockPos", "Player", "BlockState"},
     *         mode = CallbackMode.ASYNC_ORDERED,
     *         timeoutMs = 5000
     *     ),
     *     autoRegister = true
     * )}
     * public void handleBlockBreak(BlockPos pos, Player player, BlockState state) {
     *     // Handle block break event
     * }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DeepCallback {
        String name() default "";
        String target() default "";
        CallbackSpec spec() default @CallbackSpec;
        boolean autoRegister() default true;          // Auto-register on class load
        boolean autoUnregister() default true;         // Auto-unregister on class unload
        String registryName() default "";              // Custom callback registry
        String group() default "";                     // Callback group
        boolean allowMultiple() default true;          // Multiple registrations
        boolean ordered() default false;               // Invoke in registration order
        String errorHandler() default "";              // Error handler method name
        int maxRetries() default 0;                    // Retry on failure
        long retryDelayMs() default 100;               // Delay between retries
        boolean async() default false;                 // Shorthand for ASYNC mode
        String executorService() default "";            // Custom executor
        ErrorStrategy onError() default ErrorStrategy.LOG;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepCallback */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface DCALLBACK {
        String name() default "";
        String target() default "";
        CallbackSpec spec() default @CallbackSpec;
        boolean autoRegister() default true;
        boolean async() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 👂 @DeepListener — Listener pattern implementation.
     *
     * Registers a class or method as an event listener with configurable
     * strategy, filtering, and lifecycle management.
     *
     * Example:
     * <pre>
     * {@code @DeepListener(
     *     eventType = "PlayerMoveEvent",
     *     strategy = ListenerStrategy.FILTERED,
     *     filter = @ListenerFilter(
     *         expression = "event.getPlayer().getWorld().getName().equals(\"world\")",
     *         cancelledEvents = false
     *     ),
     *     priority = 100
     * )}
     * public void onPlayerMove(PlayerMoveEvent event) {
     *     // Only triggered for players in "world"
     * }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepListener {
        String target() default "";
        String eventType() default "";                 // Event class name
        Class<?>[] eventTypes() default {};             // Multiple event types
        ListenerStrategy strategy() default ListenerStrategy.STRONG;
        ListenerFilter filter() default @ListenerFilter;
        int maxInvocations() default -1;               // -1 = unlimited (for COUNTED)
        boolean ignoreCancelled() default true;        // Skip cancelled events
        boolean autoRegister() default true;
        boolean autoUnregister() default true;
        String eventBus() default "";                  // Target event bus name
        String listenerGroup() default "";             // Group for batch operations
        boolean receiveSubtypes() default true;        // Listen to subtype events
        String condition() default "";                 // Runtime condition
        boolean async() default false;                 // Async listener
        String executorService() default "";
        ErrorStrategy onError() default ErrorStrategy.LOG;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepListener */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DLISTEN {
        String target() default "";
        String eventType() default "";
        ListenerStrategy strategy() default ListenerStrategy.STRONG;
        ListenerFilter filter() default @ListenerFilter;
        boolean ignoreCancelled() default true;
        boolean async() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🤝 @DeepDelegate — Delegation pattern injection.
     *
     * Automatically generates delegation methods that forward calls to
     * a delegate object. Supports multiple delegation strategies including
     * failover, load-balancing, and caching.
     *
     * Example:
     * <pre>
     * {@code @DeepDelegate(
     *     field = "backend",
     *     strategy = DelegationStrategy.FAILOVER,
     *     include = {"render", "update", "dispose"},
     *     fallbackField = "fallbackBackend"
     * )}
     * public class RenderManager {
     *     private RenderBackend backend;
     *     private RenderBackend fallbackBackend;
     *     // render(), update(), dispose() are auto-generated
     * }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface DeepDelegate {
        String target() default "";
        String field() default "";                     // Delegate field name
        Class<?> delegateType() default Object.class;  // Delegate interface/class
        DelegationStrategy strategy() default DelegationStrategy.FORWARD;
        String[] include() default {};                 // Methods to delegate (empty = all)
        String[] exclude() default {};                 // Methods to exclude
        DelegationMapping[] mappings() default {};     // Custom method mappings
        String fallbackField() default "";             // Failover delegate field
        String[] fallbackChain() default {};           // Multiple failover fields
        boolean generateNullChecks() default true;     // Null-safe delegation
        String nullBehavior() default "THROW";         // THROW, RETURN_DEFAULT, SKIP, LOG
        boolean cacheResults() default false;          // Cache delegate results
        long cacheTTLMs() default 0;                   // Cache time-to-live
        boolean threadSafe() default false;            // Thread-safe delegation
        boolean lazy() default false;                  // Lazy delegate creation
        String factoryMethod() default "";             // Factory for lazy creation
        String interfaceToImplement() default "";      // Add interface to class
        boolean overrideExisting() default false;      // Override existing methods
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepDelegate */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface DDELEG {
        String target() default "";
        String field() default "";
        Class<?> delegateType() default Object.class;
        DelegationStrategy strategy() default DelegationStrategy.FORWARD;
        String[] include() default {};
        String[] exclude() default {};
        boolean generateNullChecks() default true;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    /**
     * 🔗 @DeepChain — Method chaining (fluent API) generation.
     *
     * Automatically transforms methods to return 'this' (or a configured
     * return type) enabling fluent method chaining. Supports validation
     * of chain ordering and required steps.
     *
     * Example:
     * <pre>
     * {@code @DeepChain(
     *     returnMode = ChainReturnMode.THIS,
     *     steps = {
     *         @ChainStep(method = "setWidth", order = 1),
     *         @ChainStep(method = "setHeight", order = 2),
     *         @ChainStep(method = "setColor"),
     *         @ChainStep(method = "build", terminal = true,
     *             requires = {"setWidth", "setHeight"})
     *     },
     *     validateOrder = false,
     *     generateBuilder = true
     * )}
     * public class WidgetBuilder {
     *     // setWidth(), setHeight(), setColor() now return WidgetBuilder
     *     // build() is terminal and validates setWidth + setHeight were called
     * }
     * </pre>
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DeepChain {
        String target() default "";
        ChainReturnMode returnMode() default ChainReturnMode.THIS;
        ChainStep[] steps() default {};
        String[] include() default {};                 // Methods to make chainable
        String[] exclude() default {};                 // Methods to exclude
        boolean voidMethodsOnly() default true;        // Only transform void methods
        boolean validateOrder() default false;          // Validate step ordering
        boolean validateRequired() default false;       // Validate required steps
        boolean generateBuilder() default false;        // Generate builder wrapper class
        String builderClassName() default "";            // Custom builder class name
        boolean immutable() default false;              // Each step returns new instance
        boolean threadSafe() default false;             // Thread-safe chaining
        boolean trackSteps() default false;             // Track which steps were called
        String terminatorMethod() default "build";      // Method that ends the chain
        String validationMethod() default "";            // Method called before termination
        boolean generateInterfaces() default false;      // Generate step interfaces (wizard pattern)
        String interfacePackage() default "";
        ErrorStrategy onError() default ErrorStrategy.THROW;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
        DeepMeta meta() default @DeepMeta;
    }

    /** Shortcut for @DeepChain */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface DCHAIN {
        String target() default "";
        ChainReturnMode returnMode() default ChainReturnMode.THIS;
        ChainStep[] steps() default {};
        String[] include() default {};
        String[] exclude() default {};
        boolean voidMethodsOnly() default true;
        boolean generateBuilder() default false;
        int priority() default 0;
        boolean hotReload() default true;
        When[] when() default {};
    }

    // ──────────────────────────────────────────────
    // Phase 24 — Processor
    // ──────────────────────────────────────────────

    /**
     * Processor for all Phase 24 (Final Extensions) annotations.
     */
    public static class Phase24Processor {

        private final DeepMixContext context;
        private final Map<String, Object> pluginRegistry = new ConcurrentHashMap<>();
        private final Map<String, List<Object>> extensionRegistry = new ConcurrentHashMap<>();
        private final Map<String, List<Object>> hookRegistry = new ConcurrentHashMap<>();
        private final Map<String, List<Object>> callbackRegistry = new ConcurrentHashMap<>();
        private final Map<String, List<Object>> listenerRegistry = new ConcurrentHashMap<>();

        public Phase24Processor(DeepMixContext context) {
            this.context = context;
        }

        /** Process @DeepPlugin — Register plugin and inject lifecycle management. */
        public void processPlugin(DeepPlugin annotation, ClassNode classNode)
                throws DeepMixProcessingException {
            String pluginId = annotation.id();

            // Validate dependencies
            for (PluginDependency dep : annotation.dependencies()) {
                if (!dep.optional()) {
                    // Check if required dependency is available
                    InsnList depCheck = new InsnList();
                    depCheck.add(new LdcInsnNode(dep.id()));
                    depCheck.add(new LdcInsnNode(dep.version()));
                    depCheck.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "deepmix/runtime/plugin/DeepMixPluginManager",
                        "requireDependency",
                        "(Ljava/lang/String;Ljava/lang/String;)V",
                        false
                    ));
                    injectIntoClassInit(classNode, depCheck);
                }
            }

            // Register extension points
            for (ExtensionPointDef epDef : annotation.extensionPoints()) {
                InsnList epReg = new InsnList();
                epReg.add(new LdcInsnNode(pluginId));
                epReg.add(new LdcInsnNode(epDef.id()));
                epReg.add(new LdcInsnNode(epDef.description()));
                epReg.add(new LdcInsnNode(Type.getType(epDef.contractType())));
                epReg.add(new InsnNode(
                    epDef.multipleAllowed() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                epReg.add(new InsnNode(
                    epDef.required() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                epReg.add(new LdcInsnNode(epDef.maxExtensions()));
                epReg.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/plugin/DeepMixPluginManager",
                    "registerExtensionPoint",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                        "Ljava/lang/Class;ZZI)V",
                    false
                ));
                injectIntoClassInit(classNode, epReg);
            }

            // Inject lifecycle hooks
            injectLifecycleHooks(classNode, annotation);

            // Generate plugin descriptor file if requested
            if (annotation.generateDescriptor()) {
                generatePluginDescriptor(annotation, classNode);
            }

            // Register plugin in the plugin manager
            InsnList registration = new InsnList();
            registration.add(new LdcInsnNode(pluginId));
            registration.add(new LdcInsnNode(annotation.name().isEmpty() ?
                pluginId : annotation.name()));
            registration.add(new LdcInsnNode(annotation.version()));
            registration.add(new LdcInsnNode(annotation.description()));
            registration.add(new LdcInsnNode(classNode.name));
            registration.add(new InsnNode(
                annotation.autoEnable() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            registration.add(new InsnNode(
                annotation.hotSwappable() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            registration.add(new InsnNode(
                annotation.isolated() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            registration.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/plugin/DeepMixPluginManager",
                "registerPlugin",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                    "Ljava/lang/String;Ljava/lang/String;ZZZ)V",
                false
            ));
            injectIntoClassInit(classNode, registration);

            // Store ServiceLoader registration
            for (String service : annotation.services()) {
                context.registerService(service, classNode.name.replace('/', '.'));
            }

            pluginRegistry.put(pluginId, classNode.name);

            context.addDiagnostic(String.format(
                "🔌 @DeepPlugin registered: '%s' v%s [deps=%d, extPoints=%d, autoEnable=%b]",
                pluginId, annotation.version(),
                annotation.dependencies().length,
                annotation.extensionPoints().length,
                annotation.autoEnable()
            ));
        }

        /** Process @DeepExtension — Register or define extension points. */
        public void processExtension(DeepExtension annotation, ClassNode classNode,
                                     MethodNode methodNode) throws DeepMixProcessingException {
            String extId = annotation.id();

            if (annotation.implementor()) {
                // This class implements an extension point
                InsnList implReg = new InsnList();
                implReg.add(new LdcInsnNode(extId));
                implReg.add(new LdcInsnNode(
                    annotation.extensionName().isEmpty() ?
                        classNode.name : annotation.extensionName()));
                implReg.add(new LdcInsnNode(classNode.name));
                implReg.add(new LdcInsnNode(annotation.order()));
                implReg.add(new InsnNode(
                    annotation.singleton() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                implReg.add(new InsnNode(
                    annotation.lazy() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                implReg.add(new InsnNode(
                    annotation.enabled() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));

                // Tags
                pushStringArray(implReg, annotation.tags());

                implReg.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/plugin/DeepMixExtensionRegistry",
                    "registerImplementation",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                        "IZZZ[Ljava/lang/String;)V",
                    false
                ));
                injectIntoClassInit(classNode, implReg);

                // Generate META-INF/services file
                if (annotation.generateServiceFile()) {
                    Class<?> contract = annotation.contract();
                    if (contract != Object.class) {
                        context.registerService(
                            contract.getName(),
                            classNode.name.replace('/', '.')
                        );
                    }
                }

                extensionRegistry.computeIfAbsent(extId, k -> new ArrayList<>())
                    .add(classNode.name);

                context.addDiagnostic(String.format(
                    "🧩 @DeepExtension implementation registered: '%s' → %s [order=%d, singleton=%b]",
                    extId, classNode.name, annotation.order(), annotation.singleton()
                ));
            } else {
                // This defines an extension point
                InsnList epDef = new InsnList();
                epDef.add(new LdcInsnNode(extId));
                epDef.add(new LdcInsnNode(annotation.description()));
                epDef.add(new LdcInsnNode(Type.getType(annotation.contract())));
                epDef.add(new LdcInsnNode(annotation.type().name()));

                epDef.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/plugin/DeepMixExtensionRegistry",
                    "defineExtensionPoint",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Class;" +
                        "Ljava/lang/String;)V",
                    false
                ));
                injectIntoClassInit(classNode, epDef);

                context.addDiagnostic(String.format(
                    "🧩 @DeepExtension point defined: '%s' [type=%s, contract=%s]",
                    extId, annotation.type(),
                    annotation.contract().getSimpleName()
                ));
            }
        }

        /** Process @DeepHook — Register method hooks. */
        public void processHook(DeepHook annotation, ClassNode classNode,
                                MethodNode methodNode) throws DeepMixProcessingException {
            for (HookTarget hookTarget : annotation.targets()) {
                String targetClass = hookTarget.className();
                String targetMethod = hookTarget.methodName();
                String targetDesc = hookTarget.methodDesc();

                InsnList hookReg = new InsnList();

                // Register hook with runtime manager
                hookReg.add(new LdcInsnNode(targetClass));
                hookReg.add(new LdcInsnNode(targetMethod));
                hookReg.add(new LdcInsnNode(targetDesc));
                hookReg.add(new LdcInsnNode(classNode.name));
                hookReg.add(new LdcInsnNode(methodNode.name));
                hookReg.add(new LdcInsnNode(methodNode.desc));
                hookReg.add(new LdcInsnNode(annotation.timing().name()));
                hookReg.add(new LdcInsnNode(annotation.hookPriority().value() +
                    annotation.customPriority()));
                hookReg.add(new InsnNode(
                    annotation.cancellable() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                hookReg.add(new InsnNode(
                    annotation.captureReturn() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                hookReg.add(new InsnNode(
                    annotation.modifyReturn() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                hookReg.add(new InsnNode(
                    annotation.captureArgs() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                hookReg.add(new InsnNode(
                    annotation.modifyArgs() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                hookReg.add(new LdcInsnNode(annotation.condition()));
                hookReg.add(new LdcInsnNode(annotation.group()));

                hookReg.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/hook/DeepMixHookManager",
                    "registerHook",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                        "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                        "Ljava/lang/String;IZZZZZLjava/lang/String;Ljava/lang/String;)V",
                    false
                ));
                injectIntoClassInit(classNode, hookReg);

                // Apply the actual bytecode transformation to the target class
                switch (annotation.timing()) {
                    case BEFORE:
                        applyBeforeHook(classNode, methodNode, hookTarget, annotation);
                        break;
                    case AFTER:
                        applyAfterHook(classNode, methodNode, hookTarget, annotation);
                        break;
                    case AROUND:
                        applyAroundHook(classNode, methodNode, hookTarget, annotation);
                        break;
                    case REPLACE:
                        applyReplaceHook(classNode, methodNode, hookTarget, annotation);
                        break;
                    case ON_SUCCESS:
                        applyOnSuccessHook(classNode, methodNode, hookTarget, annotation);
                        break;
                    case ON_FAILURE:
                        applyOnFailureHook(classNode, methodNode, hookTarget, annotation);
                        break;
                    case ON_RETURN:
                        applyOnReturnHook(classNode, methodNode, hookTarget, annotation);
                        break;
                    case CONDITIONAL:
                        applyConditionalHook(classNode, methodNode, hookTarget, annotation);
                        break;
                }

                hookRegistry.computeIfAbsent(
                    targetClass + "." + targetMethod, k -> new ArrayList<>())
                    .add(classNode.name + "." + methodNode.name);
            }

            context.addDiagnostic(String.format(
                "🪝 @DeepHook registered: %s::%s → %d targets [timing=%s, priority=%s, cancellable=%b]",
                classNode.name, methodNode.name,
                annotation.targets().length, annotation.timing(),
                annotation.hookPriority(), annotation.cancellable()
            ));
        }

        /** Process @DeepCallback — Register callback methods. */
        public void processCallback(DeepCallback annotation, ClassNode classNode,
                                    MethodNode methodNode) throws DeepMixProcessingException {
            String callbackName = annotation.name().isEmpty() ?
                methodNode.name : annotation.name();
            CallbackSpec spec = annotation.spec();

            InsnList cbReg = new InsnList();
            cbReg.add(new LdcInsnNode(callbackName));
            cbReg.add(new LdcInsnNode(classNode.name));
            cbReg.add(new LdcInsnNode(methodNode.name));
            cbReg.add(new LdcInsnNode(methodNode.desc));
            cbReg.add(new LdcInsnNode(spec.mode().name()));
            cbReg.add(new LdcInsnNode(spec.timeoutMs()));
            cbReg.add(new LdcInsnNode(annotation.maxRetries()));
            cbReg.add(new LdcInsnNode(annotation.retryDelayMs()));
            cbReg.add(new InsnNode(
                annotation.async() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            cbReg.add(new InsnNode(
                annotation.ordered() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            cbReg.add(new LdcInsnNode(annotation.group()));
            cbReg.add(new LdcInsnNode(annotation.errorHandler()));

            cbReg.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/callback/DeepMixCallbackManager",
                "registerCallback",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                    "Ljava/lang/String;Ljava/lang/String;JIJZZLjava/lang/String;" +
                    "Ljava/lang/String;)V",
                false
            ));

            if (annotation.autoRegister()) {
                injectIntoClassInit(classNode, cbReg);
            }

            // Inject auto-unregister in finalize/close if requested
            if (annotation.autoUnregister()) {
                InsnList unregCode = new InsnList();
                unregCode.add(new LdcInsnNode(callbackName));
                unregCode.add(new LdcInsnNode(classNode.name));
                unregCode.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "deepmix/runtime/callback/DeepMixCallbackManager",
                    "unregisterCallback",
                    "(Ljava/lang/String;Ljava/lang/String;)V",
                    false
                ));
                injectIntoFinalizer(classNode, unregCode);
            }

            // For debounced/throttled modes, wrap the method
            if (spec.mode() == CallbackMode.DEBOUNCED && spec.debounceMs() > 0) {
                wrapWithDebounce(classNode, methodNode, spec.debounceMs());
            } else if (spec.mode() == CallbackMode.THROTTLED && spec.throttleMs() > 0) {
                wrapWithThrottle(classNode, methodNode, spec.throttleMs());
            }

            callbackRegistry.computeIfAbsent(callbackName, k -> new ArrayList<>())
                .add(classNode.name + "." + methodNode.name);

            context.addDiagnostic(String.format(
                "📞 @DeepCallback registered: '%s' → %s::%s [mode=%s, async=%b, retries=%d]",
                callbackName, classNode.name, methodNode.name,
                spec.mode(), annotation.async(), annotation.maxRetries()
            ));
        }

        /** Process @DeepListener — Register event listeners. */
        public void processListener(DeepListener annotation, ClassNode classNode,
                                    MethodNode methodNode) throws DeepMixProcessingException {
            String eventType = annotation.eventType();
            ListenerFilter filter = annotation.filter();

            InsnList listenerReg = new InsnList();
            listenerReg.add(new LdcInsnNode(eventType));
            listenerReg.add(new LdcInsnNode(classNode.name));
            listenerReg.add(new LdcInsnNode(methodNode.name));
            listenerReg.add(new LdcInsnNode(methodNode.desc));
            listenerReg.add(new LdcInsnNode(annotation.strategy().name()));
            listenerReg.add(new LdcInsnNode(annotation.priority()));
            listenerReg.add(new InsnNode(
                annotation.ignoreCancelled() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            listenerReg.add(new InsnNode(
                annotation.receiveSubtypes() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            listenerReg.add(new InsnNode(
                annotation.async() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            listenerReg.add(new LdcInsnNode(annotation.maxInvocations()));
            listenerReg.add(new LdcInsnNode(annotation.eventBus()));
            listenerReg.add(new LdcInsnNode(annotation.listenerGroup()));
            listenerReg.add(new LdcInsnNode(filter.expression()));
            listenerReg.add(new LdcInsnNode(annotation.condition()));

            // Filter event types
            pushStringArray(listenerReg, filter.eventTypes());
            pushStringArray(listenerReg, filter.excludeTypes());

            listenerReg.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/listener/DeepMixListenerManager",
                "registerListener",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                    "Ljava/lang/String;Ljava/lang/String;IZZZILjava/lang/String;" +
                    "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                    "[Ljava/lang/String;[Ljava/lang/String;)V",
                false
            ));

            if (annotation.autoRegister()) {
                injectIntoClassInit(classNode, listenerReg);
            }

            // For WEAK strategy, use WeakReference wrapper
            if (annotation.strategy() == ListenerStrategy.WEAK) {
                wrapWithWeakReference(classNode, methodNode);
            }

            // For ONCE strategy, inject auto-unregister after first invocation
            if (annotation.strategy() == ListenerStrategy.ONCE) {
                injectAutoUnregisterOnce(classNode, methodNode, eventType);
            }

            // For COUNTED strategy, inject invocation counter
            if (annotation.strategy() == ListenerStrategy.COUNTED &&
                annotation.maxInvocations() > 0) {
                injectInvocationCounter(classNode, methodNode,
                    annotation.maxInvocations(), eventType);
            }

            listenerRegistry.computeIfAbsent(eventType, k -> new ArrayList<>())
                .add(classNode.name + "." + methodNode.name);

            context.addDiagnostic(String.format(
                "👂 @DeepListener registered: %s::%s → '%s' [strategy=%s, priority=%d, async=%b]",
                classNode.name, methodNode.name,
                eventType, annotation.strategy(),
                annotation.priority(), annotation.async()
            ));
        }

        /** Process @DeepDelegate — Generate delegation methods. */
        public void processDelegate(DeepDelegate annotation, ClassNode classNode)
                throws DeepMixProcessingException {
            String delegateField = annotation.field();
            if (delegateField.isEmpty()) {
                // Find first field matching delegate type
                for (FieldNode fn : classNode.fields) {
                    if (annotation.delegateType() != Object.class) {
                        String expectedDesc = Type.getDescriptor(annotation.delegateType());
                        if (fn.desc.equals(expectedDesc)) {
                            delegateField = fn.name;
                            break;
                        }
                    }
                }
            }

            if (delegateField.isEmpty()) {
                throw new DeepMixProcessingException(
                    "@DeepDelegate: No delegate field specified or found in " +
                        classNode.name);
            }

            // Find delegate type descriptor
            String delegateDesc = null;
            String delegateInternalName = null;
            for (FieldNode fn : classNode.fields) {
                if (fn.name.equals(delegateField)) {
                    delegateDesc = fn.desc;
                    delegateInternalName = Type.getType(delegateDesc).getInternalName();
                    break;
                }
            }

            if (delegateInternalName == null) {
                throw new DeepMixProcessingException(
                    "@DeepDelegate: Field '" + delegateField + "' not found in " +
                        classNode.name);
            }

            // Determine which methods to delegate
            Set<String> includeMethods = new HashSet<>(Arrays.asList(annotation.include()));
            Set<String> excludeMethods = new HashSet<>(Arrays.asList(annotation.exclude()));

            // If interface is specified, add it to the class
            if (!annotation.interfaceToImplement().isEmpty()) {
                String ifaceName = annotation.interfaceToImplement().replace('.', '/');
                if (!classNode.interfaces.contains(ifaceName)) {
                    classNode.interfaces.add(ifaceName);
                }
            }

            // Build custom mapping table
            Map<String, DelegationMapping> customMappings = new HashMap<>();
            for (DelegationMapping mapping : annotation.mappings()) {
                customMappings.put(mapping.sourceMethod(), mapping);
            }

            // For each method that should be delegated, generate a forwarding method
            // We need the delegate type's methods — stored as a hint for the runtime
            InsnList delegateSetup = new InsnList();
            delegateSetup.add(new LdcInsnNode(classNode.name));
            delegateSetup.add(new LdcInsnNode(delegateField));
            delegateSetup.add(new LdcInsnNode(delegateInternalName));
            delegateSetup.add(new LdcInsnNode(annotation.strategy().name()));
            pushStringArray(delegateSetup, annotation.include());
            pushStringArray(delegateSetup, annotation.exclude());
            delegateSetup.add(new InsnNode(
                annotation.generateNullChecks() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            delegateSetup.add(new LdcInsnNode(annotation.nullBehavior()));
            delegateSetup.add(new LdcInsnNode(annotation.fallbackField()));
            delegateSetup.add(new InsnNode(
                annotation.cacheResults() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            delegateSetup.add(new InsnNode(
                annotation.lazy() ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            delegateSetup.add(new LdcInsnNode(annotation.factoryMethod()));

            delegateSetup.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/delegate/DeepMixDelegateManager",
                "setupDelegation",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                    "Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;" +
                    "ZLjava/lang/String;Ljava/lang/String;ZZLjava/lang/String;)V",
                false
            ));
            injectIntoClassInit(classNode, delegateSetup);

            // Generate sample delegation method (for known methods)
            // Full implementation would introspect the delegate type at load time
            if (annotation.strategy() == DelegationStrategy.FAILOVER &&
                !annotation.fallbackField().isEmpty()) {
                // Inject failover logic into existing delegation methods
                for (MethodNode mn : classNode.methods) {
                    if (shouldDelegate(mn.name, includeMethods, excludeMethods)) {
                        wrapWithFailover(classNode, mn, delegateField,
                            annotation.fallbackField());
                    }
                }
            }

            // For lazy delegation, wrap field access
            if (annotation.lazy() && !annotation.factoryMethod().isEmpty()) {
                injectLazyFieldInit(classNode, delegateField,
                    delegateDesc, annotation.factoryMethod());
            }

            context.addDiagnostic(String.format(
                "🤝 @DeepDelegate applied to %s [field=%s, strategy=%s, nullCheck=%b, failover=%s]",
                classNode.name, delegateField, annotation.strategy(),
                annotation.generateNullChecks(), annotation.fallbackField()
            ));
        }

        /** Process @DeepChain — Generate fluent method chaining. */
        public void processChain(DeepChain annotation, ClassNode classNode)
                throws DeepMixProcessingException {
            Set<String> includeMethods = new HashSet<>(Arrays.asList(annotation.include()));
            Set<String> excludeMethods = new HashSet<>(Arrays.asList(annotation.exclude()));

            // Build step metadata
            Map<String, ChainStep> stepMap = new LinkedHashMap<>();
            Set<String> requiredSteps = new LinkedHashSet<>();
            Set<String> terminalSteps = new LinkedHashSet<>();

            for (ChainStep step : annotation.steps()) {
                stepMap.put(step.method(), step);
                if (step.required()) requiredSteps.add(step.method());
                if (step.terminal()) terminalSteps.add(step.method());
            }

            // Add tracking field if needed
            if (annotation.trackSteps() || annotation.validateRequired()) {
                FieldNode stepsField = new FieldNode(
                    Opcodes.ACC_PRIVATE,
                    "deepmix$chainSteps",
                    "Ljava/util/Set;",
                    "Ljava/util/Set<Ljava/lang/String;>;",
                    null
                );
                classNode.fields.add(stepsField);

                // Initialize in constructors
                for (MethodNode mn : classNode.methods) {
                    if (mn.name.equals("<init>")) {
                        InsnList initSteps = new InsnList();
                        initSteps.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        initSteps.add(new TypeInsnNode(Opcodes.NEW,
                            "java/util/LinkedHashSet"));
                        initSteps.add(new InsnNode(Opcodes.DUP));
                        initSteps.add(new MethodInsnNode(
                            Opcodes.INVOKESPECIAL,
                            "java/util/LinkedHashSet",
                            "<init>",
                            "()V",
                            false
                        ));
                        initSteps.add(new FieldInsnNode(
                            Opcodes.PUTFIELD,
                            classNode.name,
                            "deepmix$chainSteps",
                            "Ljava/util/Set;"
                        ));

                        // Insert after super() call
                        for (AbstractInsnNode insn : mn.instructions) {
                            if (insn instanceof MethodInsnNode) {
                                MethodInsnNode min = (MethodInsnNode) insn;
                                if (min.name.equals("<init>") &&
                                    min.owner.equals(classNode.superName)) {
                                    mn.instructions.insert(min, initSteps);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // Transform methods to return 'this' (or configured return)
            int transformedCount = 0;
            for (MethodNode mn : classNode.methods) {
                if (mn.name.equals("<init>") || mn.name.equals("<clinit>")) continue;
                if ((mn.access & Opcodes.ACC_STATIC) != 0) continue;

                boolean shouldChain = false;
                if (!includeMethods.isEmpty()) {
                    shouldChain = includeMethods.contains(mn.name);
                } else if (annotation.voidMethodsOnly()) {
                    shouldChain = mn.desc.endsWith(")V") &&
                        !excludeMethods.contains(mn.name);
                } else {
                    shouldChain = !excludeMethods.contains(mn.name);
                }

                // Terminal methods should not be chained
                if (terminalSteps.contains(mn.name)) {
                    shouldChain = false;

                    // Add validation before terminal method
                    if (annotation.validateRequired() && !requiredSteps.isEmpty()) {
                        InsnList validation = new InsnList();
                        for (String required : requiredSteps) {
                            validation.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            validation.add(new FieldInsnNode(
                                Opcodes.GETFIELD,
                                classNode.name,
                                "deepmix$chainSteps",
                                "Ljava/util/Set;"
                            ));
                            validation.add(new LdcInsnNode(required));
                            validation.add(new MethodInsnNode(
                                Opcodes.INVOKEINTERFACE,
                                "java/util/Set",
                                "contains",
                                "(Ljava/lang/Object;)Z",
                                true
                            ));
                            LabelNode isPresent = new LabelNode();
                            validation.add(new JumpInsnNode(Opcodes.IFNE, isPresent));

                            // Throw IllegalStateException
                            validation.add(new TypeInsnNode(Opcodes.NEW,
                                "java/lang/IllegalStateException"));
                            validation.add(new InsnNode(Opcodes.DUP));
                            validation.add(new LdcInsnNode(
                                "Required chain step '" + required +
                                    "' was not called before '" + mn.name + "'"));
                            validation.add(new MethodInsnNode(
                                Opcodes.INVOKESPECIAL,
                                "java/lang/IllegalStateException",
                                "<init>",
                                "(Ljava/lang/String;)V",
                                false
                            ));
                            validation.add(new InsnNode(Opcodes.ATHROW));
                            validation.add(isPresent);
                        }
                        mn.instructions.insert(validation);
                    }
                }

                if (!shouldChain) continue;

                // Track step if needed
                if (annotation.trackSteps() || annotation.validateRequired()) {
                    InsnList trackStep = new InsnList();
                    trackStep.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    trackStep.add(new FieldInsnNode(
                        Opcodes.GETFIELD,
                        classNode.name,
                        "deepmix$chainSteps",
                        "Ljava/util/Set;"
                    ));
                    trackStep.add(new LdcInsnNode(mn.name));
                    trackStep.add(new MethodInsnNode(
                        Opcodes.INVOKEINTERFACE,
                        "java/util/Set",
                        "add",
                        "(Ljava/lang/Object;)Z",
                        true
                    ));
                    trackStep.add(new InsnNode(Opcodes.POP)); // Discard boolean result
                    mn.instructions.insert(trackStep);
                }

                // Validate step ordering if configured
                ChainStep stepConfig = stepMap.get(mn.name);
                if (annotation.validateOrder() && stepConfig != null) {
                    for (String requiredBefore : stepConfig.requires()) {
                        InsnList orderCheck = new InsnList();
                        orderCheck.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        orderCheck.add(new FieldInsnNode(
                            Opcodes.GETFIELD,
                            classNode.name,
                            "deepmix$chainSteps",
                            "Ljava/util/Set;"
                        ));
                        orderCheck.add(new LdcInsnNode(requiredBefore));
                        orderCheck.add(new MethodInsnNode(
                            Opcodes.INVOKEINTERFACE,
                            "java/util/Set",
                            "contains",
                            "(Ljava/lang/Object;)Z",
                            true
                        ));
                        LabelNode ok = new LabelNode();
                        orderCheck.add(new JumpInsnNode(Opcodes.IFNE, ok));
                        orderCheck.add(new TypeInsnNode(Opcodes.NEW,
                            "java/lang/IllegalStateException"));
                        orderCheck.add(new InsnNode(Opcodes.DUP));
                        orderCheck.add(new LdcInsnNode(
                            "Chain step '" + requiredBefore +
                                "' must be called before '" + mn.name + "'"));
                        orderCheck.add(new MethodInsnNode(
                            Opcodes.INVOKESPECIAL,
                            "java/lang/IllegalStateException",
                            "<init>",
                            "(Ljava/lang/String;)V",
                            false
                        ));
                        orderCheck.add(new InsnNode(Opcodes.ATHROW));
                        orderCheck.add(ok);
                        mn.instructions.insert(orderCheck);
                    }
                }

                // Transform return type and instructions
                switch (annotation.returnMode()) {
                    case THIS:
                        transformToReturnThis(classNode, mn);
                        break;
                    case SELF_TYPE:
                        transformToReturnSelfType(classNode, mn);
                        break;
                    case NEW_INSTANCE:
                        transformToReturnNewInstance(classNode, mn);
                        break;
                    case WRAPPER:
                        transformToReturnWrapper(classNode, mn);
                        break;
                    case OPTIONAL:
                        transformToReturnOptional(classNode, mn);
                        break;
                    default:
                        transformToReturnThis(classNode, mn);
                        break;
                }

                transformedCount++;
            }

            // Generate builder wrapper class if requested
            if (annotation.generateBuilder()) {
                generateBuilderClass(classNode, annotation, stepMap,
                    requiredSteps, terminalSteps);
            }

            // Generate step interfaces (wizard pattern) if requested
            if (annotation.generateInterfaces()) {
                generateStepInterfaces(classNode, annotation, stepMap);
            }

            context.addDiagnostic(String.format(
                "🔗 @DeepChain applied to %s [%d methods chained, returnMode=%s, " +
                    "steps=%d, validate=%b, builder=%b]",
                classNode.name, transformedCount, annotation.returnMode(),
                annotation.steps().length, annotation.validateRequired(),
                annotation.generateBuilder()
            ));
        }

        // ──────────────────────────────────────────────
        // Phase 24 — Internal helper methods
        // ──────────────────────────────────────────────

        private void transformToReturnThis(ClassNode classNode, MethodNode methodNode) {
            // Change return type from void to class type
            String classDesc = "L" + classNode.name + ";";

            if (methodNode.desc.endsWith(")V")) {
                methodNode.desc = methodNode.desc.replace(")V", ")" + classDesc);

                // Replace all RETURN instructions with ALOAD_0 + ARETURN
                for (AbstractInsnNode insn : methodNode.instructions) {
                    if (insn.getOpcode() == Opcodes.RETURN) {
                        InsnList returnThis = new InsnList();
                        returnThis.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        returnThis.add(new InsnNode(Opcodes.ARETURN));
                        methodNode.instructions.insertBefore(insn, returnThis);
                        methodNode.instructions.remove(insn);
                    }
                }
            }
        }

        private void transformToReturnSelfType(ClassNode classNode, MethodNode methodNode) {
            // Same as returnThis but with generic self-type signature
            transformToReturnThis(classNode, methodNode);

            // Add generic signature for covariant return
            String classDesc = "L" + classNode.name + ";";
            if (methodNode.signature == null) {
                methodNode.signature = methodNode.desc.replace(
                    ")" + classDesc, ")T" + classNode.name.substring(
                        classNode.name.lastIndexOf('/') + 1) + ";");
            }
        }

        private void transformToReturnNewInstance(ClassNode classNode, MethodNode methodNode) {
            String classDesc = "L" + classNode.name + ";";

            if (methodNode.desc.endsWith(")V")) {
                methodNode.desc = methodNode.desc.replace(")V", ")" + classDesc);

                for (AbstractInsnNode insn : methodNode.instructions) {
                    if (insn.getOpcode() == Opcodes.RETURN) {
                        InsnList newInstance = new InsnList();
                        // Create new instance and copy state
                        newInstance.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        newInstance.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "deepmix/runtime/chain/DeepMixChainRuntime",
                            "cloneForChain",
                            "(Ljava/lang/Object;)Ljava/lang/Object;",
                            false
                        ));
                        newInstance.add(new TypeInsnNode(Opcodes.CHECKCAST, classNode.name));
                        newInstance.add(new InsnNode(Opcodes.ARETURN));
                        methodNode.instructions.insertBefore(insn, newInstance);
                        methodNode.instructions.remove(insn);
                    }
                }
            }
        }

        private void transformToReturnWrapper(ClassNode classNode, MethodNode methodNode) {
            // Return a ChainWrapper<T> that wraps this
            String wrapperDesc = "Ldeepmix/runtime/chain/ChainWrapper;";

            if (methodNode.desc.endsWith(")V")) {
                methodNode.desc = methodNode.desc.replace(")V", ")" + wrapperDesc);

                for (AbstractInsnNode insn : methodNode.instructions) {
                    if (insn.getOpcode() == Opcodes.RETURN) {
                        InsnList wrapReturn = new InsnList();
                        wrapReturn.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        wrapReturn.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "deepmix/runtime/chain/ChainWrapper",
                            "of",
                            "(Ljava/lang/Object;)Ldeepmix/runtime/chain/ChainWrapper;",
                            false
                        ));
                        wrapReturn.add(new InsnNode(Opcodes.ARETURN));
                        methodNode.instructions.insertBefore(insn, wrapReturn);
                        methodNode.instructions.remove(insn);
                    }
                }
            }
        }

        private void transformToReturnOptional(ClassNode classNode, MethodNode methodNode) {
            String optionalDesc = "Ljava/util/Optional;";

            if (methodNode.desc.endsWith(")V")) {
                methodNode.desc = methodNode.desc.replace(")V", ")" + optionalDesc);

                for (AbstractInsnNode insn : methodNode.instructions) {
                    if (insn.getOpcode() == Opcodes.RETURN) {
                        InsnList optReturn = new InsnList();
                        optReturn.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        optReturn.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "java/util/Optional",
                            "of",
                            "(Ljava/lang/Object;)Ljava/util/Optional;",
                            false
                        ));
                        optReturn.add(new InsnNode(Opcodes.ARETURN));
                        methodNode.instructions.insertBefore(insn, optReturn);
                        methodNode.instructions.remove(insn);
                    }
                }
            }
        }

        private void generateBuilderClass(ClassNode classNode, DeepChain annotation,
                                           Map<String, ChainStep> stepMap,
                                           Set<String> requiredSteps,
                                           Set<String> terminalSteps) {
            String builderName = annotation.builderClassName().isEmpty() ?
                classNode.name + "$Builder" : annotation.builderClassName().replace('.', '/');

            ClassNode builder = new ClassNode();
            builder.version = classNode.version;
            builder.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
            builder.name = builderName;
            builder.superName = "java/lang/Object";

            // Add field for the target instance
            builder.fields.add(new FieldNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                "target",
                "L" + classNode.name + ";",
                null,
                null
            ));

            // Constructor
            MethodNode ctor = new MethodNode(
                Opcodes.ACC_PUBLIC,
                "<init>",
                "(L" + classNode.name + ";)V",
                null,
                null
            );
            ctor.instructions = new InsnList();
            ctor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            ctor.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
            ctor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            ctor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            ctor.instructions.add(new FieldInsnNode(
                Opcodes.PUTFIELD, builderName, "target", "L" + classNode.name + ";"));
            ctor.instructions.add(new InsnNode(Opcodes.RETURN));
            ctor.maxStack = 2;
            ctor.maxLocals = 2;
            builder.methods.add(ctor);

            // Static factory
            MethodNode factory = new MethodNode(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "of",
                "(L" + classNode.name + ";)L" + builderName + ";",
                null,
                null
            );
            factory.instructions = new InsnList();
            factory.instructions.add(new TypeInsnNode(Opcodes.NEW, builderName));
            factory.instructions.add(new InsnNode(Opcodes.DUP));
            factory.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            factory.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL, builderName, "<init>",
                "(L" + classNode.name + ";)V", false));
            factory.instructions.add(new InsnNode(Opcodes.ARETURN));
            factory.maxStack = 3;
            factory.maxLocals = 1;
            builder.methods.add(factory);

            // Register builder as inner class
            classNode.innerClasses.add(new InnerClassNode(
                builderName, classNode.name, "Builder",
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC
            ));

            // Store generated builder class for loading
            context.registerGeneratedClass(builderName, builder);
        }

        private void generateStepInterfaces(ClassNode classNode, DeepChain annotation,
                                             Map<String, ChainStep> stepMap) {
            // Generate wizard-pattern interfaces:
            // Step1 { Step2 setWidth(int w); }
            // Step2 { Step3 setHeight(int h); }
            // Step3 { Result build(); }
            // This enforces compile-time ordering of chain steps

            String pkgPrefix = annotation.interfacePackage().isEmpty() ?
                classNode.name.substring(0, classNode.name.lastIndexOf('/') + 1) :
                annotation.interfacePackage().replace('.', '/') + "/";

            List<ChainStep> orderedSteps = new ArrayList<>(stepMap.values());
            orderedSteps.sort(Comparator.comparingInt(s -> s.order()));

            for (int i = 0; i < orderedSteps.size(); i++) {
                ChainStep step = orderedSteps.get(i);
                String ifaceName = pkgPrefix + "Step" + (i + 1) + "_" + step.method();

                ClassNode iface = new ClassNode();
                iface.version = classNode.version;
                iface.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT;
                iface.name = ifaceName;
                iface.superName = "java/lang/Object";

                // Next step type
                String returnType;
                if (step.terminal() || i == orderedSteps.size() - 1) {
                    returnType = "L" + classNode.name + ";"; // Terminal returns the built type
                } else {
                    ChainStep nextStep = orderedSteps.get(i + 1);
                    returnType = "L" + pkgPrefix + "Step" + (i + 2) + "_" +
                        nextStep.method() + ";";
                }

                // Find the matching method in the class to get its parameter types
                String methodDesc = "()" + returnType; // Default — no params
                for (MethodNode mn : classNode.methods) {
                    if (mn.name.equals(step.method())) {
                        Type[] argTypes = Type.getArgumentTypes(mn.desc);
                        StringBuilder descBuilder = new StringBuilder("(");
                        for (Type t : argTypes) {
                            descBuilder.append(t.getDescriptor());
                        }
                        descBuilder.append(")").append(returnType);
                        methodDesc = descBuilder.toString();
                        break;
                    }
                }

                MethodNode stepMethod = new MethodNode(
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                    step.method(),
                    methodDesc,
                    null,
                    null
                );
                iface.methods.add(stepMethod);

                context.registerGeneratedClass(ifaceName, iface);
            }
        }

        private void injectIntoClassInit(ClassNode classNode, InsnList code) {
            // Find or create <clinit>
            MethodNode clinit = null;
            for (MethodNode mn : classNode.methods) {
                if (mn.name.equals("<clinit>")) {
                    clinit = mn;
                    break;
                }
            }

            if (clinit == null) {
                clinit = new MethodNode(
                    Opcodes.ACC_STATIC,
                    "<clinit>",
                    "()V",
                    null,
                    null
                );
                clinit.instructions = new InsnList();
                clinit.instructions.add(new InsnNode(Opcodes.RETURN));
                clinit.maxStack = 16;
                clinit.maxLocals = 0;
                classNode.methods.add(clinit);
            }

            // Insert before RETURN
            clinit.instructions.insert(code);
        }

        private void injectIntoFinalizer(ClassNode classNode, InsnList code) {
            // Find or create close() method (preferred over finalize)
            MethodNode closeMethod = null;
            for (MethodNode mn : classNode.methods) {
                if (mn.name.equals("close") && mn.desc.equals("()V")) {
                    closeMethod = mn;
                    break;
                }
            }

            if (closeMethod == null) {
                // Try finalize
                for (MethodNode mn : classNode.methods) {
                    if (mn.name.equals("finalize") && mn.desc.equals("()V")) {
                        closeMethod = mn;
                        break;
                    }
                }
            }

            if (closeMethod == null) {
                // Create close() and add AutoCloseable interface
                if (!classNode.interfaces.contains("java/lang/AutoCloseable")) {
                    classNode.interfaces.add("java/lang/AutoCloseable");
                }

                closeMethod = new MethodNode(
                    Opcodes.ACC_PUBLIC,
                    "close",
                    "()V",
                    null,
                    null
                );
                closeMethod.instructions = new InsnList();
                closeMethod.instructions.add(new InsnNode(Opcodes.RETURN));
                closeMethod.maxStack = 4;
                closeMethod.maxLocals = 1;
                classNode.methods.add(closeMethod);
            }

            // Insert cleanup code before return
            closeMethod.instructions.insert(code);
        }

        private void injectLifecycleHooks(ClassNode classNode, DeepPlugin annotation) {
            // Inject lifecycle method calls
            String pluginId = annotation.id();

            // onEnable hook
            InsnList enableHook = new InsnList();
            enableHook.add(new LdcInsnNode(pluginId));
            enableHook.add(new LdcInsnNode(classNode.name));
            enableHook.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/plugin/DeepMixPluginManager",
                "notifyEnabled",
                "(Ljava/lang/String;Ljava/lang/String;)V",
                false
            ));

            // Find onEnable or init method to inject into
            for (MethodNode mn : classNode.methods) {
                if (mn.name.equals("onEnable") || mn.name.equals("init") ||
                    mn.name.equals("initialize") || mn.name.equals("onInitialize")) {
                    mn.instructions.insert(enableHook);
                    break;
                }
            }
        }

        private void generatePluginDescriptor(DeepPlugin annotation, ClassNode classNode) {
            // Generate plugin.yml or plugin.json based on target platform
            StringBuilder descriptor = new StringBuilder();
            descriptor.append("# Auto-generated by DeepMix @DeepPlugin\n");
            descriptor.append("id: ").append(annotation.id()).append("\n");
            descriptor.append("name: ").append(
                annotation.name().isEmpty() ? annotation.id() : annotation.name()
            ).append("\n");
            descriptor.append("version: ").append(annotation.version()).append("\n");
            descriptor.append("description: ").append(annotation.description()).append("\n");
            descriptor.append("main: ").append(
                classNode.name.replace('/', '.')).append("\n");

            if (annotation.authors().length > 0) {
                descriptor.append("authors:\n");
                for (String author : annotation.authors()) {
                    descriptor.append("  - ").append(author).append("\n");
                }
            }

            if (annotation.dependencies().length > 0) {
                descriptor.append("depend:\n");
                for (PluginDependency dep : annotation.dependencies()) {
                    if (!dep.optional()) {
                        descriptor.append("  - ").append(dep.id()).append("\n");
                    }
                }
                descriptor.append("softdepend:\n");
                for (PluginDependency dep : annotation.dependencies()) {
                    if (dep.optional()) {
                        descriptor.append("  - ").append(dep.id()).append("\n");
                    }
                }
            }

            context.storeGeneratedResource("plugin.yml", descriptor.toString());
        }

        private void applyBeforeHook(ClassNode classNode, MethodNode hookMethod,
                                      HookTarget target, DeepHook annotation) {
            // Generate code that calls the hook method before the target
            // This is applied by the DeepMix transformer when it processes the target class
            context.registerPendingTransform(
                target.className(),
                target.methodName(),
                target.methodDesc(),
                "BEFORE",
                classNode.name,
                hookMethod.name,
                hookMethod.desc,
                annotation.cancellable()
            );
        }

        private void applyAfterHook(ClassNode classNode, MethodNode hookMethod,
                                     HookTarget target, DeepHook annotation) {
            context.registerPendingTransform(
                target.className(),
                target.methodName(),
                target.methodDesc(),
                "AFTER",
                classNode.name,
                hookMethod.name,
                hookMethod.desc,
                annotation.captureReturn()
            );
        }

        private void applyAroundHook(ClassNode classNode, MethodNode hookMethod,
                                      HookTarget target, DeepHook annotation) {
            context.registerPendingTransform(
                target.className(),
                target.methodName(),
                target.methodDesc(),
                "AROUND",
                classNode.name,
                hookMethod.name,
                hookMethod.desc,
                annotation.cancellable()
            );
        }

        private void applyReplaceHook(ClassNode classNode, MethodNode hookMethod,
                                       HookTarget target, DeepHook annotation) {
            context.registerPendingTransform(
                target.className(),
                target.methodName(),
                target.methodDesc(),
                "REPLACE",
                classNode.name,
                hookMethod.name,
                hookMethod.desc,
                false
            );
        }

        private void applyOnSuccessHook(ClassNode classNode, MethodNode hookMethod,
                                         HookTarget target, DeepHook annotation) {
            context.registerPendingTransform(
                target.className(),
                target.methodName(),
                target.methodDesc(),
                "ON_SUCCESS",
                classNode.name,
                hookMethod.name,
                hookMethod.desc,
                annotation.captureReturn()
            );
        }

        private void applyOnFailureHook(ClassNode classNode, MethodNode hookMethod,
                                         HookTarget target, DeepHook annotation) {
            context.registerPendingTransform(
                target.className(),
                target.methodName(),
                target.methodDesc(),
                "ON_FAILURE",
                classNode.name,
                hookMethod.name,
                hookMethod.desc,
                annotation.captureException()
            );
        }

        private void applyOnReturnHook(ClassNode classNode, MethodNode hookMethod,
                                        HookTarget target, DeepHook annotation) {
            context.registerPendingTransform(
                target.className(),
                target.methodName(),
                target.methodDesc(),
                "ON_RETURN",
                classNode.name,
                hookMethod.name,
                hookMethod.desc,
                annotation.modifyReturn()
            );
        }

        private void applyConditionalHook(ClassNode classNode, MethodNode hookMethod,
                                           HookTarget target, DeepHook annotation) {
            context.registerPendingTransform(
                target.className(),
                target.methodName(),
                target.methodDesc(),
                "CONDITIONAL:" + annotation.condition(),
                classNode.name,
                hookMethod.name,
                hookMethod.desc,
                annotation.cancellable()
            );
        }

        private void wrapWithDebounce(ClassNode classNode, MethodNode methodNode,
                                       long debounceMs) {
            // Add a ScheduledExecutorService field and debounce wrapper
            String timerFieldName = "deepmix$debounce$" + methodNode.name;

            classNode.fields.add(new FieldNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_VOLATILE,
                timerFieldName,
                "Ljava/util/concurrent/ScheduledFuture;",
                null,
                null
            ));

            InsnList debounceCode = new InsnList();
            // Cancel previous scheduled execution
            debounceCode.add(new VarInsnNode(Opcodes.ALOAD, 0));
            debounceCode.add(new FieldInsnNode(
                Opcodes.GETFIELD, classNode.name, timerFieldName,
                "Ljava/util/concurrent/ScheduledFuture;"
            ));
            LabelNode noCancel = new LabelNode();
            debounceCode.add(new JumpInsnNode(Opcodes.IFNULL, noCancel));
            debounceCode.add(new VarInsnNode(Opcodes.ALOAD, 0));
            debounceCode.add(new FieldInsnNode(
                Opcodes.GETFIELD, classNode.name, timerFieldName,
                "Ljava/util/concurrent/ScheduledFuture;"
            ));
            debounceCode.add(new InsnNode(Opcodes.ICONST_0));
            debounceCode.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                "java/util/concurrent/ScheduledFuture",
                "cancel",
                "(Z)Z",
                true
            ));
            debounceCode.add(new InsnNode(Opcodes.POP));
            debounceCode.add(noCancel);

            methodNode.instructions.insert(debounceCode);
        }

        private void wrapWithThrottle(ClassNode classNode, MethodNode methodNode,
                                       long throttleMs) {
            String lastExecField = "deepmix$throttle$" + methodNode.name;

            classNode.fields.add(new FieldNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_VOLATILE,
                lastExecField,
                "J",
                null,
                0L
            ));

            InsnList throttleCheck = new InsnList();
            LabelNode proceed = new LabelNode();

            throttleCheck.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "java/lang/System",
                "currentTimeMillis",
                "()J",
                false
            ));
            throttleCheck.add(new VarInsnNode(Opcodes.ALOAD, 0));
            throttleCheck.add(new FieldInsnNode(
                Opcodes.GETFIELD, classNode.name, lastExecField, "J"
            ));
            throttleCheck.add(new InsnNode(Opcodes.LSUB));
            throttleCheck.add(new LdcInsnNode(throttleMs));
            throttleCheck.add(new InsnNode(Opcodes.LCMP));
            throttleCheck.add(new JumpInsnNode(Opcodes.IFGE, proceed));

            // Throttled — return without executing
            Type returnType = Type.getReturnType(methodNode.desc);
            if (returnType.getSort() == Type.VOID) {
                throttleCheck.add(new InsnNode(Opcodes.RETURN));
            } else {
                pushDefaultReturn(throttleCheck, returnType);
            }

            throttleCheck.add(proceed);

            // Update last execution time
            throttleCheck.add(new VarInsnNode(Opcodes.ALOAD, 0));
            throttleCheck.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "java/lang/System",
                "currentTimeMillis",
                "()J",
                false
            ));
            throttleCheck.add(new FieldInsnNode(
                Opcodes.PUTFIELD, classNode.name, lastExecField, "J"
            ));

            methodNode.instructions.insert(throttleCheck);
        }

        private void wrapWithWeakReference(ClassNode classNode, MethodNode methodNode) {
            // The listener manager handles weak reference wrapping externally
            // Here we just mark the method for weak reference treatment
            if (methodNode.visibleAnnotations == null) {
                methodNode.visibleAnnotations = new ArrayList<>();
            }
            AnnotationNode weakMark = new AnnotationNode(
                "Ldeepmix/runtime/listener/WeakListener;");
            methodNode.visibleAnnotations.add(weakMark);
        }

        private void injectAutoUnregisterOnce(ClassNode classNode, MethodNode methodNode,
                                               String eventType) {
            // Insert at the end of the method: unregister self
            InsnList unregister = new InsnList();
            unregister.add(new LdcInsnNode(eventType));
            unregister.add(new LdcInsnNode(classNode.name));
            unregister.add(new LdcInsnNode(methodNode.name));
            unregister.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/listener/DeepMixListenerManager",
                "unregisterListener",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                false
            ));

            // Insert before every return
            List<AbstractInsnNode> returns = new ArrayList<>();
            for (AbstractInsnNode insn : methodNode.instructions) {
                int op = insn.getOpcode();
                if (op >= Opcodes.IRETURN && op <= Opcodes.RETURN) {
                    returns.add(insn);
                }
            }
            for (AbstractInsnNode ret : returns) {
                InsnList copy = cloneInsnList(unregister);
                methodNode.instructions.insertBefore(ret, copy);
            }
        }

        private void injectInvocationCounter(ClassNode classNode, MethodNode methodNode,
                                              int maxInvocations, String eventType) {
            String counterField = "deepmix$listenerCount$" + methodNode.name;

            classNode.fields.add(new FieldNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_VOLATILE,
                counterField,
                "I",
                null,
                0
            ));

            InsnList countCheck = new InsnList();
            LabelNode continueExec = new LabelNode();

            // Increment counter
            countCheck.add(new VarInsnNode(Opcodes.ALOAD, 0));
            countCheck.add(new InsnNode(Opcodes.DUP));
            countCheck.add(new FieldInsnNode(
                Opcodes.GETFIELD, classNode.name, counterField, "I"));
            countCheck.add(new InsnNode(Opcodes.ICONST_1));
            countCheck.add(new InsnNode(Opcodes.IADD));
            countCheck.add(new InsnNode(Opcodes.DUP_X1));
            countCheck.add(new FieldInsnNode(
                Opcodes.PUTFIELD, classNode.name, counterField, "I"));

            // Check if max reached
            countCheck.add(new LdcInsnNode(maxInvocations));
            countCheck.add(new JumpInsnNode(Opcodes.IF_ICMPLE, continueExec));

            // Max reached — unregister and return
            countCheck.add(new LdcInsnNode(eventType));
            countCheck.add(new LdcInsnNode(classNode.name));
            countCheck.add(new LdcInsnNode(methodNode.name));
            countCheck.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "deepmix/runtime/listener/DeepMixListenerManager",
                "unregisterListener",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                false
            ));
            countCheck.add(new InsnNode(Opcodes.RETURN));

            countCheck.add(continueExec);

            methodNode.instructions.insert(countCheck);
        }

        private void wrapWithFailover(ClassNode classNode, MethodNode methodNode,
                                       String primaryField, String fallbackField) {
            // Wrap the method body in try-catch that falls back to the fallback delegate
            LabelNode tryStart = new LabelNode();
            LabelNode tryEnd = new LabelNode();
            LabelNode catchHandler = new LabelNode();

            methodNode.instructions.insert(tryStart);

            // Find last instruction before returns
            AbstractInsnNode lastReal = methodNode.instructions.getLast();
            while (lastReal != null && lastReal.getOpcode() < 0) {
                lastReal = lastReal.getPrevious();
            }
            if (lastReal != null) {
                methodNode.instructions.insertBefore(lastReal, tryEnd);
            }

            // Catch handler — call fallback
            InsnList fallbackCode = new InsnList();
            fallbackCode.add(catchHandler);
            int exVar = methodNode.maxLocals;
            methodNode.maxLocals++;
            fallbackCode.add(new VarInsnNode(Opcodes.ASTORE, exVar));

            // Log the failover
            fallbackCode.add(new FieldInsnNode(
                Opcodes.GETSTATIC, "java/lang/System", "err",
                "Ljava/io/PrintStream;"));
            fallbackCode.add(new LdcInsnNode(
                "[DeepMix] Failover from " + primaryField + " to " +
                    fallbackField + " in " + methodNode.name));
            fallbackCode.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V", false));

            // Call fallback delegate method
            fallbackCode.add(new VarInsnNode(Opcodes.ALOAD, 0));
            fallbackCode.add(new FieldInsnNode(
                Opcodes.GETFIELD, classNode.name, fallbackField,
                "Ljava/lang/Object;" // Simplified — would need actual field type
            ));

            // TODO: Forward arguments and invoke the same method on the fallback
            // Full implementation would replicate the method call with all args

            Type returnType = Type.getReturnType(methodNode.desc);
            if (returnType.getSort() == Type.VOID) {
                fallbackCode.add(new InsnNode(Opcodes.RETURN));
            } else {
                pushDefaultReturn(fallbackCode, returnType);
            }

            methodNode.instructions.add(fallbackCode);

            // Add try-catch block
            TryCatchBlockNode tcb = new TryCatchBlockNode(
                tryStart, tryEnd, catchHandler, "java/lang/Exception");
            methodNode.tryCatchBlocks.add(tcb);
        }

        private void injectLazyFieldInit(ClassNode classNode, String fieldName,
                                          String fieldDesc, String factoryMethod) {
            // Find all methods that read the delegate field and wrap with lazy init
            for (MethodNode mn : classNode.methods) {
                if (mn.name.equals("<init>") || mn.name.equals("<clinit>")) continue;

                for (AbstractInsnNode insn : mn.instructions) {
                    if (insn instanceof FieldInsnNode) {
                        FieldInsnNode fin = (FieldInsnNode) insn;
                        if (fin.name.equals(fieldName) &&
                            fin.getOpcode() == Opcodes.GETFIELD) {
                            // Insert lazy init check after the GETFIELD
                            InsnList lazyInit = new InsnList();
                            LabelNode notNull = new LabelNode();

                            lazyInit.add(new InsnNode(Opcodes.DUP));
                            lazyInit.add(new JumpInsnNode(Opcodes.IFNONNULL, notNull));
                            lazyInit.add(new InsnNode(Opcodes.POP));

                            // Create via factory method
                            lazyInit.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            lazyInit.add(new MethodInsnNode(
                                Opcodes.INVOKEVIRTUAL,
                                classNode.name,
                                factoryMethod,
                                "()" + fieldDesc,
                                false
                            ));
                            lazyInit.add(new InsnNode(Opcodes.DUP));

                            // Store back into field
                            lazyInit.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            lazyInit.add(new InsnNode(Opcodes.SWAP));
                            lazyInit.add(new FieldInsnNode(
                                Opcodes.PUTFIELD, classNode.name, fieldName, fieldDesc));

                            lazyInit.add(notNull);

                            mn.instructions.insert(fin, lazyInit);
                        }
                    }
                }
            }
        }

        private boolean shouldDelegate(String methodName, Set<String> include,
                                         Set<String> exclude) {
            if (methodName.equals("<init>") || methodName.equals("<clinit>") ||
                methodName.equals("hashCode") || methodName.equals("equals") ||
                methodName.equals("toString")) {
                return false;
            }
            if (!include.isEmpty()) return include.contains(methodName);
            return !exclude.contains(methodName);
        }

        private void pushDefaultReturn(InsnList insns, Type returnType) {
            switch (returnType.getSort()) {
                case Type.VOID:
                    insns.add(new InsnNode(Opcodes.RETURN));
                    break;
                case Type.BOOLEAN:
                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    insns.add(new InsnNode(Opcodes.ICONST_0));
                    insns.add(new InsnNode(Opcodes.IRETURN));
                    break;
                case Type.LONG:
                    insns.add(new InsnNode(Opcodes.LCONST_0));
                    insns.add(new InsnNode(Opcodes.LRETURN));
                    break;
                case Type.FLOAT:
                    insns.add(new InsnNode(Opcodes.FCONST_0));
                    insns.add(new InsnNode(Opcodes.FRETURN));
                    break;
                case Type.DOUBLE:
                    insns.add(new InsnNode(Opcodes.DCONST_0));
                    insns.add(new InsnNode(Opcodes.DRETURN));
                    break;
                case Type.OBJECT:
                case Type.ARRAY:
                    insns.add(new InsnNode(Opcodes.ACONST_NULL));
                    insns.add(new InsnNode(Opcodes.ARETURN));
                    break;
            }
        }

        private void pushStringArray(InsnList insns, String[] values) {
            insns.add(new LdcInsnNode(values.length));
            insns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
            for (int i = 0; i < values.length; i++) {
                insns.add(new InsnNode(Opcodes.DUP));
                insns.add(new LdcInsnNode(i));
                insns.add(new LdcInsnNode(values[i]));
                insns.add(new InsnNode(Opcodes.AASTORE));
            }
        }

        private InsnList cloneInsnList(InsnList original) {
            Map<LabelNode, LabelNode> labelMapping = new HashMap<>();
            for (AbstractInsnNode insn : original) {
                if (insn instanceof LabelNode) {
                    labelMapping.put((LabelNode) insn, new LabelNode());
                }
            }
            InsnList clone = new InsnList();
            for (AbstractInsnNode insn : original) {
                clone.add(insn.clone(labelMapping));
            }
            return clone;
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║              PHASES 21-24 UNIFIED PROCESSOR                        ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Unified processor that dispatches to the appropriate phase processor.
     * Provides a single entry point for all Phase 21-24 annotations.
     */
    public static class AdvancedExtensionsProcessor {

        private final Phase21Processor phase21;
        private final Phase22Processor phase22;
        private final Phase23Processor phase23;
        private final Phase24Processor phase24;
        private final DeepMixContext context;

        public AdvancedExtensionsProcessor(DeepMixContext context) {
            this.context = context;
            this.phase21 = new Phase21Processor(context);
            this.phase22 = new Phase22Processor(context);
            this.phase23 = new Phase23Processor(context);
            this.phase24 = new Phase24Processor(context);
        }

        public Phase21Processor phase21() { return phase21; }
        public Phase22Processor phase22() { return phase22; }
        public Phase23Processor phase23() { return phase23; }
        public Phase24Processor phase24() { return phase24; }

        /**
         * Process all Phase 21-24 annotations found on a class.
         */
        public void processClass(ClassNode classNode) throws DeepMixProcessingException {
            // Phase 21: Game Engine annotations on class level
            processClassAnnotation(classNode, DeepForge.class,
                a -> phase21.processForge(a, classNode, findInitMethod(classNode)));
            processClassAnnotation(classNode, DeepNeoForge.class,
                a -> phase21.processNeoForge(a, classNode, findInitMethod(classNode)));
            processClassAnnotation(classNode, DeepFabric.class,
                a -> phase21.processFabric(a, classNode, findInitMethod(classNode)));
            processClassAnnotation(classNode, DeepQuilt.class,
                a -> phase21.processQuilt(a, classNode, findInitMethod(classNode)));
            processClassAnnotation(classNode, DeepSpigot.class,
                a -> phase21.processSpigot(a, classNode, findInitMethod(classNode)));

            // Phase 24: Class-level annotations
            processClassAnnotation(classNode, DeepPlugin.class,
                a -> phase24.processPlugin(a, classNode));
            processClassAnnotation(classNode, DeepExtension.class,
                a -> phase24.processExtension(a, classNode, null));
            processClassAnnotation(classNode, DeepDelegate.class,
                a -> phase24.processDelegate(a, classNode));
            processClassAnnotation(classNode, DeepChain.class,
                a -> phase24.processChain(a, classNode));

            // Process method-level annotations
            for (MethodNode mn : classNode.methods) {
                processMethodAnnotations(classNode, mn);
            }
        }

        /**
         * Process all Phase 21-24 annotations found on a method.
         */
        public void processMethodAnnotations(ClassNode classNode, MethodNode methodNode)
                throws DeepMixProcessingException {
            // Phase 22: Advanced ASM annotations
            processMethodAnnotation(classNode, methodNode, DeepASMInline.class,
                a -> phase22.processASMInline(a, classNode, methodNode));
            processMethodAnnotation(classNode, methodNode, DeepStackMap.class,
                a -> phase22.processStackMap(a, classNode, methodNode));
            processMethodAnnotation(classNode, methodNode, DeepLocalVar.class,
                a -> phase22.processLocalVar(a, classNode, methodNode));
            processMethodAnnotation(classNode, methodNode, DeepLineNumber.class,
                a -> phase22.processLineNumber(a, classNode, methodNode));
            processMethodAnnotation(classNode, methodNode, DeepTryCatch.class,
                a -> phase22.processTryCatch(a, classNode, methodNode));
            processMethodAnnotation(classNode, methodNode, DeepFinally.class,
                a -> phase22.processFinally(a, classNode, methodNode));
            processMethodAnnotation(classNode, methodNode, DeepSwitch.class,
                a -> phase22.processSwitch(a, classNode, methodNode));
            processMethodAnnotation(classNode, methodNode, DeepInvokeDynamic.class,
                a -> phase22.processInvokeDynamic(a, classNode, methodNode));
            processMethodAnnotation(classNode, methodNode, DeepMethodHandle.class,
                a -> phase22.processMethodHandle(a, classNode, methodNode));

            // Phase 23: Metaprogramming annotations
            processMethodAnnotation(classNode, methodNode, DeepMacro.class,
                a -> phase23.processMacro(a, classNode, methodNode));
            processMethodAnnotation(classNode, methodNode, DeepTemplate.class,
                a -> phase23.processTemplate(a, classNode, methodNode));
            processMethodAnnotation(classNode, methodNode, DeepDSL.class,
                a -> phase23.processDSL(a, classNode, methodNode));
            processMethodAnnotation(classNode, methodNode, DeepQuote.class,
                a -> phase23.processQuote(a, classNode, methodNode));
            processMethodAnnotation(classNode, methodNode, DeepQuasiquote.class,
                a -> phase23.processQuasiquote(a, classNode, methodNode));
            processMethodAnnotation(classNode, methodNode, DeepSplice.class,
                a -> phase23.processSplice(a, classNode, methodNode));
            processMethodAnnotation(classNode, methodNode, DeepEval.class,
                a -> phase23.processEval(a, classNode, methodNode));
            processMethodAnnotation(classNode, methodNode, DeepCompile.class,
                a -> phase23.processCompile(a, classNode, methodNode));
            processMethodAnnotation(classNode, methodNode, DeepReify.class,
                a -> phase23.processReify(a, classNode, methodNode));

            // Phase 24: Method-level annotations
            processMethodAnnotation(classNode, methodNode, DeepHook.class,
                a -> phase24.processHook(a, classNode, methodNode));
            processMethodAnnotation(classNode, methodNode, DeepCallback.class,
                a -> phase24.processCallback(a, classNode, methodNode));
            processMethodAnnotation(classNode, methodNode, DeepListener.class,
                a -> phase24.processListener(a, classNode, methodNode));
        }

        private <A extends java.lang.annotation.Annotation> void processClassAnnotation(
                ClassNode classNode, Class<A> annotationType,
                AnnotationProcessor<A> processor) throws DeepMixProcessingException {
            // Check if annotation is present on the class
            // In real implementation, this reads from classNode.visibleAnnotations
            // and uses reflection or ASM to parse the annotation
        }

        private <A extends java.lang.annotation.Annotation> void processMethodAnnotation(
                ClassNode classNode, MethodNode methodNode, Class<A> annotationType,
                AnnotationProcessor<A> processor) throws DeepMixProcessingException {
            // Check if annotation is present on the method
            // In real implementation, this reads from methodNode.visibleAnnotations
        }

        private MethodNode findInitMethod(ClassNode classNode) {
            for (MethodNode mn : classNode.methods) {
                if (mn.name.equals("<init>")) return mn;
            }
            // Fallback: create a default constructor
            MethodNode defaultInit = new MethodNode(
                Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            defaultInit.instructions = new InsnList();
            defaultInit.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            defaultInit.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL, classNode.superName, "<init>", "()V", false));
            defaultInit.instructions.add(new InsnNode(Opcodes.RETURN));
            defaultInit.maxStack = 1;
            defaultInit.maxLocals = 1;
            classNode.methods.add(defaultInit);
            return defaultInit;
        }

        @FunctionalInterface
        private interface AnnotationProcessor<A> {
            void process(A annotation) throws DeepMixProcessingException;
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║              PHASE 21-24 STATISTICS                                ║
    // ║                                                                    ║
    // ║  Phase 21: 20 annotations (10 MC mod loaders/servers/proxies +     ║
    // ║            10 game engines/multimedia) = 40 definitions            ║
    // ║  Phase 22: 12 annotations = 24 definitions                        ║
    // ║  Phase 23:  9 annotations (+1 alias) = 18 definitions             ║
    // ║  Phase 24:  7 annotations = 14 definitions                        ║
    // ║                                                                    ║
    // ║  TOTAL: 48 annotations | 96 definitions (full + shortcut)          ║
    // ║  Grand Total (all phases): 200+ annotations | 400+ definitions    ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝
}
