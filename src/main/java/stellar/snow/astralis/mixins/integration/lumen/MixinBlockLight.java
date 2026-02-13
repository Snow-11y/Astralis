package stellar.snow.astralis.mixins.integration.lumen;

import stellar.snow.astralis.integration.Lumen.Lumen;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MixinBlockLight - Lumen lighting engine integration
 */
@Mixin(World.class)
public abstract class MixinBlockLight {
    @Inject(method = "getLightFromNeighbors", at = @At("HEAD"), cancellable = true)
    private void astralis$getLumenLight(CallbackInfoReturnable<Integer> cir) {
        int lumenLight = Lumen.calculateBlockLight((World)(Object)this);
        if (lumenLight >= 0) cir.setReturnValue(lumenLight);
    }
}
