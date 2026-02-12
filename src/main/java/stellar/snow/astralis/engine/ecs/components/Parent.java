package stellar.snow.astralis.engine.ecs.components;

import stellar.snow.astralis.engine.ecs.core.Entity;

/**
 * Parent - Entity hierarchy component.
 * 
 * <p>Establishes parent-child relationships between entities.
 * Used for:
 * <ul>
 *   <li>Scene graphs (transform hierarchies)</li>
 *   <li>Object composition (gun attached to player)</li>
 *   <li>Ownership tracking (inventory items)</li>
 *   <li>Hierarchical deletion (destroy children with parent)</li>
 * </ul>
 * 
 * <h2>Memory Layout</h2>
 * <pre>
 * Offset | Size | Field
 * -------|------|-------
 *   0    |  4   | parentIndex (int)
 *   4    |  4   | parentGeneration (int)
 *   8    |  4   | childCount (int)
 *  12    |  1   | inheritTransform (boolean)
 *  13    |  3   | padding
 * Total: 16 bytes
 * </pre>
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public record Parent(
    int parentIndex,
    int parentGeneration,
    int childCount,
    boolean inheritTransform
) {
    /** Component size in bytes */
    public static final int SIZE = 16;
    
    /** Component alignment */
    public static final int ALIGNMENT = 4;
    
    /** Special value for no parent */
    public static final int NO_PARENT = -1;
    
    /**
     * Create parent reference with transform inheritance.
     */
    public static Parent of(Entity parent) {
        return new Parent(parent.index(), parent.generation(), 0, true);
    }
    
    /**
     * Create parent reference without transform inheritance.
     */
    public static Parent ofNoInherit(Entity parent) {
        return new Parent(parent.index(), parent.generation(), 0, false);
    }
    
    /**
     * Create root (no parent).
     */
    public static Parent root() {
        return new Parent(NO_PARENT, 0, 0, false);
    }
    
    /**
     * Check if this entity has a parent.
     */
    public boolean hasParent() {
        return parentIndex != NO_PARENT;
    }
    
    /**
     * Get parent entity.
     */
    public Entity getParent() {
        return hasParent() ? new Entity(parentIndex, parentGeneration) : null;
    }
    
    /**
     * Check if parent matches entity.
     */
    public boolean isParent(Entity entity) {
        return parentIndex == entity.index() && parentGeneration == entity.generation();
    }
    
    /**
     * Update child count.
     */
    public Parent withChildCount(int count) {
        return new Parent(parentIndex, parentGeneration, count, inheritTransform);
    }
    
    /**
     * Increment child count.
     */
    public Parent addChild() {
        return withChildCount(childCount + 1);
    }
    
    /**
     * Decrement child count.
     */
    public Parent removeChild() {
        return withChildCount(Math.max(0, childCount - 1));
    }
    
    /**
     * Set transform inheritance.
     */
    public Parent setInheritTransform(boolean inherit) {
        return new Parent(parentIndex, parentGeneration, childCount, inherit);
    }
}
