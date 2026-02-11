package stellar.snow.astralis.debug;

import stellar.snow.astralis.api.common.GLBuffAerOpsBase;

/**
 * F3DebugRenderer - Helper for Snowium Render debug display in MC F3 screen.
 * 
 * Provides formatted strings with BEAUTIFUL explicit colors for the debug overlay.
 * Every API version gets its own unique, gorgeous color!
 * 
 * Best versions use FEMININE colors: HOT PINK, LAVENDER, MAGENTA, ORCHID!
 * 
 * Display format:
 * ┌────────────────────────────────────────────────────────────────┐
 * │ [A] Snowium Render: Vulkan 1.4 / SPIR-V 1.6                   │
 * │      ^^^^^^^^        ^^^^^^^^^^^^^^^^^^^^^^^^^                 │
 * │      Glowing         HOT PINK! (best versions)                 │
 * └────────────────────────────────────────────────────────────────┘
 * 
 * @author Astralis Team
 */
public final class F3DebugRenderer {
    
    private F3DebugRenderer() {}
    
    // Ensure colors are registered
    static {
        MinecraftColorRegistry.register();
    }
    
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
    public static final String GREEN = COLOR_CHAR + "a";
    public static final String RED = COLOR_CHAR + "c";
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Snowium branding colors
    // ─────────────────────────────────────────────────────────────────────────────
    
    // Snowium: Icy cyan glow
    public static final String SNOWIUM_STYLE = MinecraftColorRegistry.getColorCode("SNOWIUM_CYAN") + BOLD;
    
    // Astralis: Orange energy
    public static final String ASTRALIS_STYLE = MinecraftColorRegistry.getColorCode("ASTRALIS_ORANGE") + BOLD;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // API Type Enum
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Supported Graphics API types
     */
    public enum APIType {
        OPENGL("OpenGL"),
        OPENGL_ES("OpenGL ES"),
        VULKAN("Vulkan"),
        DIRECTX("DirectX"),
        METAL("Metal"),
        GLSL("GLSL"),
        GLSL_ES("GLSL ES"),
        HLSL("HLSL"),
        MSL("MSL"),
        SPIRV("SPIR-V");
        
        public final String displayName;
        
