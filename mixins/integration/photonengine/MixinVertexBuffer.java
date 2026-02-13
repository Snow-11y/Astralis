package stellar.snow.astralis.mixins.integration.photonengine;

import stellar.snow.astralis.integration.PhotonEngine.PhotonEngine;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinVertexBuffer - PhotonEngine vertex buffer integration
 */
@Mixin(VertexBuffer.class)
public abstract class MixinVertexBuffer {
    @Inject(method = "bufferData", at = @At("HEAD"))
    private void astralis$optimizeVertexBuffer(CallbackInfo ci) {
        PhotonEngine.optimizeVertexData();
    }
}
