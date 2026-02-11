package stellar.snow.astralis.bridge;

import stellar.snow.astralis.engine.ecs.core.Archetype;
import stellar.snow.astralis.engine.ecs.core.SnowySystem;
import stellar.snow.astralis.engine.ecs.core.World;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * InterpolationSystem - Handles smooth transform interpolation for rendering.
 */
public final class InterpolationSystem extends SnowySystem {

    private final MinecraftECSBridge bridge;
    private volatile float currentInterpolationFactor = 0.0f;

    public InterpolationSystem(MinecraftECSBridge bridge) {
        super("Bridge_Interpolation");
        this.bridge = bridge;
    }

    @Override
    public void update(World world, float partialTicks) {
        this.currentInterpolationFactor = Math.clamp(partialTicks, 0.0f, 1.0f);
        // Actual interpolation is performed on-demand via getInterpolatedTransform()
    }

    @Override
    public void update(World world, Archetype archetype, float deltaTime) {
        // Not used
    }

    public float getCurrentInterpolationFactor() {
        return currentInterpolationFactor;
    }
}
