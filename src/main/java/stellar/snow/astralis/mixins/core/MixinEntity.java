package stellar.snow.astralis.mixins.core;

import stellar.snow.astralis.bridge.BridgeMixinInterface;
import stellar.snow.astralis.bridge.MinecraftECSBridge;
import stellar.snow.astralis.ecs.Entity;
import stellar.snow.astralis.mixins.util.MixinHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MixinEntity - Core entity extension implementing BridgeMixinInterface.
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Stores ECS slot and entity handle directly in MC entity</li>
 *   <li>Hooks entity lifecycle events (death, teleport)</li>
 *   <li>Provides fast O(1) access to ECS data</li>
 *   <li>Thread-safe field access for render thread</li>
 * </ul>
 */
@Mixin(net.minecraft.entity.Entity.class)
public abstract class MixinEntity implements BridgeMixinInterface {

    @Unique
    private static final Logger astralis$LOGGER = Logger.getLogger("Astralis-MixinEntity");

    // ========================================================================
    // SHADOWS
    // ========================================================================

    @Shadow public int entityId;
    @Shadow public World worldObj;
    @Shadow public double posX;
    @Shadow public double posY;
    @Shadow public double posZ;
    @Shadow public double prevPosX;
    @Shadow public double prevPosY;
    @Shadow public double prevPosZ;
    @Shadow public float rotationYaw;
    @Shadow public float rotationPitch;
    @Shadow public double motionX;
    @Shadow public double motionY;
    @Shadow public double motionZ;
    @Shadow public boolean isDead;

    // ========================================================================
    // BRIDGE INTERFACE FIELDS (Volatile for cross-thread visibility)
    // ========================================================================

    /**
     * Slot index in the bridge's entity array.
     * -1 means not registered.
     */
    @Unique
    private volatile int astralis$bridgeSlot = -1;

    /**
     * Direct reference to ECS entity.
     * Null means not registered.
     */
    @Unique
    private volatile Entity astralis$ecsEntity = null;

    /**
     * Internal state flags.
     * Uses volatile for cross-thread visibility.
     */
    @Unique
    private volatile int astralis$flags = 0;

    /**
     * Cached LOD level to avoid recalculation.
     */
    @Unique
    private int astralis$lodLevel = MixinHelper.LOD_FULL;

    /**
     * Last tick when LOD was calculated.
     */
    @Unique
    private long astralis$lodCalculationTick = 0;

    // ========================================================================
    // BRIDGE INTERFACE IMPLEMENTATION
    // ========================================================================

    @Override
    public void astralis$setBridgeSlot(int slot) {
        this.astralis$bridgeSlot = slot;
        if (slot >= 0) {
            astralis$setFlag(FLAG_REGISTERED);
        } else {
            astralis$clearFlag(FLAG_REGISTERED);
        }
    }

    @Override
    public int astralis$getBridgeSlot() {
        return this.astralis$bridgeSlot;
    }

    @Override
    public void astralis$setEcsEntity(Entity entity) {
        this.astralis$ecsEntity = entity;
    }

    @Override
    public Entity astralis$getEcsEntity() {
        return this.astralis$ecsEntity;
    }

    @Override
    public void astralis$setFlags(int flags) {
        this.astralis$flags = flags;
    }

    @Override
    public int astralis$getFlags() {
        return this.astralis$flags;
    }

    // ========================================================================
    // LIFECYCLE HOOKS
    // ========================================================================

    /**
     * Hook entity death to clean up ECS resources.
     */
    @Inject(
        method = "setDead",
        at = @At("HEAD")
    )
    private void astralis$onSetDead(CallbackInfo ci) {
        astralis$unregisterFromBridge("setDead");
    }

    /**
     * Hook entity removal for cleanup.
     */
    @Inject(
        method = "onRemovedFromWorld",
        at = @At("HEAD"),
        remap = false, // Forge method
        require = 0    // Optional - may not exist
    )
    private void astralis$onRemovedFromWorld(CallbackInfo ci) {
        astralis$unregisterFromBridge("onRemovedFromWorld");
    }

    /**
     * Common unregistration logic.
     */
    @Unique
    private void astralis$unregisterFromBridge(String source) {
        if (astralis$bridgeSlot < 0) return;

        try {
            MinecraftECSBridge bridge = MixinHelper.getBridge();
            if (bridge != null) {
                bridge.unregisterEntity((net.minecraft.entity.Entity) (Object) this);
            }
        } catch (Exception e) {
            astralis$LOGGER.log(Level.FINE, "[Astralis] Error during unregister from " + source, e);
        } finally {
            astralis$bridgeSlot = -1;
            astralis$ecsEntity = null;
            astralis$flags = 0;
        }
    }

    // ========================================================================
    // POSITION CHANGE DETECTION
    // ========================================================================

    /**
     * Detect significant position changes (teleports).
     */
    @Inject(
        method = "setPosition",
        at = @At("HEAD")
    )
    private void astralis$onSetPosition(double x, double y, double z, CallbackInfo ci) {
        if (astralis$bridgeSlot < 0) return;

        // Check for teleport (large position change)
        double dx = x - posX;
        double dy = y - posY;
        double dz = z - posZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        // If distance > 8 blocks, consider it a teleport
        if (distSq > 64.0) {
            astralis$setFlag(FLAG_TELEPORTED);
            astralis$setFlag(FLAG_DIRTY_INBOUND);
        }
    }

    /**
     * Detect position changes via setPositionAndRotation.
     */
    @Inject(
        method = "setPositionAndRotation",
        at = @At("HEAD")
    )
    private void astralis$onSetPositionAndRotation(double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        if (astralis$bridgeSlot < 0) return;

        double dx = x - posX;
        double dy = y - posY;
        double dz = z - posZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq > 64.0) {
            astralis$setFlag(FLAG_TELEPORTED);
            astralis$setFlag(FLAG_DIRTY_INBOUND);
        }
    }

    // ========================================================================
    // MOVEMENT INTEGRATION
    // ========================================================================

    /**
     * Optional: Intercept vanilla movement when ECS controls physics.
     */
    @Inject(
        method = "moveEntity",
        at = @At("HEAD"),
        cancellable = true
    )
    private void astralis$onMoveEntity(double x, double y, double z, CallbackInfo ci) {
        // If ECS manages physics for this entity, we might want to skip vanilla movement
        if (astralis$hasFlag(FLAG_ECS_PHYSICS)) {
            // Option 1: Cancel vanilla movement entirely
            // ci.cancel();
            
            // Option 2: Let vanilla handle collision, then override result
            // (handled in post-move hook)
        }
    }

    // ========================================================================
    // LOD MANAGEMENT
    // ========================================================================

    /**
     * Gets cached LOD level, recalculating if stale.
     *
     * @param viewerX viewer X position
     * @param viewerY viewer Y position
     * @param viewerZ viewer Z position
     * @param currentTick current game tick
     * @return LOD level
     */
    @Unique
    public int astralis$getLOD(double viewerX, double viewerY, double viewerZ, long currentTick) {
        // Recalculate LOD every 10 ticks
        if (currentTick - astralis$lodCalculationTick >= 10) {
            double distSq = MixinHelper.distanceSquared(
                (net.minecraft.entity.Entity) (Object) this, 
                viewerX, viewerY, viewerZ
            );
            astralis$lodLevel = MixinHelper.calculateLOD(distSq);
            astralis$lodCalculationTick = currentTick;
        }
        return astralis$lodLevel;
    }

    /**
     * Force LOD recalculation next access.
     */
    @Unique
    public void astralis$invalidateLOD() {
        astralis$lodCalculationTick = 0;
    }
}
