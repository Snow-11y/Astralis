package stellar.snow.astralis.engine.render.minecraft.fabric;
import net.fabricmc.api.ClientModInitializer;
import stellar.snow.astralis.engine.render.minecraft.common.AstralisRenderEngine;
    @Override
    public void onInitializeClient() {
        System.out.println("[Astralis] Loading on Fabric");
        AstralisRenderEngine.getInstance().initialize();
    }
}
