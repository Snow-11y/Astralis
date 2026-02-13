package stellar.snow.astralis.mixins.integration.snowyasm;

import stellar.snow.astralis.integration.SnowyASM.SnowyASM;
import net.minecraft.util.ObjectIntIdentityMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinZeroAllocation - SnowyASM zero-allocation optimization integration
 */
@Mixin(ObjectIntIdentityMap.class)
public abstract class MixinZeroAllocation {
    @Inject(method = "put", at = @At("HEAD"))
    private void astralis$optimizeAllocation(Object key, int value, CallbackInfo ci) {
        SnowyASM.applyZeroAllocationOptimization();
    }
}
