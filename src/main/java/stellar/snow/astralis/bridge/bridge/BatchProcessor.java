package stellar.snow.astralis.bridge;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import net.minecraft.entity.Entity;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * BatchProcessor - SIMD-optimized batch operations for entity synchronization.
 *
 * <h2>Optimization Strategies:</h2>
 * <ul>
 *   <li>Vector API for parallel double operations</li>
 *   <li>Sequential memory access patterns for cache efficiency</li>
 *   <li>Fork-Join parallelism for large batches</li>
 *   <li>Prefetching hints for memory controller</li>
 * </ul>
 */
public final class BatchProcessor {

    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final int VECTOR_LENGTH = SPECIES.length();

    /** Threshold for parallel processing */
    private static final int PARALLEL_THRESHOLD = 256;

    /** Chunk size for parallel work splitting */
    private static final int PARALLEL_CHUNK_SIZE = 64;

    private final MemorySegment componentMemory;
    private final int entityBlockSize;
    private final ForkJoinPool pool;

    // Preallocated arrays for batch operations (thread-local)
    private static final ThreadLocal<double[]> POSITION_BUFFER =
            ThreadLocal.withInitial(() -> new double[PARALLEL_CHUNK_SIZE * 3]);
    private static final ThreadLocal<double[]> VELOCITY_BUFFER =
            ThreadLocal.withInitial(() -> new double[PARALLEL_CHUNK_SIZE * 3]);

    public BatchProcessor(MemorySegment componentMemory, int entityBlockSize) {
        this.componentMemory = componentMemory;
        this.entityBlockSize = entityBlockSize;
        this.pool = ForkJoinPool.commonPool();
    }

    // ========================================================================
    // INBOUND SYNC (MC -> ECS)
    // ========================================================================

    /**
     * Synchronizes state from Minecraft entities to ECS component memory.
     * Optimized for sequential memory writes.
     *
     * @param slots     array of active slot indices
     * @param count     number of active slots
     * @param mcEntities array of Minecraft entity references
     */
    public void syncInbound(int[] slots, int count, Entity[] mcEntities) {
        if (count == 0) return;

        if (count >= PARALLEL_THRESHOLD) {
            syncInboundParallel(slots, count, mcEntities);
        } else {
            syncInboundSequential(slots, count, mcEntities);
        }
    }

