package stellar.snow.astralis.engine.render.shadows;

public final class MobileShadowOptimizer {
    public static void optimizeForLowTier(AdvancedShadowSystem system) {
        system.setCascadeCount(2);
        system.setShadowTechnique(ShadowTechnique.PCF);
    }
}
