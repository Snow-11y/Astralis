package stellar.snow.astralis.nexus.controlpanel;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;

/**
 * BridgeComponents - Memory layout definitions and high-performance accessors.
 *
 * <h2>Entity Memory Block Layout (256 bytes total):</h2>
 * <pre>
 * Offset  Size  Field
 * ------  ----  -----
 * 0       64    Transform (current)
 *   0       8     x (double)
 *   8       8     y (double)
 *   16      8     z (double)
 *   24      4     yaw (float)
 *   28      4     pitch (float)
 *   32      4     roll (float)
 *   36      4     flags (int)
 *   40      24    [padding]
 *
 * 64      64    Transform (previous, for interpolation)
 *   [same layout as current]
 *
 * 128     32    Velocity
 *   128     8     vx (double)
 *   136     8     vy (double)
 *   144     8     vz (double)
 *   152     4     speed (float - cached magnitude)
 *   156     4     [padding]
 *
 * 160     32    Acceleration
 *   160     8     ax (double)
 *   168     8     ay (double)
 *   176     8     az (double)
 *   184     8     [padding]
 *
 * 192     32    Metadata
 *   192     4     mcEntityId (int)
 *   196     4     ecsEntityId (int)
 *   200     8     flags (long)
 *   208     8     lastSyncTick (long)
 *   216     8     [padding]
 *
 * 224     32    Reserved/User Data
 * </pre>
 */
public final class BridgeComponents {

    private BridgeComponents() {}

    // ========================================================================
    // VECTOR API SPECIES (For SIMD operations)
    // ========================================================================

    public static final VectorSpecies<Double> DOUBLE_SPECIES = DoubleVector.SPECIES_PREFERRED;
    public static final VectorSpecies<Float> FLOAT_SPECIES = FloatVector.SPECIES_PREFERRED;

    // ========================================================================
    // TRANSFORM OFFSETS (Current)
    // ========================================================================

    public static final long TRANSFORM_X = 0L;
    public static final long TRANSFORM_Y = 8L;
    public static final long TRANSFORM_Z = 16L;
    public static final long TRANSFORM_YAW = 24L;
    public static final long TRANSFORM_PITCH = 28L;
    public static final long TRANSFORM_ROLL = 32L;
    public static final long TRANSFORM_FLAGS = 36L;
    public static final long TRANSFORM_SIZE = 64L;

    // ========================================================================
    // TRANSFORM OFFSETS (Previous)
    // ========================================================================

    public static final long PREV_TRANSFORM_BASE = 64L;
    public static final long PREV_TRANSFORM_X = PREV_TRANSFORM_BASE + 0L;
    public static final long PREV_TRANSFORM_Y = PREV_TRANSFORM_BASE + 8L;
    public static final long PREV_TRANSFORM_Z = PREV_TRANSFORM_BASE + 16L;
    public static final long PREV_TRANSFORM_YAW = PREV_TRANSFORM_BASE + 24L;
    public static final long PREV_TRANSFORM_PITCH = PREV_TRANSFORM_BASE + 28L;
    public static final long PREV_TRANSFORM_ROLL = PREV_TRANSFORM_BASE + 32L;

    // ========================================================================
    // VELOCITY OFFSETS
    // ========================================================================

    public static final long VELOCITY_BASE = 128L;
    public static final long VELOCITY_X = VELOCITY_BASE + 0L;
    public static final long VELOCITY_Y = VELOCITY_BASE + 8L;
    public static final long VELOCITY_Z = VELOCITY_BASE + 16L;
    public static final long VELOCITY_SPEED = VELOCITY_BASE + 24L;
    public static final long VELOCITY_SIZE = 32L;

    // ========================================================================
    // ACCELERATION OFFSETS
    // ========================================================================

    public static final long ACCEL_BASE = 160L;
    public static final long ACCEL_X = ACCEL_BASE + 0L;
    public static final long ACCEL_Y = ACCEL_BASE + 8L;
    public static final long ACCEL_Z = ACCEL_BASE + 16L;
    public static final long ACCEL_SIZE = 32L;

