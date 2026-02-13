package stellar.snow.astralis.mixins.integration.asto;

import stellar.snow.astralis.integration.Asto.Asto;
import net.minecraft.client.resources.IResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinResourceLoading - Asto dynamic resource loading integration
 * 
 * <h2>Purpose:</h2>
 * Integrates Asto's lazy resource loading and parallel texture/model loading system.
 * Reduces startup time and memory usage through intelligent resource management.
 * 
 * <h2>Optimizations:</h2>
 * <ul>
 *   <li>Parallel texture/model loading using Virtual Threads</li>
 *   <li>Lazy texture atlas stitching (only loads visible resources)</li>
 *   <li>Resource pack caching with change detection</li>
 *   <li>JAR discovery caching for faster mod scanning</li>
 * </ul>
 * 
 * @see stellar.snow.astralis.integration.Asto.Asto
 */
@Mixin(IResourceManager.class)
public interface MixinResourceLoading {
    
    /**
     * Hooks into resource reloading to apply Asto's lazy loading optimizations.
     */
    @Inject(
        method = "reloadResources",
        at = @At("HEAD"),
        remap = false
    )
    default void astralis$onResourceReloadStart(CallbackInfo ci) {
        // Initialize Asto's lazy resource loading system
        Asto.initializeResourceLoading();
    }
}
