package stellar.snow.astralis.engine.render.vrs;
/**
 * Adaptive shading rate calculation for Variable Rate Shading
 * Determines optimal shading rate based on visual perception factors
 */
    
    // VRS rates (Vulkan/DX12)
    public static final int RATE_1X1 = 0;  // Full resolution
    public static final int RATE_1X2 = 1;  // Half vertical
    public static final int RATE_2X1 = 2;  // Half horizontal
    public static final int RATE_2X2 = 3;  // Quarter resolution
    public static final int RATE_2X4 = 4;  // 1/8 resolution
    public static final int RATE_4X2 = 5;
    public static final int RATE_4X4 = 6;  // 1/16 resolution
    
    // Thresholds for rate selection
    private float velocityThresholdHigh = 2.0f;   // pixels/frame
    private float velocityThresholdMedium = 1.0f;
    private float depthThresholdFar = 100.0f;     // units
    private float depthThresholdMid = 50.0f;
    private float luminanceThresholdDark = 0.1f;   // 0-1 range
    private float contrastThreshold = 0.05f;       // Edge detection
    
    /**
     * Calculate optimal shading rate for a pixel based on multiple factors
     */
    public int calculateShadingRate(float velocity, float depth, float luminance) {
        return calculateShadingRate(velocity, depth, luminance, 0.0f, false);
    }
    
    /**
     * Full rate calculation with all perception factors
     */
    public int calculateShadingRate(float velocity, float depth, float luminance, 
                                    float contrast, boolean isFoveated) {
        
        // Start with full rate
        int rate = RATE_1X1;
        
        // Factor 1: Motion - high motion areas can use lower rates
        // Human eye has reduced acuity during fast motion
        if (velocity > velocityThresholdHigh) {
            rate = RATE_4X4;  // Very high motion
        } else if (velocity > velocityThresholdMedium) {
            rate = RATE_2X2;  // Medium motion
        }
        
        // Factor 2: Depth - distant objects can use lower rates
        // Reduced detail is less noticeable at distance
        if (depth > depthThresholdFar) {
            rate = Math.max(rate, RATE_2X2);
        } else if (depth > depthThresholdMid) {
            rate = Math.max(rate, RATE_1X2);
        }
        
        // Factor 3: Luminance - dark areas can use lower rates
        // Reduced visual acuity in low light
        if (luminance < luminanceThresholdDark) {
            rate = Math.max(rate, RATE_2X2);
        }
        
        // Factor 4: Contrast - high contrast edges need full rate
        // Preserve sharp edges and details
        if (contrast > contrastThreshold) {
            rate = RATE_1X1;  // Override all other factors
        }
        
        // Factor 5: Foveated rendering - peripheral vision needs less detail
        if (isFoveated) {
            // Already reduced in peripheral areas
            rate = Math.max(rate, RATE_2X2);
        }
        
        return rate;
    }
    
    /**
     * Calculate rate for foveated rendering based on eccentricity
     * (distance from gaze point in visual degrees)
     */
    public int calculateFoveatedRate(float eccentricity) {
        // Human fovea: ~2° high acuity
        // Parafovea: 2-8° medium acuity
        // Periphery: >8° low acuity
        
        if (eccentricity < 2.0f) {
            return RATE_1X1;  // Foveal region - full detail
        } else if (eccentricity < 5.0f) {
            return RATE_1X2;  // Near peripheral
        } else if (eccentricity < 10.0f) {
            return RATE_2X2;  // Mid peripheral
        } else if (eccentricity < 20.0f) {
            return RATE_2X4;  // Far peripheral
        } else {
            return RATE_4X4;  // Extreme peripheral
        }
    }
    
    /**
     * Combine multiple rates (take the most conservative)
     */
    public int combineRates(int rate1, int rate2) {
        return Math.min(rate1, rate2);  // Lower number = higher quality
    }
    
    /**
     * Get performance benefit estimation (percentage of shading work saved)
     */
    public float getPerformanceBenefit(int rate) {
        return switch (rate) {
            case RATE_1X1 -> 0.0f;   // No savings
            case RATE_1X2, RATE_2X1 -> 0.5f;   // 50% savings
            case RATE_2X2 -> 0.75f;  // 75% savings
            case RATE_2X4, RATE_4X2 -> 0.875f; // 87.5% savings
            case RATE_4X4 -> 0.9375f; // 93.75% savings
            default -> 0.0f;
        };
    }
    
    // Setters for threshold tuning
    public void setVelocityThresholds(float medium, float high) {
        this.velocityThresholdMedium = medium;
        this.velocityThresholdHigh = high;
    }
    
    public void setDepthThresholds(float mid, float far) {
        this.depthThresholdMid = mid;
        this.depthThresholdFar = far;
    }
    
    public void setLuminanceThreshold(float dark) {
        this.luminanceThresholdDark = dark;
    }
    
    public void setContrastThreshold(float threshold) {
        this.contrastThreshold = threshold;
    }
}