        APIType(String displayName) {
            this.displayName = displayName;
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Header Generation
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Get the Snowium Render header for F3 display.
     * 
     * @return Formatted header: "[A] Snowium Render:"
     */
    public static String getHeader() {
        return ASTRALIS_STYLE + "[" + GLBufferOpsBase.MOD_SHORT + "] " + 
               SNOWIUM_STYLE + GLBufferOpsBase.RENDER_NAME + " Render:" + RESET;
    }
    
    /**
     * Get the full debug line for F3 display with Graphics API only.
     * 
     * @param apiType The graphics API type
     * @param versionMajor Major version number
     * @param versionMinor Minor version number
     * @return Fully formatted debug line
     */
    public static String getDebugLine(APIType apiType, int versionMajor, int versionMinor) {
        return getDebugLine(apiType, versionMajor, versionMinor, null, 0, 0);
    }
    
    /**
     * Get the full debug line for F3 display with Graphics API and Shader Language.
     * 
     * @param apiType The graphics API type
     * @param apiMajor API major version
     * @param apiMinor API minor version
     * @param shaderType The shader language type (can be null)
     * @param shaderMajor Shader major version
     * @param shaderMinor Shader minor version
     * @return Fully formatted debug line
     */
    public static String getDebugLine(APIType apiType, int apiMajor, int apiMinor,
                                       APIType shaderType, int shaderMajor, int shaderMinor) {
        String apiColor = getColorForAPI(apiType, apiMajor, apiMinor);
        String apiStr = formatVersion(apiType, apiMajor, apiMinor);
        
        if (shaderType == null) {
            return getHeader() + " " + apiColor + apiStr + RESET;
        }
        
        String shaderColor = getColorForAPI(shaderType, shaderMajor, shaderMinor);
        String shaderStr = formatVersion(shaderType, shaderMajor, shaderMinor);
        
        return getHeader() + " " + apiColor + apiStr + GRAY + " / " + shaderColor + shaderStr + RESET;
    }
    
    /**
     * Get short debug line (compact format).
     */
    public static String getShortDebugLine(APIType apiType, int major, int minor) {
        String apiColor = getColorForAPI(apiType, major, minor);
        String shortVer = apiType.displayName + " " + major + "." + minor;
        return ASTRALIS_STYLE + "[A]" + RESET + " " + 
               SNOWIUM_STYLE + "Snowium" + RESET + ": " +
               apiColor + shortVer + RESET;
    }
    
    /**
     * Get extended debug info (multiple lines).
     */
    public static String[] getExtendedDebugLines(APIType apiType, int apiMajor, int apiMinor,
                                                   APIType shaderType, int shaderMajor, int shaderMinor,
                                                   boolean hasDSA, boolean hasPersistent,
                                                   boolean hasMDI, boolean cacheEnabled) {
        String mainLine = getDebugLine(apiType, apiMajor, apiMinor, shaderType, shaderMajor, shaderMinor);
        
        return new String[] {
            mainLine,
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
     * Format API version to display string.
     */
    public static String formatVersion(APIType apiType, int major, int minor) {
        return apiType.displayName + " " + major + "." + minor;
    }
    
    /**
     * Get beautiful explicit color for any API version!
     * Each version gets its own gorgeous color.
     * Best versions get FEMININE colors! 💅✨
     */
    public static String getColorForAPI(APIType apiType, int major, int minor) {
        switch (apiType) {
            case OPENGL:
                return getColorForOpenGL(major, minor);
            case OPENGL_ES:
                return getColorForOpenGLES(major, minor);
            case VULKAN:
                return getColorForVulkan(major, minor);
            case DIRECTX:
                return getColorForDirectX(major, minor);
            case METAL:
                return getColorForMetal(major, minor);
            case GLSL:
                return getColorForGLSL(major, minor);
            case GLSL_ES:
                return getColorForGLSLES(major, minor);
            case HLSL:
                return getColorForHLSL(major, minor);
            case MSL:
                return getColorForMSL(major, minor);
            case SPIRV:
                return getColorForSPIRV(major, minor);
            default:
                return WHITE;
        }
    }
    
    /**
     * OpenGL colors: Green/Blue → LAVENDER/HOT PINK for 4.5-4.6! 💜💕
     */
    private static String getColorForOpenGL(int major, int minor) {
        String colorName;
        
        if (major == 1) {
            switch (minor) {
                case 0: colorName = "OPENGL_1_0"; break;
                case 1: colorName = "OPENGL_1_1"; break;
                case 2: colorName = "OPENGL_1_2"; break;
                case 3: colorName = "OPENGL_1_3"; break;
                case 4: colorName = "OPENGL_1_4"; break;
                case 5: colorName = "OPENGL_1_5"; break;
                default: colorName = "OPENGL_1_5";
            }
        } else if (major == 2) {
            colorName = minor == 0 ? "OPENGL_2_0" : "OPENGL_2_1";
        } else if (major == 3) {
            switch (minor) {
                case 0: colorName = "OPENGL_3_0"; break;
                case 1: colorName = "OPENGL_3_1"; break;
                case 2: colorName = "OPENGL_3_2"; break;
                default: colorName = "OPENGL_3_3";
            }
        } else if (major == 4) {
            switch (minor) {
                case 0: colorName = "OPENGL_4_0"; break;
                case 1: colorName = "OPENGL_4_1"; break;
                case 2: colorName = "OPENGL_4_2"; break;
                case 3: colorName = "OPENGL_4_3"; break;
                case 4: colorName = "OPENGL_4_4"; break;
                case 5: colorName = "OPENGL_4_5"; break; // LAVENDER! 💜
                default: colorName = "OPENGL_4_6"; // HOT PINK! 💕
            }
        } else {
            colorName = "OPENGL_4_6";
        }
        
        return MinecraftColorRegistry.getColorCode(colorName);
    }
    
    /**
     * OpenGL ES colors: Bronze/Green → Blue
     */
    private static String getColorForOpenGLES(int major, int minor) {
        String colorName;
        
        if (major == 2) {
            colorName = "OPENGL_ES_2_0";
        } else if (major == 3) {
            switch (minor) {
                case 0: colorName = "OPENGL_ES_3_0"; break;
                case 1: colorName = "OPENGL_ES_3_1"; break;
                default: colorName = "OPENGL_ES_3_2";
            }
        } else {
            colorName = "OPENGL_ES_3_2";
        }
        
        return MinecraftColorRegistry.getColorCode(colorName);
    }
    
    /**
     * Vulkan colors: Red → ORCHID/HOT PINK for 1.3-1.4! 🌸💕
     */
    private static String getColorForVulkan(int major, int minor) {
        String colorName;
        
        if (major == 1) {
            switch (minor) {
                case 0: colorName = "VULKAN_1_0"; break;
                case 1: colorName = "VULKAN_1_1"; break;
                case 2: colorName = "VULKAN_1_2"; break;
                case 3: colorName = "VULKAN_1_3"; break; // ORCHID! 🌸
                default: colorName = "VULKAN_1_4"; // HOT PINK! 💕
            }
        } else {
            colorName = "VULKAN_1_4";
        }
        
        return MinecraftColorRegistry.getColorCode(colorName);
    }
    
    /**
     * DirectX colors: Blue → VIOLET/MAGENTA for 12.1-12.2! 💜💖
     */
    private static String getColorForDirectX(int major, int minor) {
        String colorName;
        
        if (major == 9) {
            colorName = "DIRECTX_9";
        } else if (major == 10) {
            colorName = "DIRECTX_10";
        } else if (major == 11) {
            colorName = "DIRECTX_11";
        } else if (major == 12) {
            if (minor == 0) {
                colorName = "DIRECTX_12_0";
            } else if (minor == 1) {
                colorName = "DIRECTX_12_1"; // VIOLET! 💜
            } else {
                colorName = "DIRECTX_12_2"; // MAGENTA! 💖
            }
        } else {
            colorName = "DIRECTX_12_2";
        }
        
        return MinecraftColorRegistry.getColorCode(colorName);
    }
    
    /**
     * Metal colors: Silver/Blue → ROSE PINK/LAVENDER for 3.1-3.2! 🌹💜
     */
    private static String getColorForMetal(int major, int minor) {
        String colorName;
        
        if (major == 1) {
            switch (minor) {
                case 0: colorName = "METAL_1_0"; break;
                case 1: colorName = "METAL_1_1"; break;
                default: colorName = "METAL_1_2";
            }
        } else if (major == 2) {
            switch (minor) {
                case 0: colorName = "METAL_2_0"; break;
                case 1: colorName = "METAL_2_1"; break;
                default: colorName = "METAL_2_2";
            }
        } else if (major == 3) {
            switch (minor) {
                case 0: colorName = "METAL_3_0"; break;
                case 1: colorName = "METAL_3_1"; break; // ROSE PINK! 🌹
                default: colorName = "METAL_3_2"; // LAVENDER! 💜
            }
        } else {
            colorName = "METAL_3_2";
        }
        
        return MinecraftColorRegistry.getColorCode(colorName);
    }
    
    /**
     * GLSL colors: Green/Blue → MEDIUM PURPLE/DEEP PINK for 450-460! 💜💕
     */
    private static String getColorForGLSL(int major, int minor) {
        int version = major * 100 + minor;
        String colorName;
        
        if (version <= 110) colorName = "GLSL_110";
        else if (version <= 120) colorName = "GLSL_120";
        else if (version <= 130) colorName = "GLSL_130";
        else if (version <= 140) colorName = "GLSL_140";
        else if (version <= 150) colorName = "GLSL_150";
        else if (version <= 330) colorName = "GLSL_330";
        else if (version <= 400) colorName = "GLSL_400";
        else if (version <= 410) colorName = "GLSL_410";
        else if (version <= 420) colorName = "GLSL_420";
        else if (version <= 430) colorName = "GLSL_430";
        else if (version <= 440) colorName = "GLSL_440";
        else if (version <= 450) colorName = "GLSL_450"; // MEDIUM PURPLE! 💜
        else colorName = "GLSL_460"; // DEEP PINK! 💕
        
        return MinecraftColorRegistry.getColorCode(colorName);
    }
    
    /**
     * GLSL ES colors: Brown/Green → Blue
     */
    private static String getColorForGLSLES(int major, int minor) {
        int version = major * 100 + minor;
        String colorName;
        
        if (version <= 100) colorName = "GLSL_ES_100";
        else if (version <= 300) colorName = "GLSL_ES_300";
        else if (version <= 310) colorName = "GLSL_ES_310";
        else colorName = "GLSL_ES_320";
        
        return MinecraftColorRegistry.getColorCode(colorName);
    }
    
    /**
     * HLSL colors: Blue → ORCHID/MAGENTA/HOT PINK for 6.6-6.8! 🌸💖💕
     */
    private static String getColorForHLSL(int major, int minor) {
        String colorName;
        
        if (major <= 1) colorName = "HLSL_1_0";
        else if (major <= 2) colorName = "HLSL_2_0";
        else if (major <= 3) colorName = "HLSL_3_0";
        else if (major <= 4) colorName = "HLSL_4_0";
        else if (major <= 5) colorName = "HLSL_5_0";
        else if (major == 6) {
            if (minor == 0) colorName = "HLSL_6_0";
            else if (minor == 1) colorName = "HLSL_6_1";
            else if (minor == 2) colorName = "HLSL_6_2";
            else if (minor == 3) colorName = "HLSL_6_3";
            else if (minor == 4) colorName = "HLSL_6_4";
            else if (minor == 5) colorName = "HLSL_6_5";
            else if (minor == 6) colorName = "HLSL_6_6"; // ORCHID! 🌸
            else if (minor == 7) colorName = "HLSL_6_7"; // MAGENTA! 💖
            else colorName = "HLSL_6_8"; // HOT PINK! 💕
        } else {
            colorName = "HLSL_6_8";
        }
        
        return MinecraftColorRegistry.getColorCode(colorName);
    }
    
    /**
     * MSL colors: Silver/Blue → PINK/LAVENDER for 3.1-3.2! 💗💜
     */
    private static String getColorForMSL(int major, int minor) {
        String colorName;
        
        if (major == 1) {
            switch (minor) {
                case 0: colorName = "MSL_1_0"; break;
                case 1: colorName = "MSL_1_1"; break;
                default: colorName = "MSL_1_2";
            }
        } else if (major == 2) {
            switch (minor) {
                case 0: colorName = "MSL_2_0"; break;
                case 1: colorName = "MSL_2_1"; break;
                default: colorName = "MSL_2_2";
            }
        } else if (major == 3) {
            switch (minor) {
                case 0: colorName = "MSL_3_0"; break;
                case 1: colorName = "MSL_3_1"; break; // PINK! 💗
                default: colorName = "MSL_3_2"; // LAVENDER! 💜
            }
        } else {
            colorName = "MSL_3_2";
        }
        
        return MinecraftColorRegistry.getColorCode(colorName);
    }
    
    /**
     * SPIR-V colors: Red → MAGENTA/HOT PINK for 1.5-1.6! 💖💕
     */
    private static String getColorForSPIRV(int major, int minor) {
        String colorName;
        
        if (major == 1) {
            switch (minor) {
                case 0: colorName = "SPIRV_1_0"; break;
                case 1: colorName = "SPIRV_1_1"; break;
                case 2: colorName = "SPIRV_1_2"; break;
                case 3: colorName = "SPIRV_1_3"; break;
                case 4: colorName = "SPIRV_1_4"; break;
                case 5: colorName = "SPIRV_1_5"; break; // MAGENTA! 💖
                default: colorName = "SPIRV_1_6"; // HOT PINK! 💕
            }
        } else {
            colorName = "SPIRV_1_6";
        }
        
        return MinecraftColorRegistry.getColorCode(colorName);
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
     * Get ARGB color for any API.
     */
    public static int getARGBForAPI(APIType apiType, int major, int minor) {
        return CustomColors.toARGB(getRGBForAPI(apiType, major, minor));
    }
    
    /**
     * Get RGB color value for any API.
     */
    public static int getRGBForAPI(APIType apiType, int major, int minor) {
        String colorName = getColorNameForAPI(apiType, major, minor);
        return MinecraftColorRegistry.getRGB(colorName);
    }
    
    /**
     * Get color name for API version.
     */
    private static String getColorNameForAPI(APIType apiType, int major, int minor) {
        // Extract color name from the color code
        String colorCode = getColorForAPI(apiType, major, minor);
        
        // Simple lookup based on API type and version
        switch (apiType) {
            case OPENGL:
                if (major == 4 && minor == 6) return "OPENGL_4_6";
                if (major == 4 && minor == 5) return "OPENGL_4_5";
                break;
            case VULKAN:
                if (major == 1 && minor == 4) return "VULKAN_1_4";
                if (major == 1 && minor == 3) return "VULKAN_1_3";
                break;
            case DIRECTX:
                if (major == 12 && minor == 2) return "DIRECTX_12_2";
                if (major == 12 && minor == 1) return "DIRECTX_12_1";
                break;
            case SPIRV:
                if (major == 1 && minor == 6) return "SPIRV_1_6";
                if (major == 1 && minor == 5) return "SPIRV_1_5";
                break;
        }
        
        return "HOT_PINK"; // Default to hot pink because why not! 💕
    }
    
    /**
     * Get Snowium brand color (ARGB).
     */
    public static int getSnowiumColor() {
        return CustomColors.toARGB(CustomColors.SNOWIUM_CYAN);
    }
    
    /**
     * Get Astralis brand color (ARGB).
     */
    public static int getAstralisColor() {
        return CustomColors.toARGB(CustomColors.ASTRALIS_ORANGE);
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
        return CustomColors.lerp(baseColor, glowColor, t);
    }
}
