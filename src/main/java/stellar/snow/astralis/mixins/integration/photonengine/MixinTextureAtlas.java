package stellar.snow.astralis.mixins.integration.photonengine;

import stellar.snow.astralis.integration.PhotonEngine.PhotonEngine;
import net.minecraft.client.renderer.texture.TextureMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinTextureAtlas - PhotonEngine texture optimization integration
 */
@Mixin(TextureMap.class)
public abstract class MixinTextureAtlas {
    @Inject(method = "loadTexture", at = @At("HEAD"))
    private void astralis$photonTextureLoad(CallbackInfo ci) {
        PhotonEngine.optimizeTextureLoading();
    }
}
