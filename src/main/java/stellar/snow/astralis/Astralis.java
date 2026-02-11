package stellar.snow.astralis;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stellar.snow.astralis.core.InitializationManager;
import stellar.snow.astralis.commands.CommandAstralis;
import stellar.snow.astralis.integration.DeepMix.DeepMix;
import stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore;

/**
 * Main mod class for Astralis.
 * Delegates initialization to InitializationManager for proper sequencing.
 * 
 * <h2>Initialization Sequence:</h2>
 * <ol>
 *   <li>PRE_INIT: Config, logging, common systems</li>
 *   <li>INIT: ECS World, client/server systems</li>
 *   <li>POST_INIT: UniversalCapabilities, GPU backends</li>
 *   <li>WINDOW_CREATED: Advanced backends (Vulkan/DirectX/Metal)</li>
 * </ol>
 * 
 * @version Winter's-Early-0.1.2
 */
@Mod(
    modid = Astralis.MODID,
    name = Astralis.NAME,
    version = Astralis.VERSION,
    acceptableRemoteVersions = "*",
    guiFactory = "stellar.snow.astralis.client.AstralisGuiFactory"
)
public class Astralis {
    
    // ========================================================================
    // MOD CONSTANTS
    // ========================================================================
    
    public static final String MODID = "astralis";
    public static final String NAME = "Astralis";
    public static final String VERSION = "Winter's-Early-0.1.2";
    
    public static final Logger LOGGER = LogManager.getLogger(NAME);
    
    @Mod.Instance(MODID)
    public static Astralis instance;
    
    // ========================================================================
    // INITIALIZATION EVENT HANDLERS
    // ========================================================================
    
