package stellar.snow.astralis.engine.render.shadows;
    public static void optimizeForLowTier(AdvancedShadowSystem system) {
        system.setCascadeCount(2);
        system.setShadowTechnique(ShadowTechnique.PCF);
    }
}
