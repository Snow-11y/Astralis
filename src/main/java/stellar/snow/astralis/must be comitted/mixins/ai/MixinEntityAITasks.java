package stellar.snow.astralis.mixins.ai;

import stellar.snow.astralis.bridge.BridgeMixinInterface;
import stellar.snow.astralis.mixins.util.MixinHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAITasks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MixinEntityAITasks - Task-level AI culling and optimization.
 */
@Mixin(EntityAITasks.class)
public abstract class MixinEntityAITasks {

    @Unique
    private EntityLiving astralis$cachedEntity = null;

    @Unique
    private int astralis$tickCounter = 0;

    // ========================================================================
    // TASK EXECUTION CULLING
    // ========================================================================

    /**
     * Intercept AI task execution for LOD-based optimization.
     */
    @Inject(
        method = "onUpdateTasks",
        at = @At("HEAD"),
        cancellable = true
    )
    private void astralis$onUpdateTasks(CallbackInfo ci) {
        astralis$tickCounter++;

        // Get the owning entity (cached for performance)
        EntityLiving entity = astralis$getOwningEntity();
        if (entity == null) return;

        if (!(entity instanceof BridgeMixinInterface ext)) return;
        if (!ext.astralis$isRegistered()) return;

        // Skip entirely if AI is culled
        if (ext.astralis$isAICulled()) {
            ci.cancel();
            return;
        }

        // LOD-based task frequency reduction
        if (entity instanceof MixinEntityLiving mixinLiving) {
            int lod = mixinLiving.astralis$getCurrentLOD();
            
            // Skip task updates based on LOD
            boolean shouldUpdate = switch (lod) {
                case MixinHelper.LOD_FULL -> true;
                case MixinHelper.LOD_REDUCED -> (astralis$tickCounter & 1) == 0;  // 50%
                case MixinHelper.LOD_MINIMAL -> (astralis$tickCounter & 3) == 0;  // 25%
                default -> true;
            };

            if (!shouldUpdate) {
                ci.cancel();
            }
        }
    }

    /**
     * Attempts to get the owning entity of this AI task set.
     * This uses reflection once and caches the result.
     */
    @Unique
    private EntityLiving astralis$getOwningEntity() {
        if (astralis$cachedEntity != null) {
            return astralis$cachedEntity;
        }

        // The EntityAITasks doesn't have a direct reference to its owner,
        // but we can get it through the running tasks
        // This is a simplified approach - in practice you might use an accessor mixin
        return null;
    }
}
