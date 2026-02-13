package stellar.snow.astralis.mixins.integration.photonengine;

import stellar.snow.astralis.integration.PhotonEngine.PhotonEngine;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MixinFontRenderer - PhotonEngine font rendering integration
 */
@Mixin(FontRenderer.class)
public abstract class MixinFontRenderer {
    @Inject(method = "drawString", at = @At("HEAD"))
    private void astralis$photonFontRender(String text, CallbackInfoReturnable<Integer> cir) {
        PhotonEngine.optimizeFontRendering();
    }
}