    // ========================================================================
    // METADATA OFFSETS
    // ========================================================================

    public static final long META_BASE = 192L;
    public static final long META_MC_ID = META_BASE + 0L;
    public static final long META_ECS_ID = META_BASE + 4L;
    public static final long META_FLAGS = META_BASE + 8L;
    public static final long META_LAST_SYNC = META_BASE + 16L;
    public static final long META_SIZE = 32L;

    // ========================================================================
    // USER DATA OFFSETS
    // ========================================================================

    public static final long USER_DATA_BASE = 224L;
    public static final long USER_DATA_SIZE = 32L;

    // ========================================================================
    // GPU-COMPATIBLE TRANSFORM (Packed for shader upload)
    // ========================================================================

    /** Size of transform data for GPU: position (3 floats) + rotation (4 floats for quaternion) */
    public static final int GPU_TRANSFORM_SIZE = 28; // 7 floats * 4 bytes

    // ========================================================================
    // METADATA FLAGS
    // ========================================================================

    public static final long FLAG_TRANSFORM_DIRTY = 1L << 0;
    public static final long FLAG_VELOCITY_DIRTY = 1L << 1;
    public static final long FLAG_ACCEL_DIRTY = 1L << 2;
    public static final long FLAG_NEEDS_SYNC = 1L << 3;
    public static final long FLAG_TELEPORTED = 1L << 4;
    public static final long FLAG_DEAD = 1L << 5;
    public static final long FLAG_INVISIBLE = 1L << 6;
    public static final long FLAG_NO_CLIP = 1L << 7;

    // ========================================================================
    // MEMORY LAYOUT DEFINITIONS
    // ========================================================================

