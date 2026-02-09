﻿package stellar.snow.astralis;

import stellar.snow.astralis.api.common.CompatibilityLayer;
import stellar.snow.astralis.nexus.GLOptimizer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = Astralis.MODID,
    name = Astralis.NAME,
    version = Astralis.VERSION,
    acceptableRemoteVersions = "*"
)
public class Astralis {
    public static final String MODID = "astralis";
    public static final String NAME = "Astralis";
    public static final String VERSION = "Winter's-Early-0.1.2";
    
    public static final Logger LOGGER = LogManager.getLogger(NAME);
    
    private static boolean compatibilityMessageShown = false;
    
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Astralis PreInit - Java version: {}", System.getProperty("java.version"));
        
        // Register event handler for client tick
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("Astralis Init - Starting culling engine");
    }
    
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // Initialize GL optimizer after OpenGL context is ready
        LOGGER.info("Astralis PostInit - Initializing GL optimizer");
        GLOptimizer.initialize();
        
        LOGGER.info("Astralis GL Optimizer: {}", GLOptimizer.isEnabled() ? "ENABLED" : "DISABLED");
        
        // Print full report to console
        if (GLOptimizer.isEnabled()) {
            System.out.println(GLOptimizer.getDetailedReport());
        }
    }
    
    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandAstralis());
    }
    
    @Mod.EventHandler
    public void serverStop(FMLServerStoppingEvent event) {
        if (GLOptimizer.isEnabled()) {
            GLOptimizer.printStats();
        }
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // Show compatibility message once when player joins world
        if (event.phase == TickEvent.Phase.END && !compatibilityMessageShown) {
            if (net.minecraft.client.Minecraft.getMinecraft().player != null) {
                CompatibilityLayer.displayInGameMessage();
                compatibilityMessageShown = true;
            }
        }
    }
}
