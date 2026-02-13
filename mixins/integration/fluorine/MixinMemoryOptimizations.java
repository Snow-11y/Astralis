package stellar.snow.astralis.mixins.integration.fluorine;

import stellar.snow.astralis.integration.Fluorine.Fluorine;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinMemoryOptimizations - Fluorine memory management integration
 */
@Mixin(Chunk.class)
public abstract class MixinMemoryOptimizations {
    @Inject(method = "onLoad", at = @At("RETURN"))
    private void astralis$onChunkLoad(CallbackInfo ci) {
        Fluorine.optimizeChunkMemory((Chunk)(Object)this);
    }
}