    /**
     * Pre-initialization phase.
     * Sets up configuration, logging, and prepares systems.
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("╔═══════════════════════════════════════════════════════════════╗");
        LOGGER.info("║                    ASTRALIS STARTING                         ║");
        LOGGER.info("║              Modern Graphics for Minecraft 1.12.2            ║");
        LOGGER.info("║      Vulkan • SPIR-V • DirectX • HLSL • OpenGL • GLSL      ║");
        LOGGER.info("║          OpenGL ES • GLSL ES • Metal • MSL • bgfx           ║");
        LOGGER.info("╚═══════════════════════════════════════════════════════════════╝");
        LOGGER.info("[Astralis] DeepMix early loader: active");
        LOGGER.info("[Astralis] Mini_DirtyRoom: " + Mini_DirtyRoomCore.VERSION
                + " | bootstrap=" + (Mini_DirtyRoomCore.isBootstrapComplete() ? "complete" : "pending")
                + " | LWJGL override=" + Mini_DirtyRoomCore.TARGET_LWJGL_VERSION);
        
        try {
            // Delegate to initialization manager
            InitializationManager.preInitialize();
            
            // Register event handlers
            MinecraftForge.EVENT_BUS.register(this);
            
        } catch (Exception e) {
            LOGGER.fatal("Pre-initialization failed!", e);
            throw new RuntimeException("Astralis failed to pre-initialize", e);
        }
    }
    
    /**
     * Main initialization phase.
     * Initializes core systems and ECS world.
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        try {
            // Delegate to initialization manager
            InitializationManager.initialize();
            
        } catch (Exception e) {
            LOGGER.fatal("Initialization failed!", e);
            throw new RuntimeException("Astralis failed to initialize", e);
        }
    }
    
    /**
     * Post-initialization phase.
     * Finalizes setup, initializes GPU backend and rendering systems.
     * This runs after all mods are loaded and OpenGL context is available.
     */
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        try {
            // Delegate to initialization manager
            // GPU backend initialization happens here (needs OpenGL context)
            InitializationManager.postInitialize();
            
        } catch (Exception e) {
            LOGGER.fatal("Post-initialization failed!", e);
            throw new RuntimeException("Astralis failed to post-initialize", e);
        }
    }
    
    /**
     * Server starting - register commands.
     */
    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent event) {
        try {
            event.registerServerCommand(new CommandAstralis());
            LOGGER.info("Astralis server commands registered");
        } catch (Exception e) {
            LOGGER.error("Failed to register server commands", e);
        }
    }
    
    /**
     * Server stopping - cleanup and statistics.
     */
    @Mod.EventHandler
    public void serverStop(FMLServerStoppingEvent event) {
        LOGGER.info("Astralis shutting down...");
        try {
            InitializationManager.shutdown();
        } catch (Exception e) {
            LOGGER.error("Error during shutdown", e);
        }
    }
    
    // ========================================================================
    // RUNTIME EVENT HANDLERS
    // ========================================================================
    
    private boolean firstTickComplete = false;
    
    /**
     * Client tick handler for runtime updates.
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // First tick initialization (after world is fully loaded)
        if (!firstTickComplete) {
            if (net.minecraft.client.Minecraft.getMinecraft().player != null) {
                onFirstClientTick();
                firstTickComplete = true;
            }
        }
    }
    
    /**
     * Called on first client tick when player is available.
     * Use for displaying messages or doing post-world-load initialization.
     */
    private void onFirstClientTick() {
        try {
            // Display compatibility or welcome message if needed
            displayWelcomeMessage();
        } catch (Exception e) {
            LOGGER.error("Error during first tick initialization", e);
        }
    }
    
    /**
     * Display welcome message to player.
     */
    private void displayWelcomeMessage() {
        // Only show if initialization was successful
        if (InitializationManager.isInitialized()) {
            String backend = "OpenGL";
            if (InitializationManager.isVulkanActive()) {
                backend = "Vulkan";
            } else if (InitializationManager.isDirectXActive()) {
                backend = "DirectX";
            } else if (InitializationManager.isOpenGLESActive()) {
                backend = "OpenGL ES";
            }
            
            LOGGER.info("Astralis is ready! {} backend active.", backend);
            LOGGER.info("DeepMix mixin loader: active | config=mixins.astralis.json");
            LOGGER.info("Mini_DirtyRoom: v{} | env={} | Java={} | LWJGL→{}",
                    Mini_DirtyRoomCore.VERSION,
                    Mini_DirtyRoomCore.getEnvironment() != null
                        ? Mini_DirtyRoomCore.getEnvironment().detectedLoader : "unknown",
                    Mini_DirtyRoomCore.getEnvironment() != null
                        ? Mini_DirtyRoomCore.getEnvironment().javaVersion : "?",
                    Mini_DirtyRoomCore.TARGET_LWJGL_VERSION);
            
            if (InitializationManager.isVulkanActive()) {
                LOGGER.info("Vulkan device: {}", 
                    InitializationManager.getVulkanBackend().getDeviceName());
            } else if (InitializationManager.isDirectXActive()) {
                LOGGER.info("DirectX API: {}", 
                    InitializationManager.getDirectXManager().getCurrentAPI().displayName);
            } else if (InitializationManager.isOpenGLESActive()) {
                LOGGER.info("OpenGL ES version: {}.{}", 
                    stellar.snow.astralis.engine.gpu.authority.UniversalCapabilities.GLES.majorVersion,
                    stellar.snow.astralis.engine.gpu.authority.UniversalCapabilities.GLES.minorVersion);
            }
        } else {
            LOGGER.warn("Astralis loaded but some systems failed to initialize");
        }
    }
    
    // ========================================================================
    // PUBLIC API
    // ========================================================================
    
    /**
     * Check if Astralis is fully initialized and ready.
     */
    public static boolean isReady() {
        return InitializationManager.isInitialized();
    }
    
    /**
     * Get current initialization phase.
     */
    public static InitializationManager.InitializationPhase getPhase() {
        return InitializationManager.getCurrentPhase();
    }
    
    /**
     * Check if Vulkan backend is active.
     */
    public static boolean isVulkanActive() {
        return InitializationManager.isVulkanActive();
    }
    
    /**
     * Check if DirectX backend is active.
     */
    public static boolean isDirectXActive() {
        return InitializationManager.isDirectXActive();
    }
    
    /**
     * Check if OpenGL ES backend is active.
     */
    public static boolean isOpenGLESActive() {
        return InitializationManager.isOpenGLESActive