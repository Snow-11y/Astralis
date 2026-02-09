package stellar.snow.astralis.mixins.render;

import stellar.snow.astralis.patcher.UniversalPatcher;
import stellar.snow.astralis.patcher.UniversalPatcher.SafePathRegistry;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for GlStateManager - The State Router
 * Routes GL state calls through UniversalPatcher to sync with Vulkan
 * 
 * Uses @Inject with HEAD cancellation instead of @Overwrite for compatibility
 * with other mods (OptiFine, Sodium, etc.)
 * 
 * Priority 9999 ensures we run LAST, after other mods have made their changes.
 * This allows us to capture the final intended state.
 */
@Mixin(value = GlStateManager.class, priority = 9999)
public abstract class MixinGlStateManager {

    // ═══════════════════════════════════════════════════════════════════════════
    // EXTERNAL MOD COMPATIBILITY LAYER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Routes any external modifications through UniversalPatcher.
     * This is called when other mods have modified GlStateManager behavior.
     * 
     * @param operation The GL operation being performed
     * @param args Arguments for the operation
     * @return true if UniversalPatcher handled it, false to continue normal flow
     */
    private static boolean routeExternalModification(SafePathRegistry.Operation operation, int... args) {
        // Check if we're in a state where external mod routing is needed
        if (!UniversalPatcher.isHijackingRendering()) {
            return false; // Let normal OpenGL handle it
        }
        
        // Route through UniversalPatcher which knows how to handle external modifications
        // See: src/main/java/com/example/modid/mixins/MixinUniversalPatcher.java
        try {
            UniversalPatcher.execute(operation, args);
            return true;
        } catch (Exception e) {
            // If routing fails, allow fallback to normal behavior
            return false;
        }
    }
    
    private static boolean routeExternalModificationFloat(SafePathRegistry.Operation operation, float... args) {
        if (!UniversalPatcher.isHijackingRendering()) {
            return false;
        }
        
        try {
            UniversalPatcher.executeFloat(operation, args);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CAPABILITY STATE - Using @Inject for compatibility
    // ═══════════════════════════════════════════════════════════════════════════

    @Inject(method = "enableAlpha", at = @At("HEAD"), cancellable = true)
    private static void onEnableAlpha(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.ENABLE_CAPABILITY, 
                SafePathRegistry.GL_ALPHA_TEST)) {
            // UniversalPatcher handled it (including OpenGL call)
            ci.cancel();
        }
        // If not cancelled, other mods' modifications + vanilla will execute
    }

