package stellar.snow.astralis.mixins.integration.haku;

import stellar.snow.astralis.integration.Haku.Haku;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinHakuCore - Haku core systems integration
 */
@Mixin(Minecraft.class)
public abstract class MixinHakuCore {
    @Inject(method = "init", at = @At("RETURN"))
    private void astralis$initHaku(CallbackInfo ci) {
        Haku.initialize();
    }
}
