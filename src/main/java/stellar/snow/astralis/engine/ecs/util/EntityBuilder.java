package stellar.snow.astralis.engine.ecs.util;

import stellar.snow.astralis.engine.ecs.components.*;
import stellar.snow.astralis.engine.ecs.core.Entity;
import stellar.snow.astralis.engine.ecs.core.World;

import java.util.ArrayList;
import java.util.List;

/**
 * EntityBuilder - Fluent API for creating entities with components.
 * 
 * <p>Provides a clean, readable way to construct entities:
 * <pre>
 * Entity player = EntityBuilder.create(world)
 *     .withTransform(0, 0, 0)
 *     .withVelocity(0, 0, 0)
 *     .withHealth(100)
 *     .withTag(Tag.PLAYER | Tag.COLLIDABLE)
 *     .build();
 * </pre>
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Type-safe component addition</li>
 *   <li>Sensible defaults for common components</li>
 *   <li>Batch component registration</li>
 *   <li>Reusable builder instances</li>
 * </ul>
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public class EntityBuilder {
    
    private final World world;
    private final List<ComponentData> components;
    
    /**
     * Component data holder.
     */
    private record ComponentData(Class<?> type, Object data) {}
    
    private EntityBuilder(World world) {
        this.world = world;
        this.components = new ArrayList<>();
    }
    
    /**
     * Create a new entity builder.
     */
    public static EntityBuilder create(World world) {
        return new EntityBuilder(world);
    }
    
    // ========================================================================
    // TRANSFORM
    // ========================================================================
    
    /**
     * Add Transform component at origin.
     */
    public EntityBuilder withTransform() {
        return withTransform(Transform.identity());
    }
    
    /**
     * Add Transform component at position.
     */
    public EntityBuilder withTransform(float x, float y, float z) {
        return withTransform(Transform.at(x, y, z));
    }
    
    /**
     * Add Transform component with full parameters.
     */
    public EntityBuilder withTransform(float x, float y, float z, float yaw, float pitch, float roll) {
        return withTransform(Transform.of(x, y, z, yaw, pitch, roll));
    }
    
    /**
     * Add Transform component.
     */
    public EntityBuilder withTransform(Transform transform) {
        components.add(new ComponentData(Transform.class, transform));
        return this;
    }
    
    // ========================================================================
    // VELOCITY
    // ========================================================================
    
    /**
     * Add Velocity component (stationary).
     */
    public EntityBuilder withVelocity() {
        return withVelocity(Velocity.zero());
    }
    
    /**
     * Add Velocity component with linear velocity.
     */
    public EntityBuilder withVelocity(float vx, float vy, float vz) {
        return withVelocity(new Velocity(vx, vy, vz, 0, 0, 0));
    }
    
    /**
     * Add Velocity component.
     */
    public EntityBuilder withVelocity(Velocity velocity) {
        components.add(new ComponentData(Velocity.class, velocity));
        return this;
    }
    
    // ========================================================================
    // HEALTH
    // ========================================================================
    
    /**
     * Add Health component with max HP.
     */
    public EntityBuilder withHealth(float maxHp) {
        return withHealth(Health.withMax(maxHp));
    }
    
    /**
     * Add Health component with max HP and regeneration.
     */
    public EntityBuilder withHealth(float maxHp, float regenPerSecond) {
        return withHealth(Health.withRegen(maxHp, regenPerSecond));
    }
    
    /**
     * Add Health component.
     */
    public EntityBuilder withHealth(Health health) {
        components.add(new ComponentData(Health.class, health));
        return this;
    }
    
    // ========================================================================
    // LIFETIME
    // ========================================================================
    
    /**
     * Add Lifetime component with duration.
     */
    public EntityBuilder withLifetime(float durationSeconds) {
        return withLifetime(Lifetime.of(durationSeconds, world.getTime()));
    }
    
    /**
     * Add Lifetime component.
     */
    public EntityBuilder withLifetime(Lifetime lifetime) {
        components.add(new ComponentData(Lifetime.class, lifetime));
        return this;
    }
    
    // ========================================================================
    // TAG
    // ========================================================================
    
    /**
     * Add Tag component with tag bits.
     */
    public EntityBuilder withTag(long tagMask) {
        return withTag(new Tag(tagMask));
    }
    
    /**
     * Add Tag component.
     */
    public EntityBuilder withTag(Tag tag) {
        components.add(new ComponentData(Tag.class, tag));
        return this;
    }
    
    // ========================================================================
    // RENDER INFO
    // ========================================================================
    
    /**
     * Add RenderInfo component with model and texture.
     */
    public EntityBuilder withRenderInfo(int modelId, int textureId) {
        return withRenderInfo(RenderInfo.of(modelId, textureId));
    }
    
    /**
     * Add RenderInfo component with model, texture, and color.
     */
    public EntityBuilder withRenderInfo(int modelId, int textureId, int color) {
        return withRenderInfo(RenderInfo.of(modelId, textureId, color));
    }
    
    /**
     * Add RenderInfo component.
     */
    public EntityBuilder withRenderInfo(RenderInfo renderInfo) {
        components.add(new ComponentData(RenderInfo.class, renderInfo));
        return this;
    }
    
    // ========================================================================
    // CUSTOM COMPONENTS
    // ========================================================================
    
    /**
     * Add custom component.
     */
    public <T> EntityBuilder with(Class<T> componentClass, T data) {
        components.add(new ComponentData(componentClass, data));
        return this;
    }
    
    // ========================================================================
    // BUILD
    // ========================================================================
    
    /**
     * Create the entity with all specified components.
     */
    public Entity build() {
        // Create entity
        Entity entity = world.createEntity();
        
        // Add all components
        for (ComponentData comp : components) {
            world.addComponent(entity, comp.type, comp.data);
        }
        
        // Clear for reuse
        components.clear();
        
        return entity;
    }
    
    /**
     * Build multiple copies of this entity.
     */
    public List<Entity> buildMultiple(int count) {
        List<Entity> entities = new ArrayList<>(count);
        
        // Store component data
        List<ComponentData> templateComponents = new ArrayList<>(components);
        
        for (int i = 0; i < count; i++) {
            // Restore template
            components.clear();
            components.addAll(templateComponents);
            
            // Build entity
            entities.add(build());
        }
        
        return entities;
    }
    
    /**
     * Clear all components without building.
     */
    public EntityBuilder reset() {
        components.clear();
        return this;
    }
}
