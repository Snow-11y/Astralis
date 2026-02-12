package stellar.snow.astralis.engine.ecs.components;

/**
 * Transform - Position, rotation, and scale in 3D space.
 * 
 * <p>This is the most fundamental component for any spatial entity.
 * Uses compact layout for cache efficiency: 40 bytes total.
 * 
 * <h2>Memory Layout</h2>
 * <pre>
 * Offset | Size | Field
 * -------|------|-------
 *   0    |  4   | posX (float)
 *   4    |  4   | posY (float)
 *   8    |  4   | posZ (float)
 *  12    |  4   | rotX (float, radians)
 *  16    |  4   | rotY (float, radians)
 *  20    |  4   | rotZ (float, radians)
 *  24    |  4   | scaleX (float)
 *  28    |  4   | scaleY (float)
 *  32    |  4   | scaleZ (float)
 *  36    |  4   | dirty (int, boolean flag)
 * Total: 40 bytes
 * </pre>
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public record Transform(
    float posX, float posY, float posZ,
    float rotX, float rotY, float rotZ,
    float scaleX, float scaleY, float scaleZ,
    boolean dirty
) {
    /** Component size in bytes */
    public static final int SIZE = 40;
    
    /** Component alignment */
    public static final int ALIGNMENT = 4;
    
    /**
     * Create transform at origin with identity rotation and scale.
     */
    public static Transform identity() {
        return new Transform(0, 0, 0, 0, 0, 0, 1, 1, 1, false);
    }
    
    /**
     * Create transform at specific position with identity rotation and scale.
     */
    public static Transform at(float x, float y, float z) {
        return new Transform(x, y, z, 0, 0, 0, 1, 1, 1, false);
    }
    
    /**
     * Create transform with position and rotation.
     */
    public static Transform of(float x, float y, float z, float yaw, float pitch, float roll) {
        return new Transform(x, y, z, pitch, yaw, roll, 1, 1, 1, false);
    }
    
    /**
     * Mark transform as dirty (needs matrix recalculation).
     */
    public Transform markDirty() {
        return new Transform(posX, posY, posZ, rotX, rotY, rotZ, scaleX, scaleY, scaleZ, true);
    }
    
    /**
     * Clear dirty flag.
     */
    public Transform clearDirty() {
        return new Transform(posX, posY, posZ, rotX, rotY, rotZ, scaleX, scaleY, scaleZ, false);
    }
    
    /**
     * Translate by offset.
     */
    public Transform translate(float dx, float dy, float dz) {
        return new Transform(
            posX + dx, posY + dy, posZ + dz,
            rotX, rotY, rotZ,
            scaleX, scaleY, scaleZ,
            true
        );
    }
    
    /**
     * Rotate by Euler angles (radians).
     */
    public Transform rotate(float pitch, float yaw, float roll) {
        return new Transform(
            posX, posY, posZ,
            rotX + pitch, rotY + yaw, rotZ + roll,
            scaleX, scaleY, scaleZ,
            true
        );
    }
    
    /**
     * Scale uniformly.
     */
    public Transform scale(float factor) {
        return new Transform(
            posX, posY, posZ,
            rotX, rotY, rotZ,
            scaleX * factor, scaleY * factor, scaleZ * factor,
            true
        );
    }
    
    /**
     * Scale non-uniformly.
     */
    public Transform scale(float sx, float sy, float sz) {
        return new Transform(
            posX, posY, posZ,
            rotX, rotY, rotZ,
            scaleX * sx, scaleY * sy, scaleZ * sz,
            true
        );
    }
    
    /**
     * Get distance to another transform.
     */
    public float distanceTo(Transform other) {
        float dx = other.posX - posX;
        float dy = other.posY - posY;
        float dz = other.posZ - posZ;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * Get squared distance (faster, no sqrt).
     */
    public float distanceSquaredTo(Transform other) {
        float dx = other.posX - posX;
        float dy = other.posY - posY;
        float dz = other.posZ - posZ;
        return dx * dx + dy * dy + dz * dz;
    }
}
