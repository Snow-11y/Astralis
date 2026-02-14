package stellar.snow.astralis.engine.render.minecraft.mixins;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import stellar.snow.astralis.engine.render.minecraft.common.AstralisRenderEngine;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        AstralisRenderEngine.getInstance().initialize();
    }
    
    @WrapOperation(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/render/Camera;)V")
    )
    private void wrapWorldRender(Operation<Void> original) {
        // Apply Astralis rendering before/after world render
        AstralisRenderEngine engine = AstralisRenderEngine.getInstance();
        if (engine.isInitialized()) {
            // Pre-render hook
        }
        
        original.call();
        
        if (engine.isInitialized()) {
            // Post-render hook (apply post-processing)
        }
    }
}
