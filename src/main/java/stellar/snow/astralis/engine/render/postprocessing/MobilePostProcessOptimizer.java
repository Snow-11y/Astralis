package stellar.snow.astralis.engine.render.postprocessing;

public final class MobilePostProcessOptimizer {
    public static void optimizeForMali(PostProcessingSystem system) {
        system.config.bloomMipLevels = 5;
        system.config.ssrMaxSteps = 32;
        system.config.dofBokehSamples = 16;
    }
    
    public static void optimizeForAdreno(PostProcessingSystem system) {
        system.config.bloomMipLevels = 6;
        system.config.ssrMaxSteps = 48;
        system.config.useFP16WherePossible = true;
    }
}