    public static final StructLayout TRANSFORM_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_DOUBLE.withName("x"),
            ValueLayout.JAVA_DOUBLE.withName("y"),
            ValueLayout.JAVA_DOUBLE.withName("z"),
            ValueLayout.JAVA_FLOAT.withName("yaw"),
            ValueLayout.JAVA_FLOAT.withName("pitch"),
            ValueLayout.JAVA_FLOAT.withName("roll"),
            ValueLayout.JAVA_INT.withName("flags"),
            MemoryLayout.paddingLayout(24)
    ).withName("Transform").withByteAlignment(64);

    public static final StructLayout VELOCITY_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_DOUBLE.withName("vx"),
            ValueLayout.JAVA_DOUBLE.withName("vy"),
            ValueLayout.JAVA_DOUBLE.withName("vz"),
            ValueLayout.JAVA_FLOAT.withName("speed"),
            MemoryLayout.paddingLayout(4)
    ).withName("Velocity").withByteAlignment(32);

    public static final StructLayout ACCEL_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_DOUBLE.withName("ax"),
            ValueLayout.JAVA_DOUBLE.withName("ay"),
            ValueLayout.JAVA_DOUBLE.withName("az"),
            MemoryLayout.paddingLayout(8)
    ).withName("Acceleration").withByteAlignment(32);

    public static final StructLayout METADATA_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("mcEntityId"),
            ValueLayout.JAVA_INT.withName("ecsEntityId"),
            ValueLayout.JAVA_LONG.withName("flags"),
            ValueLayout.JAVA_LONG.withName("lastSyncTick"),
            MemoryLayout.paddingLayout(8)
    ).withName("Metadata").withByteAlignment(16);

    // ========================================================================
    // INLINE ACCESSOR METHODS (For JIT optimization)
    // ========================================================================

    /**
     * Reads position from component memory.
     * Writes to provided array to avoid allocation.
     */
    public static void getPosition(MemorySegment memory, long entityBase, double[] out) {
        out[0] = memory.get(ValueLayout.JAVA_DOUBLE, entityBase + TRANSFORM_X);
        out[1] = memory.get(ValueLayout.JAVA_DOUBLE, entityBase + TRANSFORM_Y);
        out[2] = memory.get(ValueLayout.JAVA_DOUBLE, entityBase + TRANSFORM_Z);
    }

    /**
     * Writes position to component memory.
     */
    public static void setPosition(MemorySegment memory, long entityBase, double x, double y, double z) {
        memory.set(ValueLayout.JAVA_DOUBLE, entityBase + TRANSFORM_X, x);
        memory.set(ValueLayout.JAVA_DOUBLE, entityBase + TRANSFORM_Y, y);
        memory.set(ValueLayout.JAVA_DOUBLE, entityBase + TRANSFORM_Z, z);
    }

    /**
     * Copies current transform to previous transform.
     */
    public static void copyToPrevious(MemorySegment memory, long entityBase) {
        // Copy 40 bytes (x,y,z,yaw,pitch,roll,flags) from current to previous
        MemorySegment.copy(
                memory, entityBase,
                memory, entityBase + PREV_TRANSFORM_BASE,
                40
        );
    }

    /**
     * Reads velocity from component memory.
     */
    public static void getVelocity(MemorySegment memory, long entityBase, double[] out) {
        out[0] = memory.get(ValueLayout.JAVA_DOUBLE, entityBase + VELOCITY_X);
        out[1] = memory.get(ValueLayout.JAVA_DOUBLE, entityBase + VELOCITY_Y);
        out[2] = memory.get(ValueLayout.JAVA_DOUBLE, entityBase + VELOCITY_Z);
    }

    /**
     * Writes velocity to component memory.
     */
    public static void setVelocity(MemorySegment memory, long entityBase, double vx, double vy, double vz) {
        memory.set(ValueLayout.JAVA_DOUBLE, entityBase + VELOCITY_X, vx);
        memory.set(ValueLayout.JAVA_DOUBLE, entityBase + VELOCITY_Y, vy);
        memory.set(ValueLayout.JAVA_DOUBLE, entityBase + VELOCITY_Z, vz);

        // Cache speed magnitude
        float speed = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
        memory.set(ValueLayout.JAVA_FLOAT, entityBase + VELOCITY_SPEED, speed);
    }

    /**
     * Gets metadata flags.
     */
    public static long getFlags(MemorySegment memory, long entityBase) {
        return memory.get(ValueLayout.JAVA_LONG, entityBase + META_FLAGS);
    }

    /**
     * Sets metadata flags (atomic OR).
     */
    public static void setFlags(MemorySegment memory, long entityBase, long flags) {
        long current = memory.get(ValueLayout.JAVA_LONG, entityBase + META_FLAGS);
        memory.set(ValueLayout.JAVA_LONG, entityBase + META_FLAGS, current | flags);
    }

    /**
     * Clears metadata flags (atomic AND NOT).
     */
    public static void clearFlags(MemorySegment memory, long entityBase, long flags) {
        long current = memory.get(ValueLayout.JAVA_LONG, entityBase + META_FLAGS);
        memory.set(ValueLayout.JAVA_LONG, entityBase + META_FLAGS, current & ~flags);
    }

    /**
     * Gets and clears all dirty flags atomically.
     */
    public static long getAndClearDirtyFlags(MemorySegment memory, long entityBase) {
        long current = memory.get(ValueLayout.JAVA_LONG, entityBase + META_FLAGS);
        long dirty = current & (FLAG_TRANSFORM_DIRTY | FLAG_VELOCITY_DIRTY | FLAG_ACCEL_DIRTY);
        memory.set(ValueLayout.JAVA_LONG, entityBase + META_FLAGS, current & ~dirty);
        return dirty;
    }

    /**
     * Checks if entity has any of the specified flags set.
     */
    public static boolean hasFlags(MemorySegment memory, long entityBase, long flags) {
        return (memory.get(ValueLayout.JAVA_LONG, entityBase + META_FLAGS) & flags) != 0;
    }
}
