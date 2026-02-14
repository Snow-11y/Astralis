package stellar.snow.astralis.engine.render.minecraft.neoforge;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
/**
 * NeoForge-specific event hooks for Astralis rendering
 */
@Mod.EventBusSubscriber
    
    @SubscribeEvent
    public static void onRenderLevelStage(Object event) {
        // Handle render stage events
    }
    
    @SubscribeEvent
    public static void onClientTick(Object event) {
        // Client tick update
    }
}
