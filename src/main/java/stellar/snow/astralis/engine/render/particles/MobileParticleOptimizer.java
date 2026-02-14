package stellar.snow.astralis.engine.render.particles;
    public static int getMaxParticlesForTier(String gpuName) {
        if (gpuName.contains("mali-t") || gpuName.contains("adreno 5")) {
            return 100_000; // Low-tier
        } else if (gpuName.contains("mali-g") || gpuName.contains("adreno 6")) {
            return 500_000; // Mid-tier
        }
        return 1_000_000; // High-tier
    }
}
