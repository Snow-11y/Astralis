package stellar.snow.astralis.mixins.integration.snowyasm;

import stellar.snow.astralis.integration.SnowyASM.SnowyASM;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MixinStringDeduplication - SnowyASM string optimization integration
 */
@Mixin(String.class)
public abstract class MixinStringDeduplication {
    @Inject(method = "intern", at = @At("RETURN"), remap = false)
    private void astralis$deduplicateString(CallbackInfoReturnable<String> cir) {
        SnowyASM.deduplicateString(cir.getReturnValue());
    }
}
