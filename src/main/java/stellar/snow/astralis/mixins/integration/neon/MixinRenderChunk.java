package stellar.snow.astralis.mixins.integration.neon;

import stellar.snow.astralis.integration.Neon.Neon;
import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinRenderChunk - Neon render chunk optimization integration
 */
@Mixin(RenderChunk.class)
public abstract class MixinRenderChunk {
    @Inject(method = "deleteGlResources", at = @At("HEAD"))
    private void astralis$cleanupNeonResources(CallbackInfo ci) {
        Neon.cleanupChunkResources((RenderChunk)(Object)this);
    }
}
