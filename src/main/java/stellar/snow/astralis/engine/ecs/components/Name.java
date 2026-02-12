package stellar.snow.astralis.engine.ecs.components;

/**
 * Name - Debug name for entities.
 * 
 * <p>Stores a string identifier for debugging and inspection.
 * Useful in development but can be stripped in production builds.
 * 
 * <h2>Usage</h2>
 * <pre>
 * Entity player = EntityBuilder.create(world)
 *     .with(Name.class, Name.of("Player"))
 *     .withTransform(0, 0, 0)
 *     .build();
 * 
 * // Later in debugging
 * Name name = world.getComponent(entity, Name.class);
 * System.out.println("Entity: " + name.value());
 * </pre>
 * 
 * <h2>Note</h2>
 * This is a heavyweight component (stores String).
 * Only use for debugging or entities that truly need names.
 * Consider using Tag component for categorization instead.
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public record Name(String value) {
    
    /**
     * Create name component.
     */
    public static Name of(String name) {
        return new Name(name != null ? name : "Unnamed");
    }
    
    /**
     * Get name value.
     */
    public String get() {
        return value;
    }
    
    /**
     * Check if name matches.
     */
    public boolean is(String other) {
        return value.equals(other);
    }
    
    /**
     * Check if name contains substring.
     */
    public boolean contains(String substring) {
        return value.contains(substring);
    }
    
    /**
     * Get name with prefix.
     */
    public Name withPrefix(String prefix) {
        return new Name(prefix + value);
    }
    
    /**
     * Get name with suffix.
     */
    public Name withSuffix(String suffix) {
        return new Name(value + suffix);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
