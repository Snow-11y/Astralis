package stellar.snow.astralis.bridge;

import stellar.snow.astralis.engine.ecs.core.Archetype;
import stellar.snow.astralis.engine.ecs.core.SnowySystem;
import stellar.snow.astralis.engine.ecs.core.SystemScheduler;
import stellar.snow.astralis.engine.ecs.core.World;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * SyncSystems - ECS systems for Minecraft entity synchronization.
 */
public final class SyncSystems {

    private static final Logger LOGGER = Logger.getLogger(SyncSystems.class.getName());

    private SyncSystems() {}

    // ========================================================================
    // INBOUND SYNC SYSTEM
    // ========================================================================

    /**
     * Syncs state from Minecraft entities to ECS components (PRE_UPDATE).
     */
    public static final class InboundSyncSystem extends SnowySystem {

        private final MinecraftECSBridge bridge;
        private final AtomicLong syncCount = new AtomicLong(0);

        public InboundSyncSystem(MinecraftECSBridge bridge) {
            super("Bridge_InboundSync");
            this.bridge = bridge;
        }

        @Override
        public void update(World world, float deltaTime) {
            // Batch processing is handled by the bridge's tick pipeline
            // This system is a marker for scheduling purposes
            syncCount.incrementAndGet();
        }

        @Override
        public void update(World world, Archetype archetype, float deltaTime) {
            // Not used - we process at the bridge level for better batching
        }

        public long getSyncCount() {
            return syncCount.get();
        }
    }

    // ========================================================================
    // OUTBOUND SYNC SYSTEM
    // ========================================================================

    /**
     * Syncs state from ECS components back to Minecraft entities (POST_UPDATE).
     */
    public static final class OutboundSyncSystem extends SnowySystem {

        private final MinecraftECSBridge bridge;
        private final AtomicLong writeCount = new AtomicLong(0);
        private final AtomicLong lastProcessedVersion = new AtomicLong(0);

        public OutboundSyncSystem(MinecraftECSBridge bridge) {
            super("Bridge_OutboundSync");
            this.bridge = bridge;
        }

        @Override
        public void update(World world, float deltaTime) {
            // Batch processing handled at bridge level
            writeCount.incrementAndGet();
        }

        @Override
        public void update(World world, Archetype archetype, float deltaTime) {
            // Not used
        }

        @Override
        protected void onAfterUpdate(World world, float deltaTime) {
            lastProcessedVersion.set(world.getStatistics().componentOperations());
        }

        public long getWriteCount() {
            return writeCount.get();
        }
    }
}
