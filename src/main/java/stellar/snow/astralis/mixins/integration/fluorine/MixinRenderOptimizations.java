package stellar.snow.astralis.mixins.integration.fluorine;

import stellar.snow.astralis.integration.Fluorine.Fluorine;
import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinRenderOptimizations - Fluorine rendering optimization integration
 */
@Mixin(RenderGlobal.class)
public abstract class MixinRenderOptimizations {
    @Inject(method = "renderEntities", at = @At("HEAD"))
    private void astralis$onRenderEntities(CallbackInfo ci) {
        Fluorine.beginEntityRenderOptimizations();
    }
}
