package stellar.snow.astralis.debug;

/**
 * CustomColors - Beautiful color palette for Snowium Render debug display.
 * 
 * Defines explicit RGB colors for each API version with feminine colors
 * for the best/latest versions (hot pink, lavender, magenta, etc.)
 * 
 * Color Philosophy:
 * - Each API has a base theme color
 * - Progression from darker to brighter
 * - Best versions use feminine colors (hot pink, lavender, magenta, pink)
 * 
 * @author Astralis Team
 */
public final class CustomColors {
    
    private CustomColors() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CORE BRAND COLORS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Snowium brand color - Icy cyan glow */
    public static final int SNOWIUM_CYAN = 0x00FFFF;
    
    /** Astralis brand color - Warm orange energy */
    public static final int ASTRALIS_ORANGE = 0xFFAA00;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FEMININE COLORS (for best/latest versions)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Hot pink - Ultimate feminine power */
    public static final int HOT_PINK = 0xFF1493;
    
    /** Magenta - Vibrant and bold */
    public static final int MAGENTA = 0xFF00FF;
    
    /** Lavender - Soft and elegant */
    public static final int LAVENDER = 0xE6E6FA;
    
    /** Deep pink - Rich and warm */
    public static final int DEEP_PINK = 0xFF69B4;
    
    /** Orchid - Delicate purple */
    public static final int ORCHID = 0xDA70D6;
    
    /** Fuchsia - Electric pink */
    public static final int FUCHSIA = 0xFF00FF;
    
    /** Rose pink - Romantic soft pink */
    public static final int ROSE_PINK = 0xFFB6C1;
    
    /** Violet - Royal purple */
    public static final int VIOLET = 0xEE82EE;
    
    /** Pink - Classic soft pink */
    public static final int PINK = 0xFFC0CB;
    
    /** Plum - Deep purple */
    public static final int PLUM = 0xDDA0DD;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // OPENGL COLORS (1.0 - 4.6)
    // Base: Green/Blue → Feminine Purple/Lavender for 4.5-4.6
    // ═══════════════════════════════════════════════════════════════════════════
    
    // OpenGL 1.x - Ancient era (dark grays)
    public static final int OPENGL_1_0 = 0x2F2F2F;  // Very dark gray
    public static final int OPENGL_1_1 = 0x3F3F3F;  // Dark gray
    public static final int OPENGL_1_2 = 0x4F4F4F;  // Medium dark gray
    public static final int OPENGL_1_3 = 0x5F5F5F;  // Gray
    public static final int OPENGL_1_4 = 0x6F6F6F;  // Light gray
    public static final int OPENGL_1_5 = 0x7F7F7F;  // Lighter gray
    
    // OpenGL 2.x - Bronze/Gold era
    public static final int OPENGL_2_0 = 0xCD7F32;  // Bronze
    public static final int OPENGL_2_1 = 0xFFD700;  // Gold
    
    // OpenGL 3.x - Green/Cyan progression
    public static final int OPENGL_3_0 = 0x32CD32;  // Lime green
    public static final int OPENGL_3_1 = 0x3CB371;  // Medium sea green
    public static final int OPENGL_3_2 = 0x2E8B57;  // Sea green
    public static final int OPENGL_3_3 = 0x00CED1;  // Dark turquoise
    
    // OpenGL 4.x - Cyan to Blue to Feminine
    public static final int OPENGL_4_0 = 0x00BFFF;  // Deep sky blue
    public static final int OPENGL_4_1 = 0x1E90FF;  // Dodger blue
    public static final int OPENGL_4_2 = 0x4169E1;  // Royal blue
    public static final int OPENGL_4_3 = 0x6A5ACD;  // Slate blue
    public static final int OPENGL_4_4 = 0x9370DB;  // Medium purple
    public static final int OPENGL_4_5 = 0xE6E6FA;  // LAVENDER (DSA!)
    public static final int OPENGL_4_6 = 0xFF1493;  // HOT PINK (Best!)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // OPENGL ES COLORS (2.0 - 3.2)
    // Base: Bronze/Green → Blue for 3.2
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final int OPENGL_ES_2_0 = 0xB8860B;  // Dark goldenrod
    public static final int OPENGL_ES_3_0 = 0x90EE90;  // Light green
    public static final int OPENGL_ES_3_1 = 0x00CED1;  // Dark turquoise
    public static final int OPENGL_ES_3_2 = 0x4169E1;  // Royal blue
    
