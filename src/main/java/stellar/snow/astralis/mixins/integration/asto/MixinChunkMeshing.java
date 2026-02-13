package stellar.snow.astralis.mixins.integration.asto;

import stellar.snow.astralis.integration.Asto.Asto;
import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinChunkMeshing - Asto chunk meshing optimization integration
 * 
 * <h2>Purpose:</h2>
 * Integrates Asto's parallel chunk meshing and mesh caching optimizations.
 * Hooks into chunk rebuild events to leverage Asto's performance improvements.
 * 
 * <h2>Optimizations:</h2>
 * <ul>
 *   <li>Parallel chunk mesh building via Virtual Threads</li>
 *   <li>Mesh result caching and deduplication</li>
 *   <li>Fast chunk lookup using off-heap hash tables</li>
 *   <li>View frustum culling integration</li>
 * </ul>
 * 
 * @see stellar.snow.astralis.integration.Asto.Asto
 */
@Mixin(RenderChunk.class)
public abstract class MixinChunkMeshing {
    
    /**
     * Hooks into chunk rebuilding to apply Asto optimizations.
     * Enables parallel meshing and mesh caching.
     */
    @Inject(
        method = "rebuildChunk",
        at = @At("HEAD")
    )
    private void astralis$onRebuildChunkHead(float x, float y, float z, CallbackInfo ci) {
        // Hook for Asto chunk meshing optimization initialization
        Asto.onChunkMeshingStart((RenderChunk)(Object)this);
    }
    
    /**
     * Post-rebuild hook for cleanup and metrics collection.
     */
    @Inject(
        method = "rebuildChunk",
        at = @At("RETURN")
    )
    private void astralis$onRebuildChunkReturn(float x, float y, float z, CallbackInfo ci) {
        // Hook for Asto chunk meshing finalization
        Asto.onChunkMeshingComplete((RenderChunk)(Object)this);
    }
}
