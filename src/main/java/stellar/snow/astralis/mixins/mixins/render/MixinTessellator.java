package stellar.snow.astralis.mixins.render;

import stellar.snow.astralis.patcher.UniversalPatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

/**
 * Mixin for Tessellator - The Data Bridge
 * Captures immediate mode vertex data for Vulkan rendering
 */
@Mixin(Tessellator.class)
public class MixinTessellator {

    @Shadow
    private BufferBuilder buffer;

    /**
     * Intercepts draw call to capture vertex data for Vulkan
     */
    @Inject(
        method = "draw",
        at = @At("HEAD")
    )
    private void onDrawPre(CallbackInfo ci) {
        if (!UniversalPatcher.isHijackingRendering()) return;
        if (UniversalPatcher.isShuttingDown()) return;

        try {
            // Get vertex data from BufferBuilder
            ByteBuffer data = buffer.getByteBuffer();
            int vertexCount = buffer.getVertexCount();
            VertexFormat format = buffer.getVertexFormat();
            int drawMode = buffer.getDrawMode();

            if (vertexCount > 0 && data != null) {
                // Send to UniversalPatcher for Vulkan upload
                UniversalPatcher.onTessellatorPreDraw(data, vertexCount, drawMode, format);
            }
        } catch (Exception e) {
            // Don't crash the game if tessellator capture fails
            System.err.println("[MixinTessellator] PreDraw capture failed: " + e.getMessage());
        }
    }

    /**
     * Post-draw hook for issuing Vulkan draw commands
     */
    @Inject(
        method = "draw",
        at = @At("TAIL")
    )
    private void onDrawPost(CallbackInfo ci) {
        if (!UniversalPatcher.isHijackingRendering()) return;

        try {
            UniversalPatcher.onTessellatorPostDraw();
        } catch (Exception e) {
            // Ignore post-draw errors
        }
    }
}
