package stellar.snow.astralis.mixins.integration.lumen;

import stellar.snow.astralis.integration.Lumen.Lumen;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinLightingEngine - Lumen advanced lighting integration
 */
@Mixin(WorldProvider.class)
public abstract class MixinLightingEngine {
    @Inject(method = "calculateCelestialAngle", at = @At("RETURN"))
    private void astralis$updateLumenLighting(long worldTime, float partialTicks, CallbackInfo ci) {
        Lumen.updateGlobalLighting((WorldProvider)(Object)this, worldTime, partialTicks);
    }
}
