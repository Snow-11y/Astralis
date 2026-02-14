package stellar.snow.astralis.engine.render.minecraft.mixins;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(WorldRenderer.class)
    
    @Inject(method = "renderShadows", at = @At("HEAD"), cancellable = true)
    private void onRenderShadows(CallbackInfo ci) {
        // Replace vanilla shadows with Astralis advanced shadows
        // ci.cancel();
    }
}
