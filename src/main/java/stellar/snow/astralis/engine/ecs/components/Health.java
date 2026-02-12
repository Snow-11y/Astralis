package stellar.snow.astralis.engine.ecs.components;

/**
 * Health - Health points with damage and healing tracking.
 * 
 * <p>Tracks current and maximum health, with support for shields,
 * regeneration, and invulnerability periods.
 * 
 * <h2>Memory Layout</h2>
 * <pre>
 * Offset | Size | Field
 * -------|------|-------
 *   0    |  4   | current (float)
 *   4    |  4   | maximum (float)
 *   8    |  4   | shield (float)
 *  12    |  4   | regenRate (float, HP/s)
 *  16    |  4   | invulnerableUntil (float, timestamp)
 *  20    |  1   | isDead (boolean)
 *  21    |  3   | padding
 * Total: 24 bytes
 * </pre>
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public record Health(
    float current,
    float maximum,
    float shield,
    float regenRate,
    float invulnerableUntil,
    boolean isDead
) {
    /** Component size in bytes */
    public static final int SIZE = 24;
    
    /** Component alignment */
    public static final int ALIGNMENT = 4;
    
    /**
     * Create health with specified max HP.
     */
    public static Health withMax(float maxHp) {
        return new Health(maxHp, maxHp, 0, 0, 0, false);
    }
    
    /**
     * Create health with max HP and regeneration.
     */
    public static Health withRegen(float maxHp, float regenPerSecond) {
        return new Health(maxHp, maxHp, 0, regenPerSecond, 0, false);
    }
    
    /**
     * Create health with shield.
     */
    public static Health withShield(float maxHp, float shieldAmount) {
        return new Health(maxHp, maxHp, shieldAmount, 0, 0, false);
    }
    
    /**
     * Check if entity is alive.
     */
    public boolean isAlive() {
        return !isDead && current > 0;
    }
    
    /**
     * Check if entity is invulnerable.
     */
    public boolean isInvulnerable(float currentTime) {
        return currentTime < invulnerableUntil;
    }
    
    /**
     * Get health percentage (0.0 to 1.0).
     */
    public float percentage() {
        return maximum > 0 ? Math.max(0, Math.min(1, current / maximum)) : 0;
    }
    
    /**
     * Apply damage, accounting for shield and invulnerability.
     */
    public Health damage(float amount, float currentTime) {
        if (isDead || isInvulnerable(currentTime) || amount <= 0) {
            return this;
        }
        
        // Shield absorbs damage first
        float remainingDamage = amount;
        float newShield = shield;
        
        if (shield > 0) {
            if (shield >= remainingDamage) {
                newShield = shield - remainingDamage;
                remainingDamage = 0;
            } else {
                remainingDamage -= shield;
                newShield = 0;
            }
        }
        
        // Apply remaining damage to health
        float newHealth = Math.max(0, current - remainingDamage);
        boolean died = newHealth <= 0;
        
        return new Health(newHealth, maximum, newShield, regenRate, invulnerableUntil, died);
    }
    
    /**
     * Heal by amount (capped at maximum).
     */
    public Health heal(float amount) {
        if (isDead || amount <= 0) {
            return this;
        }
        return new Health(
            Math.min(maximum, current + amount),
            maximum, shield, regenRate, invulnerableUntil, false
        );
    }
    
    /**
     * Regenerate health over time.
     */
    public Health regenerate(float deltaTime) {
        if (isDead || regenRate <= 0 || current >= maximum) {
            return this;
        }
        return heal(regenRate * deltaTime);
    }
    
    /**
     * Add shield.
     */
    public Health addShield(float amount) {
        return new Health(current, maximum, shield + amount, regenRate, invulnerableUntil, isDead);
    }
    
    /**
     * Make invulnerable for duration.
     */
    public Health makeInvulnerable(float currentTime, float duration) {
        return new Health(
            current, maximum, shield, regenRate,
            currentTime + duration, isDead
        );
    }
    
    /**
     * Fully restore health and shield.
     */
    public Health restore() {
        return new Health(maximum, maximum, 0, regenRate, invulnerableUntil, false);
    }
    
    /**
     * Kill entity immediately.
     */
    public Health kill() {
        return new Health(0, maximum, 0, regenRate, invulnerableUntil, true);
    }
}
