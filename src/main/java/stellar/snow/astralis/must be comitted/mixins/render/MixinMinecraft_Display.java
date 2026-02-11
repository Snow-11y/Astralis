package stellar.snow.astralis.mixins.render;

import stellar.snow.astralis.patcher.UniversalPatcher;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Minecraft main class - Display/Frame management
 * Prevents double buffer swap and manages frame lifecycle
 */
@Mixin(Minecraft.class)
public class MixinMinecraft_Display {

    /**
     * Begin frame hook - called at start of game loop
     */
    @Inject(
        method = "runGameLoop",
        at = @At("HEAD")
    )
    private void onRunGameLoopStart(CallbackInfo ci) {
        if (!UniversalPatcher.beginFrame()) {
            // Frame should be skipped (minimized, etc.)
            // The actual skip logic is handled elsewhere
        }
    }

    /**
     * Prevents the "Double Swap" crash.
     * Redirects the Display.update() call in the main loop.
     */
    @Redirect(
        method = "runGameLoop",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/opengl/Display;update()V"
        )
    )
    private void redirectDisplayUpdate() {
        if (UniversalPatcher.shouldSuppressDisplayUpdate()) {
            // Let Vulkan handle the presentation
            UniversalPatcher.handleDisplayUpdate();
        } else {
            // Fallback to vanilla OpenGL swap
            Display.update();
        }
    }

    /**
     * End frame hook - called after rendering completes
     */
    @Inject(
        method = "runGameLoop",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;checkGLError(Ljava/lang/String;)V",
            ordinal = 0
        )
    )
    private void onRunGameLoopEnd(CallbackInfo ci) {
        UniversalPatcher.endFrame();
    }

    /**
     * Handle window resize detection
     */
    @Inject(
        method = "runGameLoop",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;updateDisplay()V"
        )
    )
    private void onPreUpdateDisplay(CallbackInfo ci) {
        UniversalPatcher.checkAndHandleResize();
    }
}
