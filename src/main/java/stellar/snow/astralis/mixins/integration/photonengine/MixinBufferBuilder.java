package stellar.snow.astralis.mixins.integration.photonengine;

import stellar.snow.astralis.integration.PhotonEngine.PhotonEngine;
import net.minecraft.client.renderer.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinBufferBuilder - PhotonEngine buffer optimization integration
 */
@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder {
    @Inject(method = "finishDrawing", at = @At("HEAD"))
    private void astralis$optimizeBuffer(CallbackInfo ci) {
        PhotonEngine.optimizeBufferUsage((BufferBuilder)(Object)this);
    }
}
