package stellar.snow.astralis.mixins.integration.neon;

import stellar.snow.astralis.integration.Neon.Neon;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinChunkRenderDispatcher - Neon chunk rendering integration
 */
@Mixin(ChunkRenderDispatcher.class)
public abstract class MixinChunkRenderDispatcher {
    @Inject(method = "uploadChunk", at = @At("HEAD"))
    private void astralis$neonChunkUpload(CallbackInfo ci) {
        Neon.optimizeChunkUpload();
    }
}