    @Inject(method = "disableAlpha", at = @At("HEAD"), cancellable = true)
    private static void onDisableAlpha(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.DISABLE_CAPABILITY,
                SafePathRegistry.GL_ALPHA_TEST)) {
            ci.cancel();
        }
    }

    @Inject(method = "enableBlend", at = @At("HEAD"), cancellable = true)
    private static void onEnableBlend(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.ENABLE_CAPABILITY,
                SafePathRegistry.GL_BLEND)) {
            ci.cancel();
        }
    }

    @Inject(method = "disableBlend", at = @At("HEAD"), cancellable = true)
    private static void onDisableBlend(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.DISABLE_CAPABILITY,
                SafePathRegistry.GL_BLEND)) {
            ci.cancel();
        }
    }

    @Inject(method = "enableDepth", at = @At("HEAD"), cancellable = true)
    private static void onEnableDepth(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.ENABLE_CAPABILITY,
                SafePathRegistry.GL_DEPTH_TEST)) {
            ci.cancel();
        }
    }

    @Inject(method = "disableDepth", at = @At("HEAD"), cancellable = true)
    private static void onDisableDepth(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.DISABLE_CAPABILITY,
                SafePathRegistry.GL_DEPTH_TEST)) {
            ci.cancel();
        }
    }

    @Inject(method = "enableCull", at = @At("HEAD"), cancellable = true)
    private static void onEnableCull(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.ENABLE_CAPABILITY,
                SafePathRegistry.GL_CULL_FACE)) {
            ci.cancel();
        }
    }

    @Inject(method = "disableCull", at = @At("HEAD"), cancellable = true)
    private static void onDisableCull(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.DISABLE_CAPABILITY,
                SafePathRegistry.GL_CULL_FACE)) {
            ci.cancel();
        }
    }

    @Inject(method = "enableFog", at = @At("HEAD"), cancellable = true)
    private static void onEnableFog(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.ENABLE_CAPABILITY,
                SafePathRegistry.GL_FOG)) {
            ci.cancel();
        }
    }

    @Inject(method = "disableFog", at = @At("HEAD"), cancellable = true)
    private static void onDisableFog(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.DISABLE_CAPABILITY,
                SafePathRegistry.GL_FOG)) {
            ci.cancel();
        }
    }

    @Inject(method = "enableLighting", at = @At("HEAD"), cancellable = true)
    private static void onEnableLighting(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.ENABLE_CAPABILITY,
                SafePathRegistry.GL_LIGHTING)) {
            ci.cancel();
        }
    }

    @Inject(method = "disableLighting", at = @At("HEAD"), cancellable = true)
    private static void onDisableLighting(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.DISABLE_CAPABILITY,
                SafePathRegistry.GL_LIGHTING)) {
            ci.cancel();
        }
    }

    @Inject(method = "enableTexture2D", at = @At("HEAD"), cancellable = true)
    private static void onEnableTexture2D(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.ENABLE_CAPABILITY,
                SafePathRegistry.GL_TEXTURE_2D)) {
            ci.cancel();
        }
    }

    @Inject(method = "disableTexture2D", at = @At("HEAD"), cancellable = true)
    private static void onDisableTexture2D(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.DISABLE_CAPABILITY,
                SafePathRegistry.GL_TEXTURE_2D)) {
            ci.cancel();
        }
    }

    @Inject(method = "enableColorMaterial", at = @At("HEAD"), cancellable = true)
    private static void onEnableColorMaterial(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.ENABLE_CAPABILITY,
                SafePathRegistry.GL_COLOR_MATERIAL)) {
            ci.cancel();
        }
    }

    @Inject(method = "disableColorMaterial", at = @At("HEAD"), cancellable = true)
    private static void onDisableColorMaterial(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.DISABLE_CAPABILITY,
                SafePathRegistry.GL_COLOR_MATERIAL)) {
            ci.cancel();
        }
    }

    @Inject(method = "enableNormalize", at = @At("HEAD"), cancellable = true)
    private static void onEnableNormalize(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.ENABLE_CAPABILITY,
                SafePathRegistry.GL_NORMALIZE)) {
            ci.cancel();
        }
    }

    @Inject(method = "disableNormalize", at = @At("HEAD"), cancellable = true)
    private static void onDisableNormalize(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.DISABLE_CAPABILITY,
                SafePathRegistry.GL_NORMALIZE)) {
            ci.cancel();
        }
    }

    @Inject(method = "enableRescaleNormal", at = @At("HEAD"), cancellable = true)
    private static void onEnableRescaleNormal(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.ENABLE_CAPABILITY,
                SafePathRegistry.GL_RESCALE_NORMAL)) {
            ci.cancel();
        }
    }

    @Inject(method = "disableRescaleNormal", at = @At("HEAD"), cancellable = true)
    private static void onDisableRescaleNormal(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.DISABLE_CAPABILITY,
                SafePathRegistry.GL_RESCALE_NORMAL)) {
            ci.cancel();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BLEND FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Inject(method = "blendFunc(II)V", at = @At("HEAD"), cancellable = true)
    private static void onBlendFunc(int srcFactor, int dstFactor, CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.BLEND_FUNC, 
                srcFactor, dstFactor)) {
            ci.cancel();
        }
    }

    @Inject(method = "tryBlendFuncSeparate(IIII)V", at = @At("HEAD"), cancellable = true)
    private static void onTryBlendFuncSeparate(int srcFactor, int dstFactor,
                                               int srcFactorAlpha, int dstFactorAlpha, 
                                               CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.BLEND_FUNC_SEPARATE,
                srcFactor, dstFactor, srcFactorAlpha, dstFactorAlpha)) {
            ci.cancel();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DEPTH FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Inject(method = "depthFunc(I)V", at = @At("HEAD"), cancellable = true)
    private static void onDepthFunc(int depthFunc, CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.DEPTH_FUNC, depthFunc)) {
            ci.cancel();
        }
    }

    @Inject(method = "depthMask(Z)V", at = @At("HEAD"), cancellable = true)
    private static void onDepthMask(boolean flagIn, CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.DEPTH_MASK, flagIn ? 1 : 0)) {
            ci.cancel();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COLOR OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Inject(method = "colorMask(ZZZZ)V", at = @At("HEAD"), cancellable = true)
    private static void onColorMask(boolean red, boolean green, boolean blue, 
                                    boolean alpha, CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.COLOR_MASK,
                red ? 1 : 0, green ? 1 : 0, blue ? 1 : 0, alpha ? 1 : 0)) {
            ci.cancel();
        }
    }

    @Inject(method = "clearColor(FFFF)V", at = @At("HEAD"), cancellable = true)
    private static void onClearColor(float red, float green, float blue, 
                                     float alpha, CallbackInfo ci) {
        if (routeExternalModificationFloat(SafePathRegistry.Operation.CLEAR_COLOR,
                red, green, blue, alpha)) {
            ci.cancel();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEXTURE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Inject(method = "setActiveTexture(I)V", at = @At("HEAD"), cancellable = true)
    private static void onSetActiveTexture(int texture, CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.ACTIVE_TEXTURE, texture)) {
            ci.cancel();
        }
    }

    @Inject(method = "bindTexture(I)V", at = @At("HEAD"), cancellable = true)
    private static void onBindTexture(int texture, CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.BIND_TEXTURE,
                0x0DE1, // GL_TEXTURE_2D
                texture)) {
            ci.cancel();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VIEWPORT/SCISSOR
    // ═══════════════════════════════════════════════════════════════════════════

    @Inject(method = "viewport(IIII)V", at = @At("HEAD"), cancellable = true)
    private static void onViewport(int x, int y, int width, int height, CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.VIEWPORT, 
                x, y, width, height)) {
            ci.cancel();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // POLYGON OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Inject(method = "cullFace(Lnet/minecraft/client/renderer/GlStateManager$CullFace;)V", 
            at = @At("HEAD"), cancellable = true)
    private static void onCullFace(GlStateManager.CullFace cullFace, CallbackInfo ci) {
        // Convert CullFace enum to GL constant
        int glMode = switch (cullFace) {
            case BACK -> 0x0405;  // GL_BACK
            case FRONT -> 0x0404; // GL_FRONT
            case FRONT_AND_BACK -> 0x0408; // GL_FRONT_AND_BACK
        };
        
        if (routeExternalModification(SafePathRegistry.Operation.CULL_FACE, glMode)) {
            ci.cancel();
        }
    }

    @Inject(method = "doPolygonOffset(FF)V", at = @At("HEAD"), cancellable = true)
    private static void onDoPolygonOffset(float factor, float units, CallbackInfo ci) {
        if (routeExternalModificationFloat(SafePathRegistry.Operation.POLYGON_OFFSET, 
                factor, units)) {
            ci.cancel();
        }
    }

    @Inject(method = "enablePolygonOffset", at = @At("HEAD"), cancellable = true)
    private static void onEnablePolygonOffset(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.ENABLE_CAPABILITY,
                SafePathRegistry.GL_POLYGON_OFFSET_FILL)) {
            ci.cancel();
        }
    }

    @Inject(method = "disablePolygonOffset", at = @At("HEAD"), cancellable = true)
    private static void onDisablePolygonOffset(CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.DISABLE_CAPABILITY,
                SafePathRegistry.GL_POLYGON_OFFSET_FILL)) {
            ci.cancel();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CLEAR OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Inject(method = "clear(I)V", at = @At("HEAD"), cancellable = true)
    private static void onClear(int mask, CallbackInfo ci) {
        if (routeExternalModification(SafePathRegistry.Operation.CLEAR, mask)) {
            ci.cancel();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MATRIX OPERATIONS - Sync with UniversalPatcher matrix stack
    // ═══════════════════════════════════════════════════════════════════════════

    @Inject(method = "pushMatrix", at = @At("HEAD"))
    private static void onPushMatrix(CallbackInfo ci) {
        if (UniversalPatcher.isHijackingRendering()) {
            UniversalPatcher.pushModelViewMatrix();
        }
    }

    @Inject(method = "popMatrix", at = @At("HEAD"))
    private static void onPopMatrix(CallbackInfo ci) {
        if (UniversalPatcher.isHijackingRendering()) {
            UniversalPatcher.popModelViewMatrix();
        }
    }

    @Inject(method = "loadIdentity", at = @At("HEAD"))
    private static void onLoadIdentity(CallbackInfo ci) {
        if (UniversalPatcher.isHijackingRendering()) {
            UniversalPatcher.loadIdentity(false); // ModelView
        }
    }

    @Inject(method = "translate(FFF)V", at = @At("HEAD"))
    private static void onTranslateF(float x, float y, float z, CallbackInfo ci) {
        if (UniversalPatcher.isHijackingRendering()) {
            UniversalPatcher.translate(x, y, z);
        }
    }

    @Inject(method = "translate(DDD)V", at = @At("HEAD"))
    private static void onTranslateD(double x, double y, double z, CallbackInfo ci) {
        if (UniversalPatcher.isHijackingRendering()) {
            UniversalPatcher.translate((float) x, (float) y, (float) z);
        }
    }

    @Inject(method = "rotate(FFFF)V", at = @At("HEAD"))
    private static void onRotate(float angle, float x, float y, float z, CallbackInfo ci) {
        if (UniversalPatcher.isHijackingRendering()) {
            UniversalPatcher.rotate(angle, x, y, z);
        }
    }

    @Inject(method = "scale(FFF)V", at = @At("HEAD"))
    private static void onScaleF(float x, float y, float z, CallbackInfo ci) {
        if (UniversalPatcher.isHijackingRendering()) {
            UniversalPatcher.scale(x, y, z);
        }
    }

    @Inject(method = "scale(DDD)V", at = @At("HEAD"))
    private static void onScaleD(double x, double y, double z, CallbackInfo ci) {
        if (UniversalPatcher.isHijackingRendering()) {
            UniversalPatcher.scale((float) x, (float) y, (float) z);
        }
    }

    @Inject(method = "ortho(DDDDDD)V", at = @At("HEAD"))
    private static void onOrtho(double left, double right, double bottom, 
                                double top, double zNear, double zFar, CallbackInfo ci) {
        if (UniversalPatcher.isHijackingRendering()) {
            UniversalPatcher.ortho((float) left, (float) right, (float) bottom,
                (float) top, (float) zNear, (float) zFar);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXTERNAL MOD DETECTION
    // Detects when other mods have modified GlStateManager and routes accordingly
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Called at TAIL to detect if external mods modified the state after us.
     * Routes any detected changes to UniversalPatcher.
     */
    @Inject(method = "enableBlend", at = @At("TAIL"))
    private static void onEnableBlendPost(CallbackInfo ci) {
        // If we didn't cancel (meaning another mod might have modified),
        // ensure UniversalPatcher knows the final state
        if (UniversalPatcher.isHijackingRendering()) {
            UniversalPatcher.notifyExternalStateChange(
                SafePathRegistry.Operation.ENABLE_CAPABILITY,
                SafePathRegistry.GL_BLEND
            );
        }
    }
    
    @Inject(method = "disableBlend", at = @At("TAIL"))
    private static void onDisableBlendPost(CallbackInfo ci) {
        if (UniversalPatcher.isHijackingRendering()) {
            UniversalPatcher.notifyExternalStateChange(
                SafePathRegistry.Operation.DISABLE_CAPABILITY,
                SafePathRegistry.GL_BLEND
            );
        }
    }
    
    // Add more TAIL injections for other critical state methods as needed
}
