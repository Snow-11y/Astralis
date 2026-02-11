package stellar.snow.astralis.mixins.ai;

import stellar.snow.astralis.bridge.BridgeComponents;
import stellar.snow.astralis.bridge.BridgeMixinInterface;
import stellar.snow.astralis.bridge.MinecraftECSBridge;
import stellar.snow.astralis.mixins.core.MixinEntity;
import stellar.snow.astralis.mixins.util.MixinHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * MixinEntityLiving - Advanced AI culling and LOD management.
 *
 * <h2>Culling Strategies:</h2>
 * <ul>
 *   <li>Distance-based LOD with configurable thresholds</li>
 *   <li>Frustum culling for off-screen entities</li>
 *   <li>Priority-based culling for important entities</li>
 *   <li>Tick-skipping for distant entities</li>
 * </ul>
 */
@Mixin(EntityLiving.class)
public abstract class MixinEntityLiving extends EntityLivingBase implements BridgeMixinInterface {

    // Required constructor for mixin
    protected MixinEntityLiving(World worldIn) {
        super(worldIn);
    }

    // ========================================================================
    // SHADOWS
    // ========================================================================

    @Shadow protected abstract void updateEntityActionState();

    // ========================================================================
    // UNIQUE FIELDS
    // ========================================================================

    @Unique
    private int astralis$currentLOD = MixinHelper.LOD_FULL;

    @Unique
    private long astralis$lastAITick = 0;

    @Unique
    private boolean astralis$aiSkippedLastTick = false;

    // ========================================================================
    // AI CULLING
    // ========================================================================

    /**
     * Main AI culling hook - intercepts entity action state updates.
     */
    @Inject(
        method = "updateEntityActionState",
        at = @At("HEAD"),
        cancellable = true
    )
    private void astralis$onUpdateEntityActionState(CallbackInfo ci) {
        // Skip culling for unregistered entities
        if (!astralis$isRegistered()) return;

        // Never cull the player
        if ((Object) this instanceof EntityPlayer) return;

        // Get bridge and calculate LOD
        MinecraftECSBridge bridge = MixinHelper.getBridge();
        if (bridge == null) return;

        long currentTick = bridge.getTickCount();

        // Calculate LOD based on distance to viewer
        EntityPlayer viewer = astralis$getViewer();
        if (viewer != null) {
            astralis$currentLOD = astralis$calculateLODInternal(viewer, currentTick);
        }

        // Check if AI should be processed this tick
        boolean shouldProcess = MixinHelper.shouldProcessAI(astralis$currentLOD, currentTick);

        if (!shouldProcess) {
            // Skip AI this tick
            astralis$aiSkippedLastTick = true;
            astralis$setFlag(FLAG_AI_CULLED);
            
            // Update component flags in ECS memory
            astralis$updateCullingFlags(true);
            
            ci.cancel();
            return;
        }

        // AI is processing
        astralis$aiSkippedLastTick = false;
        astralis$clearFlag(FLAG_AI_CULLED);
        astralis$updateCullingFlags(false);
        astralis$lastAITick = currentTick;
    }

    /**
     * Hook into living update for additional optimizations.
     */
    @Inject(
        method = "onLivingUpdate",
        at = @At("HEAD"),
        cancellable = true
    )
    private void astralis$onLivingUpdate(CallbackInfo ci) {
        if (!astralis$isRegistered()) return;

        // If completely culled, skip all living updates
        if (astralis$currentLOD == MixinHelper.LOD_CULLED) {
            // Still need minimal processing to stay alive
            // Just skip heavy computation
            return;
        }

        // For MINIMAL LOD, skip certain expensive operations
        if (astralis$currentLOD == MixinHelper.LOD_MINIMAL) {
            // Could skip pathfinding updates, etc.
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    @Unique
    private EntityPlayer astralis$getViewer() {
        if (this.worldObj.isRemote) {
            Minecraft mc = Minecraft.getMinecraft();
            return mc.thePlayer;
        }
        return null;
    }

    @Unique
    private int astralis$calculateLODInternal(Entity viewer, long currentTick) {
        // Use cached LOD from MixinEntity if available
        if (this instanceof MixinEntity mixinEntity) {
            return mixinEntity.astralis$getLOD(viewer.posX, viewer.posY, viewer.posZ, currentTick);
        }

        // Fallback calculation
        double distSq = MixinHelper.distanceSquared((Entity) (Object) this, viewer);
        return MixinHelper.calculateLOD(distSq);
    }

    @Unique
    private void astralis$updateCullingFlags(boolean aiCulled) {
        int slot = astralis$getBridgeSlot();
        if (slot < 0) return;

        MinecraftECSBridge bridge = MixinHelper.getBridge();
        if (bridge == null) return;

        try {
            MemorySegment memory = bridge.getComponentMemory();
            long base = bridge.getEntityMemoryOffset(slot);

            long flags = memory.get(ValueLayout.JAVA_LONG, base + BridgeComponents.META_FLAGS);

            if (aiCulled) {
                flags |= BridgeComponents.FLAG_NO_CLIP; // Repurpose or add AI_CULLED flag
            } else {
                flags &= ~BridgeComponents.FLAG_NO_CLIP;
            }

            memory.set(ValueLayout.JAVA_LONG, base + BridgeComponents.META_FLAGS, flags);
        } catch (Exception e) {
            // Silently ignore - non-critical
        }
    }

    /**
     * Gets the current LOD level for external queries.
     */
    @Unique
    public int astralis$getCurrentLOD() {
        return astralis$currentLOD;
    }

    /**
     * Checks if AI was skipped last tick.
     */
    @Unique
    public boolean astralis$wasAISkipped() {
        return astralis$aiSkippedLastTick;
    }
}
