package stellar.snow.astralis.engine.ecs.components;

import stellar.snow.astralis.engine.ecs.core.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Children - Track child entities in hierarchy.
 * 
 * <p>Stores list of child entities for efficient hierarchy traversal.
 * Works together with Parent component to form bidirectional relationships.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Create parent-child relationship
 * world.addComponent(parent, Children.class, Children.empty());
 * world.addComponent(child, Parent.class, Parent.of(parent));
 * 
 * // Add child
 * Children children = world.getComponent(parent, Children.class);
 * children = children.add(child);
 * world.updateComponent(parent, Children.class, children);
 * </pre>
 * 
 * <h2>Note</h2>
 * This is a heavyweight component (stores list of entities).
 * Only add to entities that actually have children.
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public record Children(List<Entity> entities) {
    
    /**
     * Create empty children list.
     */
    public static Children empty() {
        return new Children(new ArrayList<>());
    }
    
    /**
     * Create with initial children.
     */
    public static Children of(Entity... children) {
        List<Entity> list = new ArrayList<>(children.length);
        Collections.addAll(list, children);
        return new Children(list);
    }
    
    /**
     * Get number of children.
     */
    public int count() {
        return entities.size();
    }
    
    /**
     * Check if has any children.
     */
    public boolean isEmpty() {
        return entities.isEmpty();
    }
    
    /**
     * Check if contains child.
     */
    public boolean contains(Entity child) {
        return entities.contains(child);
    }
    
    /**
     * Get child at index.
     */
    public Entity get(int index) {
        return entities.get(index);
    }
    
    /**
     * Add child.
     */
    public Children add(Entity child) {
        List<Entity> newList = new ArrayList<>(entities);
        if (!newList.contains(child)) {
            newList.add(child);
        }
        return new Children(newList);
    }
    
    /**
     * Remove child.
     */
    public Children remove(Entity child) {
        List<Entity> newList = new ArrayList<>(entities);
        newList.remove(child);
        return new Children(newList);
    }
    
    /**
     * Get read-only view of children.
     */
    public List<Entity> getEntities() {
        return Collections.unmodifiableList(entities);
    }
}
