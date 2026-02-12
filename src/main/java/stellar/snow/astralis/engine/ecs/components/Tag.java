package stellar.snow.astralis.engine.ecs.components;

/**
 * Tag - Bit-based tagging system for fast entity categorization.
 * 
 * <p>Uses a 64-bit mask to store up to 64 different boolean tags.
 * Extremely efficient for filtering and querying entities by category.
 * 
 * <h2>Common Tag Bits</h2>
 * <pre>
 * Bit | Tag
 * ----|--------------------
 *  0  | Player
 *  1  | Enemy
 *  2  | Projectile
 *  3  | Particle
 *  4  | Obstacle
 *  5  | Trigger
 *  6  | Visible
 *  7  | Collidable
 *  8  | Persistent
 *  9  | Temporary
 * 10  | AI_Controlled
 * 11  | Physics_Enabled
 * ... | (custom tags)
 * </pre>
 * 
 * <h2>Memory Layout</h2>
 * <pre>
 * Offset | Size | Field
 * -------|------|-------
 *   0    |  8   | mask (long)
 * Total: 8 bytes
 * </pre>
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public record Tag(long mask) {
    /** Component size in bytes */
    public static final int SIZE = 8;
    
    /** Component alignment */
    public static final int ALIGNMENT = 8;
    
    // Common tag definitions
    public static final long PLAYER = 1L << 0;
    public static final long ENEMY = 1L << 1;
    public static final long PROJECTILE = 1L << 2;
    public static final long PARTICLE = 1L << 3;
    public static final long OBSTACLE = 1L << 4;
    public static final long TRIGGER = 1L << 5;
    public static final long VISIBLE = 1L << 6;
    public static final long COLLIDABLE = 1L << 7;
    public static final long PERSISTENT = 1L << 8;
    public static final long TEMPORARY = 1L << 9;
    public static final long AI_CONTROLLED = 1L << 10;
    public static final long PHYSICS_ENABLED = 1L << 11;
    public static final long INTERACTABLE = 1L << 12;
    public static final long INVULNERABLE = 1L << 13;
    public static final long FLYING = 1L << 14;
    public static final long UNDERWATER = 1L << 15;
    
    /**
     * Empty tag (no tags set).
     */
    public static Tag empty() {
        return new Tag(0);
    }
    
    /**
     * Tag with single bit set.
     */
    public static Tag of(long tagBit) {
        return new Tag(tagBit);
    }
    
    /**
     * Tag with multiple bits set.
     */
    public static Tag of(long... tagBits) {
        long combined = 0;
        for (long bit : tagBits) {
            combined |= bit;
        }
        return new Tag(combined);
    }
    
    /**
     * Check if tag is set.
     */
    public boolean has(long tagBit) {
        return (mask & tagBit) != 0;
    }
    
    /**
     * Check if all tags are set.
     */
    public boolean hasAll(long tagMask) {
        return (mask & tagMask) == tagMask;
    }
    
    /**
     * Check if any tag is set.
     */
    public boolean hasAny(long tagMask) {
        return (mask & tagMask) != 0;
    }
    
    /**
     * Add tag(s).
     */
    public Tag add(long tagBit) {
        return new Tag(mask | tagBit);
    }
    
    /**
     * Add multiple tags.
     */
    public Tag add(long... tagBits) {
        long newMask = mask;
        for (long bit : tagBits) {
            newMask |= bit;
        }
        return new Tag(newMask);
    }
    
    /**
     * Remove tag(s).
     */
    public Tag remove(long tagBit) {
        return new Tag(mask & ~tagBit);
    }
    
    /**
     * Remove multiple tags.
     */
    public Tag remove(long... tagBits) {
        long newMask = mask;
        for (long bit : tagBits) {
            newMask &= ~bit;
        }
        return new Tag(newMask);
    }
    
    /**
     * Toggle tag(s).
     */
    public Tag toggle(long tagBit) {
        return new Tag(mask ^ tagBit);
    }
    
    /**
     * Clear all tags.
     */
    public Tag clear() {
        return empty();
    }
    
    /**
     * Count number of tags set.
     */
    public int count() {
        return Long.bitCount(mask);
    }
    
    /**
     * Check if no tags are set.
     */
    public boolean isEmpty() {
        return mask == 0;
    }
}