    private void syncInboundSequential(int[] slots, int count, Entity[] mcEntities) {
        for (int i = 0; i < count; i++) {
            int slot = slots[i];
            Entity mc = mcEntities[slot];
            if (mc == null) continue;

            long base = (long) slot * entityBlockSize;

            // Copy current to previous (for interpolation)
            BridgeComponents.copyToPrevious(componentMemory, base);

            // Read new values from MC entity
            componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_X, mc.posX);
            componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Y, mc.posY);
            componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Z, mc.posZ);
            componentMemory.set(ValueLayout.JAVA_FLOAT, base + BridgeComponents.TRANSFORM_YAW, mc.rotationYaw);
            componentMemory.set(ValueLayout.JAVA_FLOAT, base + BridgeComponents.TRANSFORM_PITCH, mc.rotationPitch);

            componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_X, mc.motionX);
            componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_Y, mc.motionY);
            componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_Z, mc.motionZ);

            // Compute and cache velocity magnitude
            float speed = (float) Math.sqrt(mc.motionX * mc.motionX + mc.motionY * mc.motionY + mc.motionZ * mc.motionZ);
            componentMemory.set(ValueLayout.JAVA_FLOAT, base + BridgeComponents.VELOCITY_SPEED, speed);
        }
    }

    private void syncInboundParallel(int[] slots, int count, Entity[] mcEntities) {
        pool.invoke(new InboundSyncTask(slots, 0, count, mcEntities, componentMemory, entityBlockSize));
    }

    private static final class InboundSyncTask extends RecursiveAction {
        private final int[] slots;
        private final int start;
        private final int end;
        private final Entity[] mcEntities;
        private final MemorySegment memory;
        private final int blockSize;

        InboundSyncTask(int[] slots, int start, int end, Entity[] mcEntities, MemorySegment memory, int blockSize) {
            this.slots = slots;
            this.start = start;
            this.end = end;
            this.mcEntities = mcEntities;
            this.memory = memory;
            this.blockSize = blockSize;
        }

        @Override
        protected void compute() {
            int length = end - start;

            if (length <= PARALLEL_CHUNK_SIZE) {
                computeDirectly();
            } else {
                int mid = start + length / 2;
                invokeAll(
                        new InboundSyncTask(slots, start, mid, mcEntities, memory, blockSize),
                        new InboundSyncTask(slots, mid, end, mcEntities, memory, blockSize)
                );
            }
        }

        private void computeDirectly() {
            for (int i = start; i < end; i++) {
                int slot = slots[i];
                Entity mc = mcEntities[slot];
                if (mc == null) continue;

                long base = (long) slot * blockSize;

                BridgeComponents.copyToPrevious(memory, base);

                memory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_X, mc.posX);
                memory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Y, mc.posY);
                memory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Z, mc.posZ);
                memory.set(ValueLayout.JAVA_FLOAT, base + BridgeComponents.TRANSFORM_YAW, mc.rotationYaw);
                memory.set(ValueLayout.JAVA_FLOAT, base + BridgeComponents.TRANSFORM_PITCH, mc.rotationPitch);

                memory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_X, mc.motionX);
                memory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_Y, mc.motionY);
                memory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_Z, mc.motionZ);

                float speed = (float) Math.sqrt(mc.motionX * mc.motionX + mc.motionY * mc.motionY + mc.motionZ * mc.motionZ);
                memory.set(ValueLayout.JAVA_FLOAT, base + BridgeComponents.VELOCITY_SPEED, speed);
            }
        }
    }

    // ========================================================================
    // OUTBOUND SYNC (ECS -> MC)
    // ========================================================================

    /**
     * Synchronizes state from ECS component memory back to Minecraft entities.
     * Only writes dirty entities.
     *
     * @param slots     array of active slot indices
     * @param count     number of active slots
     * @param mcEntities array of Minecraft entity references
     */
    public void syncOutbound(int[] slots, int count, Entity[] mcEntities) {
        if (count == 0) return;

        if (count >= PARALLEL_THRESHOLD) {
            syncOutboundParallel(slots, count, mcEntities);
        } else {
            syncOutboundSequential(slots, count, mcEntities);
        }
    }

    private void syncOutboundSequential(int[] slots, int count, Entity[] mcEntities) {
        for (int i = 0; i < count; i++) {
            int slot = slots[i];
            Entity mc = mcEntities[slot];
            if (mc == null) continue;

            long base = (long) slot * entityBlockSize;

            // Check dirty flags
            long flags = BridgeComponents.getAndClearDirtyFlags(componentMemory, base);
            if (flags == 0) continue;

            // Write back dirty components
            if ((flags & BridgeComponents.FLAG_TRANSFORM_DIRTY) != 0) {
                mc.posX = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_X);
                mc.posY = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Y);
                mc.posZ = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Z);
                mc.rotationYaw = componentMemory.get(ValueLayout.JAVA_FLOAT, base + BridgeComponents.TRANSFORM_YAW);
                mc.rotationPitch = componentMemory.get(ValueLayout.JAVA_FLOAT, base + BridgeComponents.TRANSFORM_PITCH);

                // Update bounding box
                mc.setPosition(mc.posX, mc.posY, mc.posZ);
            }

            if ((flags & BridgeComponents.FLAG_VELOCITY_DIRTY) != 0) {
                mc.motionX = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_X);
                mc.motionY = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_Y);
                mc.motionZ = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_Z);
            }
        }
    }

    private void syncOutboundParallel(int[] slots, int count, Entity[] mcEntities) {
        pool.invoke(new OutboundSyncTask(slots, 0, count, mcEntities, componentMemory, entityBlockSize));
    }

    private static final class OutboundSyncTask extends RecursiveAction {
        private final int[] slots;
        private final int start;
        private final int end;
        private final Entity[] mcEntities;
        private final MemorySegment memory;
        private final int blockSize;

        OutboundSyncTask(int[] slots, int start, int end, Entity[] mcEntities, MemorySegment memory, int blockSize) {
            this.slots = slots;
            this.start = start;
            this.end = end;
            this.mcEntities = mcEntities;
            this.memory = memory;
            this.blockSize = blockSize;
        }

        @Override
        protected void compute() {
            int length = end - start;

            if (length <= PARALLEL_CHUNK_SIZE) {
                computeDirectly();
            } else {
                int mid = start + length / 2;
                invokeAll(
                        new OutboundSyncTask(slots, start, mid, mcEntities, memory, blockSize),
                        new OutboundSyncTask(slots, mid, end, mcEntities, memory, blockSize)
                );
            }
        }

        private void computeDirectly() {
            for (int i = start; i < end; i++) {
                int slot = slots[i];
                Entity mc = mcEntities[slot];
                if (mc == null) continue;

                long base = (long) slot * blockSize;

                long flags = BridgeComponents.getAndClearDirtyFlags(memory, base);
                if (flags == 0) continue;

                if ((flags & BridgeComponents.FLAG_TRANSFORM_DIRTY) != 0) {
                    mc.posX = memory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_X);
                    mc.posY = memory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Y);
                    mc.posZ = memory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Z);
                    mc.rotationYaw = memory.get(ValueLayout.JAVA_FLOAT, base + BridgeComponents.TRANSFORM_YAW);
                    mc.rotationPitch = memory.get(ValueLayout.JAVA_FLOAT, base + BridgeComponents.TRANSFORM_PITCH);
                    mc.setPosition(mc.posX, mc.posY, mc.posZ);
                }

                if ((flags & BridgeComponents.FLAG_VELOCITY_DIRTY) != 0) {
                    mc.motionX = memory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_X);
                    mc.motionY = memory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_Y);
                    mc.motionZ = memory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_Z);
                }
            }
        }
    }

    // ========================================================================
    // SIMD BATCH OPERATIONS
    // ========================================================================

    /**
     * Batch velocity integration using Vector API.
     * Updates positions based on velocities.
     *
     * @param slots     active slot indices
     * @param count     number of slots
     * @param deltaTime time step in seconds
     */
    public void integrateVelocities(int[] slots, int count, float deltaTime) {
        double dt = deltaTime;

        int vectorized = (count / VECTOR_LENGTH) * VECTOR_LENGTH;

        // Vectorized loop (processes VECTOR_LENGTH entities at once)
        for (int i = 0; i < vectorized; i += VECTOR_LENGTH) {
            // Load positions and velocities for VECTOR_LENGTH entities
            double[] px = new double[VECTOR_LENGTH];
            double[] py = new double[VECTOR_LENGTH];
            double[] pz = new double[VECTOR_LENGTH];
            double[] vx = new double[VECTOR_LENGTH];
            double[] vy = new double[VECTOR_LENGTH];
            double[] vz = new double[VECTOR_LENGTH];

            for (int j = 0; j < VECTOR_LENGTH; j++) {
                int slot = slots[i + j];
                long base = (long) slot * entityBlockSize;

                px[j] = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_X);
                py[j] = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Y);
                pz[j] = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Z);
                vx[j] = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_X);
                vy[j] = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_Y);
                vz[j] = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_Z);
            }

            // SIMD: newPos = pos + vel * dt
            DoubleVector pxv = DoubleVector.fromArray(SPECIES, px, 0);
            DoubleVector pyv = DoubleVector.fromArray(SPECIES, py, 0);
            DoubleVector pzv = DoubleVector.fromArray(SPECIES, pz, 0);
            DoubleVector vxv = DoubleVector.fromArray(SPECIES, vx, 0);
            DoubleVector vyv = DoubleVector.fromArray(SPECIES, vy, 0);
            DoubleVector vzv = DoubleVector.fromArray(SPECIES, vz, 0);

            pxv = pxv.add(vxv.mul(dt));
            pyv = pyv.add(vyv.mul(dt));
            pzv = pzv.add(vzv.mul(dt));

            // Store results back
            double[] newPx = new double[VECTOR_LENGTH];
            double[] newPy = new double[VECTOR_LENGTH];
            double[] newPz = new double[VECTOR_LENGTH];

            pxv.intoArray(newPx, 0);
            pyv.intoArray(newPy, 0);
            pzv.intoArray(newPz, 0);

            for (int j = 0; j < VECTOR_LENGTH; j++) {
                int slot = slots[i + j];
                long base = (long) slot * entityBlockSize;

                componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_X, newPx[j]);
                componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Y, newPy[j]);
                componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Z, newPz[j]);

                BridgeComponents.setFlags(componentMemory, base, BridgeComponents.FLAG_TRANSFORM_DIRTY);
            }
        }

        // Scalar remainder
        for (int i = vectorized; i < count; i++) {
            int slot = slots[i];
            long base = (long) slot * entityBlockSize;

            double x = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_X);
            double y = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Y);
            double z = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Z);
            double vxVal = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_X);
            double vyVal = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_Y);
            double vzVal = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_Z);

            componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_X, x + vxVal * dt);
            componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Y, y + vyVal * dt);
            componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Z, z + vzVal * dt);

            BridgeComponents.setFlags(componentMemory, base, BridgeComponents.FLAG_TRANSFORM_DIRTY);
        }
    }
}
