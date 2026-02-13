package stellar.snow.astralis.mixins.integration.legacyfix;

import stellar.snow.astralis.integration.LegacyFix.LegacyFix;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinCompatibilityFixes - LegacyFix compatibility layer integration
 */
@Mixin(Minecraft.class)
public abstract class MixinCompatibilityFixes {
    @Inject(method = "init", at = @At("RETURN"))
    private void astralis$applyLegacyFixes(CallbackInfo ci) {
        LegacyFix.applyCompatibilityFixes();
    }
}
