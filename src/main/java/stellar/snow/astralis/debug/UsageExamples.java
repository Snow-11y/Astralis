/**
 * USAGE EXAMPLES - Beautiful API Color System
 * 
 * This file demonstrates how to use the gorgeous color system with
 * hot pink, lavender, magenta, and other feminine colors for the best API versions!
 */

package stellar.snow.astralis.debug;

public class UsageExamples {
    
    /**
     * STEP 1: Initialize the color system (do this once at mod init)
     */
    public static void initializeColors() {
        // Register all beautiful colors to Minecraft
        MinecraftColorRegistry.register();
    }
    
    /**
     * EXAMPLE 1: Display OpenGL 4.6 in F3 debug screen (HOT PINK! 💕)
     */
    public static String exampleOpenGL46() {
        return F3DebugRenderer.getDebugLine(
            F3DebugRenderer.APIType.OPENGL, 
            4, 
            6
        );
        // Output: [FF] Snowium Render: <HOT_PINK>OpenGL 4.6</HOT_PINK>
    }
    
    /**
     * EXAMPLE 2: Display Vulkan 1.4 (HOT PINK! 💕)
     */
    public static String exampleVulkan14() {
        return F3DebugRenderer.getDebugLine(
            F3DebugRenderer.APIType.VULKAN,
            1,
            4
        );
        // Output: [FF] Snowium Render: <HOT_PINK>Vulkan 1.4</HOT_PINK>
    }
    
    /**
     * EXAMPLE 3: Display Vulkan 1.4 with SPIR-V 1.6 (BOTH HOT PINK! 💕💕)
     */
    public static String exampleVulkanWithSPIRV() {
        return F3DebugRenderer.getDebugLine(
            F3DebugRenderer.APIType.VULKAN, 1, 4,
            F3DebugRenderer.APIType.SPIRV, 1, 6
        );
        // Output: [FF] Snowium Render: <HOT_PINK>Vulkan 1.4</HOT_PINK> / <HOT_PINK>SPIR-V 1.6</HOT_PINK>
    }
    
    /**
     * EXAMPLE 4: Display DirectX 12.2 (MAGENTA! 💖)
     */
    public static String exampleDirectX122() {
        return F3DebugRenderer.getDebugLine(
            F3DebugRenderer.APIType.DIRECTX,
            12,
            2
        );
        // Output: [FF] Snowium Render: <MAGENTA>DirectX 12.2</MAGENTA>
    }
    
    /**
     * EXAMPLE 5: Display Metal 3.2 (LAVENDER! 💜)
     */
    public static String exampleMetal32() {
        return F3DebugRenderer.getDebugLine(
            F3DebugRenderer.APIType.METAL,
            3,
            2
        );
        // Output: [FF] Snowium Render: <LAVENDER>Metal 3.2</LAVENDER>
    }
    
    /**
     * EXAMPLE 6: Display HLSL Shader Model 6.8 (HOT PINK! 💕)
     */
    public static String exampleHLSL68() {
        return F3DebugRenderer.getDebugLine(
            F3DebugRenderer.APIType.HLSL,
            6,
            8
        );
        // Output: [FF] Snowium Render: <HOT_PINK>HLSL 6.8</HOT_PINK>
    }
    
    /**
     * EXAMPLE 7: Display GLSL 460 (DEEP PINK! 💕)
     */
    public static String exampleGLSL460() {
        return F3DebugRenderer.getDebugLine(
            F3DebugRenderer.APIType.GLSL,
            4,
            60
        );
        // Output: [FF] Snowium Render: <DEEP_PINK>GLSL 4.60</DEEP_PINK>
    }
    
    /**
     * EXAMPLE 8: Extended debug info with features
     */
    public static String[] exampleExtendedDebug() {
        return F3DebugRenderer.getExtendedDebugLines(
            F3DebugRenderer.APIType.OPENGL, 4, 6,
            F3DebugRenderer.APIType.GLSL, 4, 60,
            true,  // has DSA
            true,  // has Persistent
            true,  // has MDI
            true   // cache enabled
        );
        // Output:
        // [FF] Snowium Render: <HOT_PINK>OpenGL 4.6</HOT_PINK> / <DEEP_PINK>GLSL 4.60</DEEP_PINK>
        //   DSA: ✓  Persistent: ✓  MDI: ✓
        //   State Cache: ✓
    }
    
    /**
     * EXAMPLE 9: Use custom colors directly in your own text
     */
    public static String exampleCustomText() {
        String hotPink = MinecraftColorRegistry.getColorCode("HOT_PINK");
        String lavender = MinecraftColorRegistry.getColorCode("LAVENDER");
        String magenta = MinecraftColorRegistry.getColorCode("MAGENTA");
        
        return hotPink + "This is HOT PINK! " +
               lavender + "This is LAVENDER! " +
               magenta + "This is MAGENTA!";
    }
    
    /**
     * EXAMPLE 10: Get RGB values for custom rendering
     */
    public static void exampleRGBValues() {
        int hotPinkRGB = CustomColors.HOT_PINK;        // 0xFF1493
        int lavenderRGB = CustomColors.LAVENDER;       // 0xE6E6FA
        int magentaRGB = CustomColors.MAGENTA;         // 0xFF00FF
        int orchidRGB = CustomColors.ORCHID;           // 0xDA70D6
        int deepPinkRGB = CustomColors.DEEP_PINK;      // 0xFF69B4
        
        // Use these for custom GL/Vulkan/DX rendering
    }
    
    /**
     * COLOR REFERENCE GUIDE
     * 
     * BEST VERSIONS GET FEMININE COLORS:
     * 
     * OpenGL 4.6      → HOT PINK      (0xFF1493) 💕
     * OpenGL 4.5      → LAVENDER      (0xE6E6FA) 💜
     * 
     * Vulkan 1.4      → HOT PINK      (0xFF1493) 💕
     * Vulkan 1.3      → ORCHID        (0xDA70D6) 🌸
     * 
     * DirectX 12.2    → MAGENTA       (0xFF00FF) 💖
     * DirectX 12.1    → VIOLET        (0xEE82EE) 💜
     * 
     * Metal 3.2       → LAVENDER      (0xE6E6FA) 💜
     * Metal 3.1       → ROSE PINK     (0xFFB6C1) 🌹
     * 
     * GLSL 460        → DEEP PINK     (0xFF69B4) 💕
     * GLSL 450        → MEDIUM PURPLE (0x9370DB) 💜
     * 
     * HLSL 6.8        → HOT PINK      (0xFF1493) 💕
     * HLSL 6.7        → MAGENTA       (0xFF00FF) 💖
     * HLSL 6.6        → ORCHID        (0xDA70D6) 🌸
     * 
     * MSL 3.2         → LAVENDER      (0xE6E6FA) 💜
     * MSL 3.1         → PINK          (0xFFC0CB) 💗
     * 
     * SPIR-V 1.6      → HOT PINK      (0xFF1493) 💕
     * SPIR-V 1.5      → MAGENTA       (0xFF00FF) 💖
     * 
     * Each older version gets progressively darker colors,
     * creating a beautiful visual hierarchy! ✨
     */
}
