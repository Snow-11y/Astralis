package stellar.snow.astralis.debug;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * MinecraftColorRegistry - Registers custom colors to Minecraft's formatting system.
 * 
 * This allows us to use beautiful custom colors (hot pink, lavender, etc.)
 * in Minecraft's chat, debug overlay, and any text rendering system.
 * 
 * Usage:
 *   MinecraftColorRegistry.register();
 *   String text = MinecraftColorRegistry.getColorCode("HOT_PINK") + "Hot Pink Text!";
 * 
 * @author Astralis Team
 */
public final class MinecraftColorRegistry {
    
    private MinecraftColorRegistry() {}
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COLOR REGISTRY
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static final Map<String, TextColor> CUSTOM_COLORS = new HashMap<>();
    private static final Map<String, String> COLOR_CODES = new HashMap<>();
    private static boolean registered = false;
    
    // Color code character
    public static final char COLOR_CHAR = '\u00A7';
    
    // ═══════════════════════════════════════════════════════════════════════════
    // REGISTRATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Register all custom colors to Minecraft.
     * Call this during mod initialization.
     */
    public static void register() {
        if (registered) return;
        
        // Register all feminine colors
        registerColor("HOT_PINK", CustomColors.HOT_PINK);
        registerColor("MAGENTA", CustomColors.MAGENTA);
        registerColor("LAVENDER", CustomColors.LAVENDER);
        registerColor("DEEP_PINK", CustomColors.DEEP_PINK);
        registerColor("ORCHID", CustomColors.ORCHID);
        registerColor("FUCHSIA", CustomColors.FUCHSIA);
        registerColor("ROSE_PINK", CustomColors.ROSE_PINK);
        registerColor("VIOLET", CustomColors.VIOLET);
        registerColor("PINK", CustomColors.PINK);
        registerColor("PLUM", CustomColors.PLUM);
        
        // Register brand colors
        registerColor("SNOWIUM_CYAN", CustomColors.SNOWIUM_CYAN);
        registerColor("ASTRALIS_ORANGE", CustomColors.ASTRALIS_ORANGE);
        
        // Register all OpenGL colors
        registerOpenGLColors();
        registerOpenGLESColors();
        registerVulkanColors();
        registerDirectXColors();
        registerMetalColors();
        registerGLSLColors();
        registerGLSLESColors();
        registerHLSLColors();
        registerMSLColors();
        registerSPIRVColors();
        
        registered = true;
    }
    
    private static void registerOpenGLColors() {
        registerColor("OPENGL_1_0", CustomColors.OPENGL_1_0);
        registerColor("OPENGL_1_1", CustomColors.OPENGL_1_1);
        registerColor("OPENGL_1_2", CustomColors.OPENGL_1_2);
        registerColor("OPENGL_1_3", CustomColors.OPENGL_1_3);
        registerColor("OPENGL_1_4", CustomColors.OPENGL_1_4);
        registerColor("OPENGL_1_5", CustomColors.OPENGL_1_5);
        registerColor("OPENGL_2_0", CustomColors.OPENGL_2_0);
        registerColor("OPENGL_2_1", CustomColors.OPENGL_2_1);
        registerColor("OPENGL_3_0", CustomColors.OPENGL_3_0);
        registerColor("OPENGL_3_1", CustomColors.OPENGL_3_1);
        registerColor("OPENGL_3_2", CustomColors.OPENGL_3_2);
        registerColor("OPENGL_3_3", CustomColors.OPENGL_3_3);
        registerColor("OPENGL_4_0", CustomColors.OPENGL_4_0);
        registerColor("OPENGL_4_1", CustomColors.OPENGL_4_1);
        registerColor("OPENGL_4_2", CustomColors.OPENGL_4_2);
        registerColor("OPENGL_4_3", CustomColors.OPENGL_4_3);
        registerColor("OPENGL_4_4", CustomColors.OPENGL_4_4);
        registerColor("OPENGL_4_5", CustomColors.OPENGL_4_5);
        registerColor("OPENGL_4_6", CustomColors.OPENGL_4_6);
    }
    
