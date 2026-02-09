package stellar.snow.astralis.nexus.debug;

import stellar.snow.astralis.api.common.GLBufferOpsBase;

/**
 * F3DebugRenderer - Helper for Snowium Render debug display in MC F3 screen.
 * 
 * Provides formatted strings with proper colors for the debug overlay.
 * 
 * Display format:
 * ┌────────────────────────────────────────────────────────────────┐
 * │ [FF] Snowium Render: GL 4.6 (DSA)                              │
 * │      ^^^^^^^^        ^^^^^^                                    │
 * │      Glowing         Version-colored                           │
 * └────────────────────────────────────────────────────────────────┘
 */
public final class F3DebugRenderer {
    
    private F3DebugRenderer() {}
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Color formatting for MC chat/debug (§ codes)
    // ─────────────────────────────────────────────────────────────────────────────
    
    // Minecraft color codes
    public static final char COLOR_CHAR = '\u00A7';
    
    public static final String RESET = COLOR_CHAR + "r";
    public static final String BOLD = COLOR_CHAR + "l";
    public static final String ITALIC = COLOR_CHAR + "o";
    
    // Basic colors
    public static final String WHITE = COLOR_CHAR + "f";
    public static final String GRAY = COLOR_CHAR + "7";
    public static final String DARK_GRAY = COLOR_CHAR + "8";
    public static final String GOLD = COLOR_CHAR + "6";
    public static final String YELLOW = COLOR_CHAR + "e";
    public static final String GREEN = COLOR_CHAR + "a";
    public static final String AQUA = COLOR_CHAR + "b";
    public static final String BLUE = COLOR_CHAR + "9";
    public static final String LIGHT_PURPLE = COLOR_CHAR + "d";
    public static final String RED = COLOR_CHAR + "c";
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Snowium branding colors (approximations using MC color codes)
    // ─────────────────────────────────────────────────────────────────────────────
    
    // Snowium: Icy cyan glow - use AQUA + BOLD
    public static final String SNOWIUM_STYLE = AQUA + BOLD;
    
    // Astralis: Orange energy - use GOLD + BOLD
    public static final String ASTRALIS_STYLE = GOLD + BOLD;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Header Generation
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Get the Snowium Render header for F3 display.
     * 
     * @return Formatted header: "[FF] Snowium Render:"
     */
    public static String getHeader() {
        return ASTRALIS_STYLE + "[" + GLBufferOpsBase.MOD_SHORT + "] " + 
               SNOWIUM_STYLE + GLBufferOpsBase.RENDER_NAME + " Render:" + RESET;
    }
    
    /**
     * Get the full debug line for F3 display.
     * 
     * @param glVersionCode The GL version code (e.g., 46, 33, 21)
     * @param versionName Human-readable name (e.g., "OpenGL 4.6 (DSA)")
     * @return Fully formatted debug line
     */
    public static String getDebugLine(int glVersionCode, String versionName) {
        String versionColor = getColorCodeForVersion(glVersionCode);
        return getHeader() + " " + versionColor + versionName + RESET;
    }
    
    /**
     * Get short debug line (compact format).
     */
    public static String getShortDebugLine(int glVersionCode) {
        String shortVer = formatVersionShort(glVersionCode);
        String versionColor = getColorCodeForVersion(glVersionCode);
        return ASTRALIS_STYLE + "[FF]" + RESET + " " + 
               SNOWIUM_STYLE + "Snowium" + RESET + ": " +
               versionColor + "GL " + shortVer + RESET;
    }
    
    /**
     * Get extended debug info (multiple lines).
     */
    public static String[] getExtendedDebugLines(int glVersionCode, String versionName,
                                                   boolean hasDSA, boolean hasPersistent,
                                                   boolean hasMDI, boolean cacheEnabled) {
        String vColor = getColorCodeForVersion(glVersionCode);
        return new String[] {
            getHeader() + " " + vColor + versionName + RESET,
            GRAY + "  DSA: " + formatBool(hasDSA) + 
                   "  Persistent: " + formatBool(hasPersistent) +
                   "  MDI: " + formatBool(hasMDI) + RESET,
            GRAY + "  State Cache: " + formatBool(cacheEnabled) + RESET
        };
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Convert GL version code to short string.
     */
    public static String formatVersionShort(int code) {
        if (code == 121) return "1.2.1";
        return (code / 10) + "." + (code % 10);
    }
    
    /**
     * Get MC color code for GL version.
     */
    public static String getColorCodeForVersion(int glVersionCode) {
        // Map version to closest MC color
        if (glVersionCode < 15) return DARK_GRAY;  // No VBO
        if (glVersionCode < 20) return GOLD;       // Bronze era
        if (glVersionCode == 21) return YELLOW;    // Gold - MC minimum
        if (glVersionCode < 33) return GREEN;      // Lime green era
        if (glVersionCode == 33) return GREEN;     // Sweet spot
        if (glVersionCode < 43) return AQUA;       // Cyan-blue era
        if (glVersionCode < 45) return BLUE;       // Deep blue era
        if (glVersionCode == 45) return LIGHT_PURPLE; // DSA - violet
        return LIGHT_PURPLE + BOLD;                // 4.6 - magenta/bold
    }
    
    /**
     * Format boolean for display.
     */
    private static String formatBool(boolean val) {
        return val ? (GREEN + "✓" + RESET) : (RED + "✗" + RESET);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // RGB Color helpers (for custom rendering if needed)
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Get ARGB color for GL version (for custom font rendering).
     */
    public static int getARGBForVersion(int glVersionCode) {
        return 0xFF000000 | GLBufferOpsBase.getColorForVersion(glVersionCode);
    }
    
    /**
     * Get Snowium brand color (ARGB).
     */
    public static int getSnowiumColor() {
        return 0xFF000000 | GLBufferOpsBase.SNOWIUM_COLOR;
    }
    
    /**
     * Get Astralis brand color (ARGB).
     */
    public static int getAstralisColor() {
        return 0xFF000000 | GLBufferOpsBase.ASTRALIS_COLOR;
    }
    
    /**
     * Create pulsing glow effect value (0.0 to 1.0).
     * Call each frame with gameTime for animation.
     */
    public static float getGlowPulse(long gameTime) {
        // Smooth sine wave, 2 second period
        double phase = (gameTime % 2000L) / 2000.0 * Math.PI * 2.0;
        return (float) (0.5 + 0.5 * Math.sin(phase));
    }
    
    /**
     * Interpolate color for glow effect.
     */
    public static int interpolateColor(int baseColor, int glowColor, float t) {
        int bR = (baseColor >> 16) & 0xFF;
        int bG = (baseColor >> 8) & 0xFF;
        int bB = baseColor & 0xFF;
        
        int gR = (glowColor >> 16) & 0xFF;
        int gG = (glowColor >> 8) & 0xFF;
        int gB = glowColor & 0xFF;
        
        int r = (int) (bR + (gR - bR) * t);
        int g = (int) (bG + (gG - bG) * t);
        int b = (int) (bB + (gB - bB) * t);
        
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
