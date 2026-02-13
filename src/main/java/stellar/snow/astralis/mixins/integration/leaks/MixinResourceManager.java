package stellar.snow.astralis.mixins.integration.leaks;

import stellar.snow.astralis.integration.AllTheLeaksReborn.AllTheLeaksReborn;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinResourceManager - AllTheLeaksReborn resource leak tracking
 */
@Mixin(SimpleReloadableResourceManager.class)
public abstract class MixinResourceManager {
    @Inject(method = "reloadResources", at = @At("RETURN"))
    private void astralis$trackResourceLeaks(CallbackInfo ci) {
        AllTheLeaksReborn.checkResourceLeaks();
    }
}
