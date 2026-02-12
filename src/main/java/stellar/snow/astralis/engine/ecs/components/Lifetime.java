package stellar.snow.astralis.engine.ecs.components;

/**
 * Lifetime - Automatically destroy entity after duration.
 * 
 * <p>Useful for particles, temporary effects, projectiles, etc.
 * Entity will be marked for destruction when current time exceeds destroyAt.
 * 
 * <h2>Memory Layout</h2>
 * <pre>
 * Offset | Size | Field
 * -------|------|-------
 *   0    |  8   | createdAt (double, seconds)
 *   8    |  8   | destroyAt (double, seconds)
 *  16    |  4   | duration (float, seconds)
 *  20    |  4   | padding
 * Total: 24 bytes
 * </pre>
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public record Lifetime(
    double createdAt,
    double destroyAt,
    float duration
) {
    /** Component size in bytes */
    public static final int SIZE = 24;
    
    /** Component alignment */
    public static final int ALIGNMENT = 8;
    
    /**
     * Create lifetime starting now.
     */
    public static Lifetime of(float durationSeconds, double currentTime) {
        return new Lifetime(
            currentTime,
            currentTime + durationSeconds,
            durationSeconds
        );
    }
    
    /**
     * Create infinite lifetime (never expires).
     */
    public static Lifetime infinite(double currentTime) {
        return new Lifetime(currentTime, Double.MAX_VALUE, Float.MAX_VALUE);
    }
    
    /**
     * Check if lifetime has expired.
     */
    public boolean isExpired(double currentTime) {
        return currentTime >= destroyAt;
    }
    
    /**
     * Get remaining time in seconds.
     */
    public double remaining(double currentTime) {
        return Math.max(0, destroyAt - currentTime);
    }
    
    /**
     * Get elapsed time since creation.
     */
    public double elapsed(double currentTime) {
        return currentTime - createdAt;
    }
    
    /**
     * Get progress ratio (0.0 = just created, 1.0 = expired).
     */
    public float progress(double currentTime) {
        if (duration <= 0 || duration == Float.MAX_VALUE) {
            return 0;
        }
        double elapsed = elapsed(currentTime);
        return (float) Math.min(1.0, elapsed / duration);
    }
    
    /**
     * Extend lifetime by additional seconds.
     */
    public Lifetime extend(float additionalSeconds) {
        return new Lifetime(
            createdAt,
            destroyAt + additionalSeconds,
            duration + additionalSeconds
        );
    }
    
    /**
     * Reduce lifetime (make expire sooner).
     */
    public Lifetime reduce(float seconds) {
        float newDuration = Math.max(0, duration - seconds);
        return new Lifetime(
            createdAt,
            createdAt + newDuration,
            newDuration
        );
    }
    
    /**
     * Reset to original duration from current time.
     */
    public Lifetime reset(double currentTime) {
        return new Lifetime(
            currentTime,
            currentTime + duration,
            duration
        );
    }
}
