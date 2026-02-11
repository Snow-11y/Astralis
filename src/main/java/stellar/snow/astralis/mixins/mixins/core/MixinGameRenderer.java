package stellar.snow.astralis.mixins.core;

import stellar.snow.astralis.bridge.RenderBridge;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinGameRenderer - Hooks into frame rendering for Vulkan synchronization.
 */
@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    /**
     * Begin Vulkan frame before rendering starts.
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderBegin(float partialTicks, long nanoTime, boolean renderWorldIn, CallbackInfo ci) {
        RenderBridge bridge = RenderBridge.getInstance();
        if (bridge.isVulkan()) {
            if (!bridge.beginFrame()) {
                // Swapchain recreation in progress, skip this frame
                ci.cancel();
            }
        }
    }

    /**
     * End Vulkan frame after rendering completes.
     */
    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderEnd(float partialTicks, long nanoTime, boolean renderWorldIn, CallbackInfo ci) {
        RenderBridge bridge = RenderBridge.getInstance();
        if (bridge.isVulkan()) {
            bridge.endFrame();
        }
    }
}
