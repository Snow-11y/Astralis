package stellar.snow.astralis.mixins.integration.jit;

import stellar.snow.astralis.integration.jit.JITInject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MixinBytecodeOptimization - JIT bytecode optimization integration
 */
@Mixin(targets = "net.minecraft.launchwrapper.LaunchClassLoader", remap = false)
public abstract class MixinBytecodeOptimization {
    @Inject(method = "findClass", at = @At("RETURN"), remap = false)
    private void astralis$onClassLoad(String name, CallbackInfoReturnable<Class<?>> cir) {
        JITInject.onClassLoaded(name, cir.getReturnValue());
    }
}
