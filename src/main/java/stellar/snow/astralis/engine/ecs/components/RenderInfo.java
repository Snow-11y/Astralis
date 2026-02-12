package stellar.snow.astralis.engine.ecs.components;

/**
 * RenderInfo - Visual rendering properties for entities.
 * 
 * <p>Contains model ID, texture, color tint, visibility, and render layers.
 * Optimized for batch rendering systems.
 * 
 * <h2>Memory Layout</h2>
 * <pre>
 * Offset | Size | Field
 * -------|------|-------
 *   0    |  4   | modelId (int)
 *   4    |  4   | textureId (int)
 *   8    |  4   | colorRGBA (int, packed)
 *  12    |  2   | layer (short, render layer)
 *  14    |  1   | visible (boolean)
 *  15    |  1   | castsShadow (boolean)
 *  16    |  4   | opacity (float, 0-1)
 *  20    |  4   | scale (float, uniform scale)
 * Total: 24 bytes
 * </pre>
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public record RenderInfo(
    int modelId,
    int textureId,
    int colorRGBA,
    short layer,
    boolean visible,
    boolean castsShadow,
    float opacity,
    float scale
) {
    /** Component size in bytes */
    public static final int SIZE = 24;
    
    /** Component alignment */
    public static final int ALIGNMENT = 4;
    
    // Render layers
    public static final short LAYER_BACKGROUND = -100;
    public static final short LAYER_TERRAIN = -50;
    public static final short LAYER_DEFAULT = 0;
    public static final short LAYER_ENTITIES = 50;
    public static final short LAYER_EFFECTS = 100;
    public static final short LAYER_UI = 1000;
    
    /**
     * Create default render info.
     */
    public static RenderInfo defaults() {
        return new RenderInfo(0, 0, 0xFFFFFFFF, LAYER_DEFAULT, true, true, 1.0f, 1.0f);
    }
    
    /**
     * Create with model and texture.
     */
    public static RenderInfo of(int modelId, int textureId) {
        return new RenderInfo(modelId, textureId, 0xFFFFFFFF, LAYER_DEFAULT, true, true, 1.0f, 1.0f);
    }
    
    /**
     * Create with model, texture, and color.
     */
    public static RenderInfo of(int modelId, int textureId, int color) {
        return new RenderInfo(modelId, textureId, color, LAYER_DEFAULT, true, true, 1.0f, 1.0f);
    }
    
    /**
     * Pack RGBA color into int.
     */
    public static int packColor(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
    
    /**
     * Pack RGBA color from floats (0-1).
     */
    public static int packColor(float r, float g, float b, float a) {
        return packColor(
            (int)(r * 255),
            (int)(g * 255),
            (int)(b * 255),
            (int)(a * 255)
        );
    }
    
    /**
     * Extract red channel (0-255).
     */
    public int red() {
        return (colorRGBA >> 16) & 0xFF;
    }
    
    /**
     * Extract green channel (0-255).
     */
    public int green() {
        return (colorRGBA >> 8) & 0xFF;
    }
    
    /**
     * Extract blue channel (0-255).
     */
    public int blue() {
        return colorRGBA & 0xFF;
    }
    
    /**
     * Extract alpha channel (0-255).
     */
    public int alpha() {
        return (colorRGBA >> 24) & 0xFF;
    }
    
    /**
     * Set visibility.
     */
    public RenderInfo setVisible(boolean isVisible) {
        return new RenderInfo(modelId, textureId, colorRGBA, layer, isVisible, castsShadow, opacity, scale);
    }
    
    /**
     * Hide entity.
     */
    public RenderInfo hide() {
        return setVisible(false);
    }
    
    /**
     * Show entity.
     */
    public RenderInfo show() {
        return setVisible(true);
    }
    
    /**
     * Set opacity (0 = transparent, 1 = opaque).
     */
    public RenderInfo setOpacity(float alpha) {
        return new RenderInfo(modelId, textureId, colorRGBA, layer, visible, castsShadow, 
                            Math.max(0, Math.min(1, alpha)), scale);
    }
    
    /**
     * Set color tint.
     */
    public RenderInfo setColor(int rgba) {
        return new RenderInfo(modelId, textureId, rgba, layer, visible, castsShadow, opacity, scale);
    }
    
    /**
     * Set color tint from components.
     */
    public RenderInfo setColor(int r, int g, int b, int a) {
        return setColor(packColor(r, g, b, a));
    }
    
    /**
     * Set render layer.
     */
    public RenderInfo setLayer(short renderLayer) {
        return new RenderInfo(modelId, textureId, colorRGBA, renderLayer, visible, castsShadow, opacity, scale);
    }
    
    /**
     * Set model.
     */
    public RenderInfo setModel(int model) {
        return new RenderInfo(model, textureId, colorRGBA, layer, visible, castsShadow, opacity, scale);
    }
    
    /**
     * Set texture.
     */
    public RenderInfo setTexture(int texture) {
        return new RenderInfo(modelId, texture, colorRGBA, layer, visible, castsShadow, opacity, scale);
    }
    
    /**
     * Set uniform scale.
     */
    public RenderInfo setScale(float uniformScale) {
        return new RenderInfo(modelId, textureId, colorRGBA, layer, visible, castsShadow, opacity, uniformScale);
    }
    
    /**
     * Enable/disable shadow casting.
     */
    public RenderInfo setCastsShadow(boolean shadows) {
        return new RenderInfo(modelId, textureId, colorRGBA, layer, visible, shadows, opacity, scale);
    }
}
