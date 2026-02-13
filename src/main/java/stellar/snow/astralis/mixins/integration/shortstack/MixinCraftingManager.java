package stellar.snow.astralis.mixins.integration.shortstack;

import stellar.snow.astralis.integration.ShortStack.ShortStack;
import net.minecraft.item.crafting.CraftingManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MixinCraftingManager - ShortStack crafting optimization integration
 */
@Mixin(CraftingManager.class)
public abstract class MixinCraftingManager {
    @Inject(method = "findMatchingRecipe", at = @At("HEAD"))
    private void astralis$optimizeRecipeLookup(CallbackInfoReturnable<?> cir) {
        ShortStack.optimizeCraftingLookup();
    }
}
