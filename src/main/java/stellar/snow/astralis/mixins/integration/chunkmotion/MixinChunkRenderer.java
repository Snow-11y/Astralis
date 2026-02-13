package stellar.snow.astralis.mixins.integration.chunkmotion;

import stellar.snow.astralis.integration.ChunkMotion.ChunkMotion;
import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinChunkRenderer - ChunkMotion render pipeline integration
 */
@Mixin(RenderGlobal.class)
public abstract class MixinChunkRenderer {
    @Inject(method = "setupTerrain", at = @At("RETURN"))
    private void astralis$onTerrainSetup(CallbackInfo ci) {
        ChunkMotion.updateAnimations();
    }
}
