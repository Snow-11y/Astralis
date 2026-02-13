package stellar.snow.astralis.mixins.integration.jit;

import stellar.snow.astralis.integration.jit.UniversalPatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MixinUniversalPatcher - JIT universal patcher integration
 */
@Mixin(targets = "net.minecraft.launchwrapper.LaunchClassLoader", remap = false)
public abstract class MixinUniversalPatcher {
    @Inject(method = "getClassBytes", at = @At("RETURN"), remap = false)
    private void astralis$patchBytes(String name, CallbackInfoReturnable<byte[]> cir) {
        byte[] patched = UniversalPatcher.patchClass(name, cir.getReturnValue());
        if (patched != null) cir.setReturnValue(patched);
    }
}