    // ═══════════════════════════════════════════════════════════════════════════
    // VULKAN COLORS (1.0 - 1.4)
    // Base: Red → Feminine Pink/Magenta/Lavender for 1.3-1.4
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final int VULKAN_1_0 = 0x8B0000;  // Dark red
    public static final int VULKAN_1_1 = 0xDC143C;  // Crimson
    public static final int VULKAN_1_2 = 0xFF4500;  // Orange red
    public static final int VULKAN_1_3 = 0xDA70D6;  // ORCHID (Feminine!)
    public static final int VULKAN_1_4 = 0xFF1493;  // HOT PINK (Best!)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DIRECTX COLORS (9 - 12.2)
    // Base: Blue → Feminine Magenta/Pink for 12.1-12.2
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final int DIRECTX_9 = 0x2F4F4F;   // Dark slate gray (ancient)
    public static final int DIRECTX_10 = 0x4682B4;  // Steel blue
    public static final int DIRECTX_11 = 0x1E90FF;  // Dodger blue
    public static final int DIRECTX_12_0 = 0x6495ED;  // Cornflower blue
    public static final int DIRECTX_12_1 = 0xEE82EE;  // VIOLET (Feminine!)
    public static final int DIRECTX_12_2 = 0xFF00FF;  // MAGENTA (Best!)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // METAL COLORS (1.0 - 3.2)
    // Base: Silver/White → Feminine Pink/Lavender for 3.1-3.2
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final int METAL_1_0 = 0x708090;  // Slate gray
    public static final int METAL_1_1 = 0x778899;  // Light slate gray
    public static final int METAL_1_2 = 0x87CEEB;  // Sky blue
    public static final int METAL_2_0 = 0xB0C4DE;  // Light steel blue
    public static final int METAL_2_1 = 0xADD8E6;  // Light blue
    public static final int METAL_2_2 = 0xE0FFFF;  // Light cyan
    public static final int METAL_3_0 = 0xF0E68C;  // Khaki
    public static final int METAL_3_1 = 0xFFB6C1;  // ROSE PINK (Feminine!)
    public static final int METAL_3_2 = 0xE6E6FA;  // LAVENDER (Best!)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GLSL COLORS (110 - 460)
    // Base: Green/Blue → Feminine Purple/Pink for 450-460
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final int GLSL_110 = 0x2F2F2F;  // Very dark gray
    public static final int GLSL_120 = 0x3F3F3F;  // Dark gray
    public static final int GLSL_130 = 0x556B2F;  // Dark olive green
    public static final int GLSL_140 = 0x6B8E23;  // Olive drab
    public static final int GLSL_150 = 0x7FFF00;  // Chartreuse
    public static final int GLSL_330 = 0x00FF7F;  // Spring green
    public static final int GLSL_400 = 0x00CED1;  // Dark turquoise
    public static final int GLSL_410 = 0x48D1CC;  // Medium turquoise
    public static final int GLSL_420 = 0x40E0D0;  // Turquoise
    public static final int GLSL_430 = 0x00BFFF;  // Deep sky blue
    public static final int GLSL_440 = 0x1E90FF;  // Dodger blue
    public static final int GLSL_450 = 0x9370DB;  // MEDIUM PURPLE (Feminine!)
    public static final int GLSL_460 = 0xFF69B4;  // DEEP PINK (Best!)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GLSL ES COLORS (100 - 320)
    // Base: Brown/Green → Blue for 320
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final int GLSL_ES_100 = 0x8B4513;  // Saddle brown
    public static final int GLSL_ES_300 = 0x32CD32;  // Lime green
    public static final int GLSL_ES_310 = 0x00CED1;  // Dark turquoise
    public static final int GLSL_ES_320 = 0x4169E1;  // Royal blue
    
