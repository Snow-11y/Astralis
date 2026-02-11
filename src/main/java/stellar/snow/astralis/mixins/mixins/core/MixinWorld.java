package stellar.snow.astralis.mixins.core;

import stellar.snow.astralis.bridge.BridgeMixinInterface;
import stellar.snow.astralis.bridge.MinecraftECSBridge;
import stellar.snow.astralis.mixins.util.MixinHelper;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MixinWorld - World-level entity lifecycle management.
 *
 * <h2>Responsibilities:</h2>
 * <ul>
 *   <li>Register entities when they spawn/join</li>
 *   <li>Unregister entities when they're removed</li>
 *   <li>Handle chunk loading/unloading</li>
 *   <li>Batch entity processing hooks</li>
 * </ul>
 */
@Mixin(World.class)
public abstract class MixinWorld {

    @Unique
    private static final Logger astralis$LOGGER = Logger.getLogger("Astralis-MixinWorld");

    // ========================================================================
    // SHADOWS
    // ========================================================================

    @Shadow public boolean isRemote;
    @Shadow @Final public Profiler theProfiler;
    @Shadow @Final public List<Entity> loadedEntityList;

    // ========================================================================
    // ENTITY SPAWN/JOIN
    // ========================================================================

    /**
     * Register entity with bridge when spawned in world.
     */
    @Inject(
        method = "spawnEntityInWorld",
        at = @At("RETURN")
    )
    private void astralis$onSpawnEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        // Only process if spawn was successful
        if (!cir.getReturnValue()) return;

        astralis$registerEntity(entity, "spawnEntityInWorld");
    }

    /**
     * Register entity when joining via other means (e.g., loading from NBT).
     */
    @Inject(
        method = "onEntityAdded",
        at = @At("RETURN"),
        remap = false, // Forge method
        require = 0
    )
    private void astralis$onEntityAdded(Entity entity, CallbackInfo ci) {
        astralis$registerEntity(entity, "onEntityAdded");
    }

    /**
     * Common registration logic.
     */
    @Unique
    private void astralis$registerEntity(Entity entity, String source) {
        if (!isRemote) return; // Client-side only
        if (entity == null) return;

        // Skip if already registered
        if (entity instanceof BridgeMixinInterface ext) {
            if (ext.astralis$isRegistered()) return;
        }

        MinecraftECSBridge bridge = MixinHelper.getBridge();
        if (bridge == null) return;

        try {
            theProfiler.startSection("astralis_register");
            int slot = bridge.registerEntity(entity);
            
            if (slot >= 0) {
                astralis$LOGGER.log(Level.FINE, 
                    "[Astralis] Registered entity {0} (ID: {1}) in slot {2} via {3}",
                    new Object[]{entity.getClass().getSimpleName(), entity.getEntityId(), slot, source}
                );
            }
        } catch (Exception e) {
            astralis$LOGGER.log(Level.WARNING, 
                "[Astralis] Failed to register entity " + entity.getEntityId(), e);
        } finally {
            theProfiler.endSection();
        }
    }

    // ========================================================================
    // ENTITY REMOVAL
    // ========================================================================

    /**
     * Unregister entity when explicitly removed from world.
     */
    @Inject(
        method = "removeEntity",
        at = @At("HEAD")
    )
    private void astralis$onRemoveEntity(Entity entity, CallbackInfo ci) {
        astralis$unregisterEntity(entity, "removeEntity");
    }

    /**
     * Unregister entity when removed via other means.
     */
    @Inject(
        method = "onEntityRemoved",
        at = @At("HEAD"),
        remap = false, // Forge method
        require = 0
    )
    private void astralis$onEntityRemoved(Entity entity, CallbackInfo ci) {
        astralis$unregisterEntity(entity, "onEntityRemoved");
    }

    /**
     * Handle removeEntityDangerously for thorough cleanup.
     */
    @Inject(
        method = "removeEntityDangerously",
        at = @At("HEAD")
    )
    private void astralis$onRemoveEntityDangerously(Entity entity, CallbackInfo ci) {
        astralis$unregisterEntity(entity, "removeEntityDangerously");
    }

    /**
     * Common unregistration logic.
     */
    @Unique
    private void astralis$unregisterEntity(Entity entity, String source) {
        if (!isRemote) return;
        if (entity == null) return;

        // Skip if not registered
        if (entity instanceof BridgeMixinInterface ext) {
            if (!ext.astralis$isRegistered()) return;
        } else {
            return;
        }

        MinecraftECSBridge bridge = MixinHelper.getBridge();
        if (bridge == null) return;

        try {
            theProfiler.startSection("astralis_unregister");
            bridge.unregisterEntity(entity);
            
            astralis$LOGGER.log(Level.FINE,
                "[Astralis] Unregistered entity {0} (ID: {1}) via {2}",
                new Object[]{entity.getClass().getSimpleName(), entity.getEntityId(), source}
            );
        } catch (Exception e) {
            astralis$LOGGER.log(Level.WARNING,
                "[Astralis] Failed to unregister entity " + entity.getEntityId(), e);
        } finally {
            theProfiler.endSection();
        }
    }

    // ========================================================================
    // BATCH UPDATE HOOKS
    // ========================================================================

    /**
     * Pre-tick hook for entity updates.
     * Can be used to prepare ECS state before vanilla updates.
     */
    @Inject(
        method = "updateEntities",
        at = @At("HEAD")
    )
    private void astralis$onPreUpdateEntities(CallbackInfo ci) {
        if (!isRemote) return;

        theProfiler.startSection("astralis_preUpdate");
        // Any pre-update preparation
        theProfiler.endSection();
    }

    /**
     * Post-tick hook for entity updates.
     * Can be used to sync ECS state after vanilla updates.
     */
    @Inject(
        method = "updateEntities",
        at = @At("RETURN")
    )
    private void astralis$onPostUpdateEntities(CallbackInfo ci) {
        if (!isRemote) return;

        theProfiler.startSection("astralis_postUpdate");
        // Any post-update synchronization
        theProfiler.endSection();
    }
}
