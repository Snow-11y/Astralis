package stellar.snow.astralis.mixins.util;

import stellar.snow.astralis.bridge.BridgeComponents;
import stellar.snow.astralis.bridge.BridgeMixinInterface;
import stellar.snow.astralis.bridge.MinecraftECSBridge;
import net.minecraft.entity.Entity;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MixinHelper - Thread-safe utility methods for mixin operations.
 *
 * <p>All methods are designed to be allocation-free in steady-state operation.</p>
 */
public final class MixinHelper {

    private static final Logger LOGGER = Logger.getLogger(MixinHelper.class.getName());

    // Thread-local arrays for interpolation output (avoids allocation)
    private static final ThreadLocal<double[]> INTERPOLATION_RESULT = 
        ThreadLocal.withInitial(() -> new double[6]);
    
    private static final ThreadLocal<double[]> POSITION_BUFFER = 
        ThreadLocal.withInitial(() -> new double[3]);

    private MixinHelper() {}

    // ========================================================================
    // SAFE BRIDGE ACCESS
    // ========================================================================

    /**
     * Safely gets the bridge instance, returning null if not available.
     */
    public static MinecraftECSBridge getBridge() {
        try {
            MinecraftECSBridge bridge = MinecraftECSBridge.getInstance();
            return bridge.isRunning() ? bridge : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Safely checks if an entity is registered with the bridge.
     */
    public static boolean isRegistered(Entity entity) {
        if (entity == null) return false;
        
        if (entity instanceof BridgeMixinInterface ext) {
            return ext.astralis$isRegistered();
        }
        return false;
    }

    /**
     * Safely gets the bridge slot for an entity.
     *
     * @return slot index, or -1 if not available
     */
    public static int getSlot(Entity entity) {
        if (entity == null) return -1;
        
        if (entity instanceof BridgeMixinInterface ext) {
            return ext.astralis$getBridgeSlot();
        }
        return -1;
    }

    // ========================================================================
    // INTERPOLATION HELPERS
    // ========================================================================

    /**
     * Gets interpolated transform for rendering.
     * Uses thread-local storage to avoid allocation.
     *
     * @param entity       the Minecraft entity
     * @param partialTicks interpolation factor [0, 1)
     * @return interpolated [x, y, z, yaw, pitch, roll] or null if unavailable
     */
    public static double[] getInterpolatedTransform(Entity entity, float partialTicks) {
        if (entity == null) return null;
        
        if (!(entity instanceof BridgeMixinInterface ext)) return null;
        
        int slot = ext.astralis$getBridgeSlot();
        if (slot < 0) return null;

        MinecraftECSBridge bridge = getBridge();
        if (bridge == null) return null;

        double[] result = INTERPOLATION_RESULT.get();
        
        try {
            bridge.getInterpolatedTransform(slot, partialTicks, result);
            return result;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Interpolation failed for slot " + slot, e);
            return null;
        }
    }

    /**
     * Applies interpolated position to entity for rendering.
     * Stores original values for restoration after render.
     *
     * @param entity       the entity to modify
     * @param partialTicks interpolation factor
     * @param backup       array to store original [posX, posY, posZ, yaw, pitch]
     * @return true if interpolation was applied
     */
    public static boolean applyInterpolation(Entity entity, float partialTicks, double[] backup) {
        double[] interp = getInterpolatedTransform(entity, partialTicks);
        if (interp == null) return false;

        // Backup original values
        backup[0] = entity.posX;
        backup[1] = entity.posY;
        backup[2] = entity.posZ;
        backup[3] = entity.rotationYaw;
        backup[4] = entity.rotationPitch;

        // Apply interpolated values
        entity.posX = interp[0];
        entity.posY = interp[1];
        entity.posZ = interp[2];
        entity.rotationYaw = (float) interp[3];
        entity.rotationPitch = (float) interp[4];

        return true;
    }

    /**
     * Restores original position after rendering.
     *
     * @param entity the entity to restore
     * @param backup the backup array from applyInterpolation
     */
    public static void restoreFromBackup(Entity entity, double[] backup) {
        entity.posX = backup[0];
        entity.posY = backup[1];
        entity.posZ = backup[2];
        entity.rotationYaw = (float) backup[3];
        entity.rotationPitch = (float) backup[4];
    }

    // ========================================================================
    // CULLING HELPERS
    // ========================================================================

    /**
     * Distance squared check (faster than distance).
     */
    public static double distanceSquared(Entity a, Entity b) {
        double dx = a.posX - b.posX;
        double dy = a.posY - b.posY;
        double dz = a.posZ - b.posZ;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Distance squared from a point.
     */
    public static double distanceSquared(Entity e, double x, double y, double z) {
        double dx = e.posX - x;
        double dy = e.posY - y;
        double dz = e.posZ - z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Checks if entity is within render distance.
     * Uses squared distance to avoid sqrt.
     */
    public static boolean isWithinRenderDistance(Entity entity, double viewX, double viewY, double viewZ, double maxDistSq) {
        return distanceSquared(entity, viewX, viewY, viewZ) <= maxDistSq;
    }

    // ========================================================================
    // COMPONENT ACCESS HELPERS
    // ========================================================================

    /**
     * Reads component flags from bridge memory.
     *
     * @param slot the entity slot
     * @return flags value, or 0 if unavailable
     */
    public static long getComponentFlags(int slot) {
        if (slot < 0) return 0L;

        MinecraftECSBridge bridge = getBridge();
        if (bridge == null) return 0L;

        try {
            MemorySegment memory = bridge.getComponentMemory();
            long base = bridge.getEntityMemoryOffset(slot);
            return memory.get(ValueLayout.JAVA_LONG, base + BridgeComponents.META_FLAGS);
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Sets component flags in bridge memory.
     *
     * @param slot  the entity slot
     * @param flags flags to set (OR operation)
     */
    public static void setComponentFlags(int slot, long flags) {
        if (slot < 0) return;

        MinecraftECSBridge bridge = getBridge();
        if (bridge == null) return;

        try {
            MemorySegment memory = bridge.getComponentMemory();
            long base = bridge.getEntityMemoryOffset(slot);
            long current = memory.get(ValueLayout.JAVA_LONG, base + BridgeComponents.META_FLAGS);
            memory.set(ValueLayout.JAVA_LONG, base + BridgeComponents.META_FLAGS, current | flags);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to set component flags", e);
        }
    }

    /**
     * Clears component flags in bridge memory.
     *
     * @param slot  the entity slot
     * @param flags flags to clear
     */
    public static void clearComponentFlags(int slot, long flags) {
        if (slot < 0) return;

        MinecraftECSBridge bridge = getBridge();
        if (bridge == null) return;

        try {
            MemorySegment memory = bridge.getComponentMemory();
            long base = bridge.getEntityMemoryOffset(slot);
            long current = memory.get(ValueLayout.JAVA_LONG, base + BridgeComponents.META_FLAGS);
            memory.set(ValueLayout.JAVA_LONG, base + BridgeComponents.META_FLAGS, current & ~flags);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to clear component flags", e);
        }
    }

    // ========================================================================
    // LOD (LEVEL OF DETAIL) CALCULATION
    // ========================================================================

    /** LOD levels */
    public static final int LOD_FULL = 0;      // Full update
    public static final int LOD_REDUCED = 1;   // Reduced update rate
    public static final int LOD_MINIMAL = 2;   // Minimal updates
    public static final int LOD_CULLED = 3;    // No updates

    /** Distance thresholds (squared) */
    private static final double LOD_REDUCED_DIST_SQ = 32.0 * 32.0;    // 32 blocks
    private static final double LOD_MINIMAL_DIST_SQ = 64.0 * 64.0;    // 64 blocks
    private static final double LOD_CULLED_DIST_SQ = 128.0 * 128.0;   // 128 blocks

    /**
     * Calculates LOD level based on distance to viewer.
     *
     * @param distanceSquared squared distance to viewer
     * @return LOD level constant
     */
    public static int calculateLOD(double distanceSquared) {
        if (distanceSquared >= LOD_CULLED_DIST_SQ) return LOD_CULLED;
        if (distanceSquared >= LOD_MINIMAL_DIST_SQ) return LOD_MINIMAL;
        if (distanceSquared >= LOD_REDUCED_DIST_SQ) return LOD_REDUCED;
        return LOD_FULL;
    }

    /**
     * Determines if AI should run this tick based on LOD.
     *
     * @param lod       current LOD level
     * @param tickCount current tick count
     * @return true if AI should process
     */
    public static boolean shouldProcessAI(int lod, long tickCount) {
        return switch (lod) {
            case LOD_FULL -> true;
            case LOD_REDUCED -> (tickCount & 1) == 0;      // Every 2 ticks
            case LOD_MINIMAL -> (tickCount & 3) == 0;      // Every 4 ticks
            case LOD_CULLED -> false;
            default -> true;
        };
    }

    /**
     * Determines if physics should update this tick based on LOD.
     *
     * @param lod       current LOD level
     * @param tickCount current tick count
     * @return true if physics should process
     */
    public static boolean shouldProcessPhysics(int lod, long tickCount) {
        return switch (lod) {
            case LOD_FULL -> true;
            case LOD_REDUCED -> true;                       // Physics always at reduced
            case LOD_MINIMAL -> (tickCount & 1) == 0;       // Every 2 ticks
            case LOD_CULLED -> (tickCount & 7) == 0;        // Every 8 ticks
            default -> true;
        };
    }
}