    // ═══════════════════════════════════════════════════════════════════════════
    // HLSL COLORS (Shader Model 1.0 - 6.8)
    // Base: Blue → Feminine Magenta/Hot Pink for 6.7-6.8
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final int HLSL_1_0 = 0x191970;  // Midnight blue (ancient)
    public static final int HLSL_2_0 = 0x000080;  // Navy
    public static final int HLSL_3_0 = 0x0000CD;  // Medium blue
    public static final int HLSL_4_0 = 0x4169E1;  // Royal blue
    public static final int HLSL_5_0 = 0x1E90FF;  // Dodger blue
    public static final int HLSL_6_0 = 0x00BFFF;  // Deep sky blue
    public static final int HLSL_6_1 = 0x87CEEB;  // Sky blue
    public static final int HLSL_6_2 = 0x87CEFA;  // Light sky blue
    public static final int HLSL_6_3 = 0xADD8E6;  // Light blue
    public static final int HLSL_6_4 = 0xB0E0E6;  // Powder blue
    public static final int HLSL_6_5 = 0xBA55D3;  // Medium orchid
    public static final int HLSL_6_6 = 0xDA70D6;  // ORCHID (Feminine!)
    public static final int HLSL_6_7 = 0xFF00FF;  // MAGENTA (Feminine!)
    public static final int HLSL_6_8 = 0xFF1493;  // HOT PINK (Best!)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MSL COLORS (1.0 - 3.2)
    // Base: Silver/Blue → Feminine Pink/Lavender for 3.1-3.2
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final int MSL_1_0 = 0x708090;  // Slate gray
    public static final int MSL_1_1 = 0x778899;  // Light slate gray
    public static final int MSL_1_2 = 0x87CEEB;  // Sky blue
    public static final int MSL_2_0 = 0xB0C4DE;  // Light steel blue
    public static final int MSL_2_1 = 0xADD8E6;  // Light blue
    public static final int MSL_2_2 = 0xE0FFFF;  // Light cyan
    public static final int MSL_3_0 = 0xF0E68C;  // Khaki
    public static final int MSL_3_1 = 0xFFC0CB;  // PINK (Feminine!)
    public static final int MSL_3_2 = 0xE6E6FA;  // LAVENDER (Best!)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SPIR-V COLORS (1.0 - 1.6)
    // Base: Red (like Vulkan) → Feminine Magenta/Hot Pink for 1.5-1.6
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static final int SPIRV_1_0 = 0x8B0000;  // Dark red
    public static final int SPIRV_1_1 = 0xB22222;  // Fire brick
    public static final int SPIRV_1_2 = 0xDC143C;  // Crimson
    public static final int SPIRV_1_3 = 0xFF4500;  // Orange red
    public static final int SPIRV_1_4 = 0xFF6347;  // Tomato
    public static final int SPIRV_1_5 = 0xFF00FF;  // MAGENTA (Feminine!)
    public static final int SPIRV_1_6 = 0xFF1493;  // HOT PINK (Best!)
    
    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Convert RGB int to ARGB with full opacity.
     */
    public static int toARGB(int rgb) {
        return 0xFF000000 | rgb;
    }
    
    /**
     * Extract red component (0-255).
     */
    public static int getRed(int rgb) {
        return (rgb >> 16) & 0xFF;
    }
    
    /**
     * Extract green component (0-255).
     */
    public static int getGreen(int rgb) {
        return (rgb >> 8) & 0xFF;
    }
    
    /**
     * Extract blue component (0-255).
     */
    public static int getBlue(int rgb) {
        return rgb & 0xFF;
    }
    
    /**
     * Create RGB from components.
     */
    public static int rgb(int r, int g, int b) {
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
    
    /**
     * Interpolate between two colors.
     * @param t 0.0 to 1.0
     */
    public static int lerp(int color1, int color2, float t) {
        int r1 = getRed(color1);
        int g1 = getGreen(color1);
        int b1 = getBlue(color1);
        
        int r2 = getRed(color2);
        int g2 = getGreen(color2);
        int b2 = getBlue(color2);
        
        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);
        
        return rgb(r, g, b);
    }
}
