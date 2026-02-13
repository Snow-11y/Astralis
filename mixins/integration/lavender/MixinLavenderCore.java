package stellar.snow.astralis.mixins.integration.lavender;

import stellar.snow.astralis.integration.Lavender.Lavender;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinLavenderCore - Lavender systems integration
 */
@Mixin(Minecraft.class)
public abstract class MixinLavenderCore {
    @Inject(method = "startGame", at = @At("RETURN"))
    private void astralis$initLavender(CallbackInfo ci) {
        Lavender.initialize();
    }
}
