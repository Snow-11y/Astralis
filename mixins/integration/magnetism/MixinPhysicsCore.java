package stellar.snow.astralis.mixins.integration.magnetism;

import stellar.snow.astralis.integration.MagnetismCore.MagnetismCore;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinPhysicsCore - MagnetismCore physics integration
 */
@Mixin(Entity.class)
public abstract class MixinPhysicsCore {
    @Inject(method = "move", at = @At("HEAD"))
    private void astralis$applyMagnetism(CallbackInfo ci) {
        MagnetismCore.applyPhysicsForces((Entity)(Object)this);
    }
}
