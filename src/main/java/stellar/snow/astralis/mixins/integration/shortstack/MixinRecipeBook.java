package stellar.snow.astralis.mixins.integration.shortstack;

import stellar.snow.astralis.integration.ShortStack.ShortStack;
import net.minecraft.stats.RecipeBook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinRecipeBook - ShortStack recipe book integration
 */
@Mixin(RecipeBook.class)
public abstract class MixinRecipeBook {
    @Inject(method = "unlock", at = @At("HEAD"))
    private void astralis$optimizeRecipeUnlock(CallbackInfo ci) {
        ShortStack.cacheRecipeData();
    }
}
