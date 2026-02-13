package stellar.snow.astralis.mixins.integration.leaks;

import stellar.snow.astralis.integration.AllTheLeaksReborn.AllTheLeaksReborn;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinMemoryTracker - AllTheLeaksReborn memory tracking integration
 */
@Mixin(Minecraft.class)
public abstract class MixinMemoryTracker {
    @Inject(method = "freeMemory", at = @At("HEAD"))
    private void astralis$trackMemory(CallbackInfo ci) {
        AllTheLeaksReborn.trackMemoryUsage();
    }
}
