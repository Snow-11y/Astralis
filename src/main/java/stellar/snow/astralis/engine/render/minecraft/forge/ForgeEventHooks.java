package stellar.snow.astralis.engine.render.minecraft.forge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/**
 * Forge-specific event hooks for Astralis rendering
 */
@Mod.EventBusSubscriber
    
    @SubscribeEvent
    public static void onRenderLevelStage(Object event) {
        // Handle render stage events
    }
    
    @SubscribeEvent
    public static void onRenderTick(Object event) {
        // Per-frame update
    }
}