    private static void registerOpenGLESColors() {
        registerColor("OPENGL_ES_2_0", CustomColors.OPENGL_ES_2_0);
        registerColor("OPENGL_ES_3_0", CustomColors.OPENGL_ES_3_0);
        registerColor("OPENGL_ES_3_1", CustomColors.OPENGL_ES_3_1);
        registerColor("OPENGL_ES_3_2", CustomColors.OPENGL_ES_3_2);
    }
    
    private static void registerVulkanColors() {
        registerColor("VULKAN_1_0", CustomColors.VULKAN_1_0);
        registerColor("VULKAN_1_1", CustomColors.VULKAN_1_1);
        registerColor("VULKAN_1_2", CustomColors.VULKAN_1_2);
        registerColor("VULKAN_1_3", CustomColors.VULKAN_1_3);
        registerColor("VULKAN_1_4", CustomColors.VULKAN_1_4);
    }
    
    private static void registerDirectXColors() {
        registerColor("DIRECTX_9", CustomColors.DIRECTX_9);
        registerColor("DIRECTX_10", CustomColors.DIRECTX_10);
        registerColor("DIRECTX_11", CustomColors.DIRECTX_11);
        registerColor("DIRECTX_12_0", CustomColors.DIRECTX_12_0);
        registerColor("DIRECTX_12_1", CustomColors.DIRECTX_12_1);
        registerColor("DIRECTX_12_2", CustomColors.DIRECTX_12_2);
    }
    
    private static void registerMetalColors() {
        registerColor("METAL_1_0", CustomColors.METAL_1_0);
        registerColor("METAL_1_1", CustomColors.METAL_1_1);
        registerColor("METAL_1_2", CustomColors.METAL_1_2);
        registerColor("METAL_2_0", CustomColors.METAL_2_0);
        registerColor("METAL_2_1", CustomColors.METAL_2_1);
        registerColor("METAL_2_2", CustomColors.METAL_2_2);
        registerColor("METAL_3_0", CustomColors.METAL_3_0);
        registerColor("METAL_3_1", CustomColors.METAL_3_1);
        registerColor("METAL_3_2", CustomColors.METAL_3_2);
    }
    
    private static void registerGLSLColors() {
        registerColor("GLSL_110", CustomColors.GLSL_110);
        registerColor("GLSL_120", CustomColors.GLSL_120);
        registerColor("GLSL_130", CustomColors.GLSL_130);
        registerColor("GLSL_140", CustomColors.GLSL_140);
        registerColor("GLSL_150", CustomColors.GLSL_150);
        registerColor("GLSL_330", CustomColors.GLSL_330);
        registerColor("GLSL_400", CustomColors.GLSL_400);
        registerColor("GLSL_410", CustomColors.GLSL_410);
        registerColor("GLSL_420", CustomColors.GLSL_420);
        registerColor("GLSL_430", CustomColors.GLSL_430);
        registerColor("GLSL_440", CustomColors.GLSL_440);
        registerColor("GLSL_450", CustomColors.GLSL_450);
        registerColor("GLSL_460", CustomColors.GLSL_460);
    }
    
    private static void registerGLSLESColors() {
        registerColor("GLSL_ES_100", CustomColors.GLSL_ES_100);
        registerColor("GLSL_ES_300", CustomColors.GLSL_ES_300);
        registerColor("GLSL_ES_310", CustomColors.GLSL_ES_310);
        registerColor("GLSL_ES_320", CustomColors.GLSL_ES_320);
    }
    
