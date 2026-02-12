package stellar.snow.astralis.engine.ecs.components;

/**
 * Velocity - Linear and angular velocity for physics simulation.
 * 
 * <p>Represents movement in 3D space with both linear velocity (m/s)
 * and angular velocity (rad/s).
 * 
 * <h2>Memory Layout</h2>
 * <pre>
 * Offset | Size | Field
 * -------|------|-------
 *   0    |  4   | vx (float, m/s)
 *   4    |  4   | vy (float, m/s)
 *   8    |  4   | vz (float, m/s)
 *  12    |  4   | angularX (float, rad/s)
 *  16    |  4   | angularY (float, rad/s)
 *  20    |  4   | angularZ (float, rad/s)
 * Total: 24 bytes
 * </pre>
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public record Velocity(
    float vx, float vy, float vz,
    float angularX, float angularY, float angularZ
) {
    /** Component size in bytes */
    public static final int SIZE = 24;
    
    /** Component alignment */
    public static final int ALIGNMENT = 4;
    
    /**
     * Zero velocity (stationary).
     */
    public static Velocity zero() {
        return new Velocity(0, 0, 0, 0, 0, 0);
    }
    
    /**
     * Create velocity from direction and speed.
     */
    public static Velocity fromDirection(float dirX, float dirY, float dirZ, float speed) {
        float length = (float) Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (length < 0.0001f) {
            return zero();
        }
        float scale = speed / length;
        return new Velocity(dirX * scale, dirY * scale, dirZ * scale, 0, 0, 0);
    }
    
    /**
     * Get speed (magnitude of linear velocity).
     */
    public float speed() {
        return (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
    }
    
    /**
     * Get squared speed (faster, no sqrt).
     */
    public float speedSquared() {
        return vx * vx + vy * vy + vz * vz;
    }
    
    /**
     * Get angular speed (magnitude of angular velocity).
     */
    public float angularSpeed() {
        return (float) Math.sqrt(angularX * angularX + angularY * angularY + angularZ * angularZ);
    }
    
    /**
     * Clamp speed to maximum.
     */
    public Velocity clampSpeed(float maxSpeed) {
        float currentSpeed = speed();
        if (currentSpeed <= maxSpeed) {
            return this;
        }
        float scale = maxSpeed / currentSpeed;
        return new Velocity(
            vx * scale, vy * scale, vz * scale,
            angularX, angularY, angularZ
        );
    }
    
    /**
     * Add acceleration.
     */
    public Velocity accelerate(float ax, float ay, float az, float dt) {
        return new Velocity(
            vx + ax * dt, vy + ay * dt, vz + az * dt,
            angularX, angularY, angularZ
        );
    }
    
    /**
     * Apply drag/friction.
     */
    public Velocity applyDrag(float dragCoefficient, float dt) {
        float factor = (float) Math.pow(1.0 - dragCoefficient, dt);
        return new Velocity(
            vx * factor, vy * factor, vz * factor,
            angularX * factor, angularY * factor, angularZ * factor
        );
    }
    
    /**
     * Stop all movement.
     */
    public Velocity stop() {
        return zero();
    }
}
