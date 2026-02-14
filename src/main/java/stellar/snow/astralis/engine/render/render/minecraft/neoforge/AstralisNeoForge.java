package stellar.snow.astralis.engine.render.minecraft.neoforge;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.bus.api.IEventBus;
import stellar.snow.astralis.engine.render.minecraft.common.AstralisRenderEngine;

@Mod("astralis")
public class AstralisNeoForge {
    public AstralisNeoForge(IEventBus modEventBus) {
        modEventBus.addListener(this::clientSetup);
    }
    
    private void clientSetup(FMLClientSetupEvent event) {
        System.out.println("[Astralis] Loading on NeoForge");
        AstralisRenderEngine.getInstance().initialize();
    }
}
