package stellar.snow.astralis.mixins.integration.chunkmotion;

import stellar.snow.astralis.integration.ChunkMotion.ChunkMotion;
import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinChunkAnimation - ChunkMotion smooth chunk transitions integration
 * 
 * <h2>Purpose:</h2>
 * Integrates ChunkMotion's smooth chunk loading/unloading animations.
 * Provides visual feedback during chunk updates with minimal performance impact.
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li>Smooth chunk fade-in animations</li>
 *   <li>Interpolated chunk position updates</li>
 *   <li>Chunk rebuild visual feedback</li>
 *   <li>GPU-accelerated transitions</li>
 * </ul>
 * 
 * @see stellar.snow.astralis.integration.ChunkMotion.ChunkMotion
 */
@Mixin(RenderChunk.class)
public abstract class MixinChunkAnimation {
    
    /**
     * Hook chunk rebuilding to enable animations.
     */
    @Inject(
        method = "setNeedsUpdate",
        at = @At("HEAD")
    )
    private void astralis$onChunkUpdate(boolean playerUpdate, CallbackInfo ci) {
        // Trigger ChunkMotion animation system
        ChunkMotion.onChunkUpdateQueued((RenderChunk)(Object)this, playerUpdate);
    }
}
