package stellar.snow.astralis.engine.render.minecraft.forge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import stellar.snow.astralis.engine.render.minecraft.common.AstralisRenderEngine;
@Mod("astralis")
    public AstralisForge() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
    }
    
    private void clientSetup(FMLClientSetupEvent event) {
        System.out.println("[Astralis] Loading on Forge");
        AstralisRenderEngine.getInstance().initialize();
    }
}