    private static void registerHLSLColors() {
        registerColor("HLSL_1_0", CustomColors.HLSL_1_0);
        registerColor("HLSL_2_0", CustomColors.HLSL_2_0);
        registerColor("HLSL_3_0", CustomColors.HLSL_3_0);
        registerColor("HLSL_4_0", CustomColors.HLSL_4_0);
        registerColor("HLSL_5_0", CustomColors.HLSL_5_0);
        registerColor("HLSL_6_0", CustomColors.HLSL_6_0);
        registerColor("HLSL_6_1", CustomColors.HLSL_6_1);
        registerColor("HLSL_6_2", CustomColors.HLSL_6_2);
        registerColor("HLSL_6_3", CustomColors.HLSL_6_3);
        registerColor("HLSL_6_4", CustomColors.HLSL_6_4);
        registerColor("HLSL_6_5", CustomColors.HLSL_6_5);
        registerColor("HLSL_6_6", CustomColors.HLSL_6_6);
        registerColor("HLSL_6_7", CustomColors.HLSL_6_7);
        registerColor("HLSL_6_8", CustomColors.HLSL_6_8);
    }
    
    private static void registerMSLColors() {
        registerColor("MSL_1_0", CustomColors.MSL_1_0);
        registerColor("MSL_1_1", CustomColors.MSL_1_1);
        registerColor("MSL_1_2", CustomColors.MSL_1_2);
        registerColor("MSL_2_0", CustomColors.MSL_2_0);
        registerColor("MSL_2_1", CustomColors.MSL_2_1);
        registerColor("MSL_2_2", CustomColors.MSL_2_2);
        registerColor("MSL_3_0", CustomColors.MSL_3_0);
        registerColor("MSL_3_1", CustomColors.MSL_3_1);
        registerColor("MSL_3_2", CustomColors.MSL_3_2);
    }
    
    private static void registerSPIRVColors() {
        registerColor("SPIRV_1_0", CustomColors.SPIRV_1_0);
        registerColor("SPIRV_1_1", CustomColors.SPIRV_1_1);
        registerColor("SPIRV_1_2", CustomColors.SPIRV_1_2);
        registerColor("SPIRV_1_3", CustomColors.SPIRV_1_3);
        registerColor("SPIRV_1_4", CustomColors.SPIRV_1_4);
        registerColor("SPIRV_1_5", CustomColors.SPIRV_1_5);
        registerColor("SPIRV_1_6", CustomColors.SPIRV_1_6);
    }
    
    /**
     * Register a single color.
     */
    private static void registerColor(String name, int rgb) {
        TextColor textColor = TextColor.fromRgb(rgb);
        CUSTOM_COLORS.put(name, textColor);
        
        // Create a color code string that can be used in chat
        COLOR_CODES.put(name, COLOR_CHAR + "x" + rgbToHexFormat(rgb));
    }
    
    /**
     * Convert RGB to Minecraft hex format (§x§R§R§G§G§B§B).
     */
    private static String rgbToHexFormat(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        
        String hex = String.format("%02x%02x%02x", r, g, b);
        StringBuilder result = new StringBuilder();
        
        for (char c : hex.toCharArray()) {
            result.append(COLOR_CHAR).append(c);
        }
        
        return result.toString();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get TextColor by name.
     */
    public static TextColor getTextColor(String name) {
        return CUSTOM_COLORS.get(name);
    }
    
    /**
     * Get color code string for chat formatting.
     */
    public static String getColorCode(String name) {
        return COLOR_CODES.getOrDefault(name, "");
    }
    
    /**
     * Create a Style with the custom color.
     */
    public static Style createStyle(String colorName) {
        TextColor color = getTextColor(colorName);
        return color != null ? Style.EMPTY.withColor(color) : Style.EMPTY;
    }
    
    /**
     * Get RGB value for a color name.
     */
    public static int getRGB(String name) {
        TextColor color = getTextColor(name);
        return color != null ? color.getValue() : 0xFFFFFF;
    }
    
    /**
     * Check if color is registered.
     */
    public static boolean hasColor(String name) {
        return CUSTOM_COLORS.containsKey(name);
    }
    
    /**
     * Get all registered color names.
     */
    public static String[] getAllColorNames() {
        return CUSTOM_COLORS.keySet().toArray(new String[0]);
    }
}
