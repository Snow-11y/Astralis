package stellar.snow.astralis.engine.ecs.components;

/**
 * AABB - Axis-Aligned Bounding Box for collision detection.
 * 
 * <p>Defines a rectangular collision volume aligned with world axes.
 * Used for broad-phase collision detection and spatial queries.
 * 
 * <h2>Memory Layout</h2>
 * <pre>
 * Offset | Size | Field
 * -------|------|-------
 *   0    |  4   | halfWidth (float)
 *   4    |  4   | halfHeight (float)
 *   8    |  4   | halfDepth (float)
 *  12    |  4   | layer (int, collision layer)
 * Total: 16 bytes
 * </pre>
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public record AABB(
    float halfWidth,
    float halfHeight,
    float halfDepth,
    int layer
) {
    /** Component size in bytes */
    public static final int SIZE = 16;
    
    /** Component alignment */
    public static final int ALIGNMENT = 4;
    
    // Collision layers
    public static final int LAYER_DEFAULT = 0;
    public static final int LAYER_PLAYER = 1;
    public static final int LAYER_ENEMY = 2;
    public static final int LAYER_PROJECTILE = 3;
    public static final int LAYER_TRIGGER = 4;
    public static final int LAYER_TERRAIN = 5;
    
    /**
     * Create AABB from full dimensions.
     */
    public static AABB fromSize(float width, float height, float depth) {
        return new AABB(width / 2, height / 2, depth / 2, LAYER_DEFAULT);
    }
    
    /**
     * Create AABB from full dimensions with layer.
     */
    public static AABB fromSize(float width, float height, float depth, int layer) {
        return new AABB(width / 2, height / 2, depth / 2, layer);
    }
    
    /**
     * Create cube AABB.
     */
    public static AABB cube(float size) {
        float half = size / 2;
        return new AABB(half, half, half, LAYER_DEFAULT);
    }
    
    /**
     * Create cube AABB with layer.
     */
    public static AABB cube(float size, int layer) {
        float half = size / 2;
        return new AABB(half, half, half, layer);
    }
    
    /**
     * Get full width.
     */
    public float width() {
        return halfWidth * 2;
    }
    
    /**
     * Get full height.
     */
    public float height() {
        return halfHeight * 2;
    }
    
    /**
     * Get full depth.
     */
    public float depth() {
        return halfDepth * 2;
    }
    
    /**
     * Get volume.
     */
    public float volume() {
        return width() * height() * depth();
    }
    
    /**
     * Check intersection with another AABB at given positions.
     */
    public boolean intersects(float x1, float y1, float z1,
                             AABB other, float x2, float y2, float z2) {
        return Math.abs(x1 - x2) < (halfWidth + other.halfWidth) &&
               Math.abs(y1 - y2) < (halfHeight + other.halfHeight) &&
               Math.abs(z1 - z2) < (halfDepth + other.halfDepth);
    }
    
    /**
     * Check if point is inside AABB at given position.
     */
    public boolean contains(float aabbX, float aabbY, float aabbZ,
                           float pointX, float pointY, float pointZ) {
        return Math.abs(pointX - aabbX) <= halfWidth &&
               Math.abs(pointY - aabbY) <= halfHeight &&
               Math.abs(pointZ - aabbZ) <= halfDepth;
    }
    
    /**
     * Scale AABB.
     */
    public AABB scale(float factor) {
        return new AABB(
            halfWidth * factor,
            halfHeight * factor,
            halfDepth * factor,
            layer
        );
    }
    
    /**
     * Set collision layer.
     */
    public AABB withLayer(int collisionLayer) {
        return new AABB(halfWidth, halfHeight, halfDepth, collisionLayer);
    }
}
