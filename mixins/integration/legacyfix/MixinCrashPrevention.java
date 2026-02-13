package stellar.snow.astralis.mixins.integration.legacyfix;

import stellar.snow.astralis.integration.LegacyFix.LegacyFix;
import net.minecraft.crash.CrashReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MixinCrashPrevention - LegacyFix crash prevention integration
 */
@Mixin(CrashReport.class)
public abstract class MixinCrashPrevention {
    @Inject(method = "makeCategoryDepth", at = @At("HEAD"), cancellable = true)
    private void astralis$preventCrashRecursion(int depth, CallbackInfoReturnable<?> cir) {
        if (LegacyFix.shouldPreventCrashRecursion(depth)) {
            cir.cancel();
        }
    }
}
