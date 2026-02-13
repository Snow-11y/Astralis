package stellar.snow.astralis.mixins.integration.goodopt;

import stellar.snow.astralis.integration.GoodOptimizations.GoodOptimizations;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinGeneralOptimizations - GoodOptimizations integration
 */
@Mixin(Minecraft.class)
public abstract class MixinGeneralOptimizations {
    @Inject(method = "runGameLoop", at = @At("HEAD"))
    private void astralis$onGameLoop(CallbackInfo ci) {
        GoodOptimizations.tick();
    }
}
