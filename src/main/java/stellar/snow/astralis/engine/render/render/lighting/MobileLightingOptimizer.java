package stellar.snow.astralis.engine.render.lighting;

public final class MobileLightingOptimizer {
    public static void applyLowTierOptimizations(AdvancedLightingSystem system) {
        // Reduce probe count, use light probe fallback
        system.setGITechnique(GITechnique.LIGHT_PROBES);
    }
}
