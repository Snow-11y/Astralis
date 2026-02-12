package stellar.snow.astralis.engine.ecs.util;

import stellar.snow.astralis.engine.ecs.core.Entity;
import stellar.snow.astralis.engine.ecs.core.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * EntityCommandBuffer - Deferred entity operations for thread safety.
 * 
 * <p>Queues entity operations to be executed later on the main thread.
 * Essential for modifying entities from parallel systems or callbacks.
 * 
 * <h2>Problem</h2>
 * Directly modifying entities during iteration can cause:
 * <ul>
 *   <li>ConcurrentModificationException</li>
 *   <li>Invalid archetype transitions</li>
 *   <li>Corrupted entity state</li>
 * </ul>
 * 
 * <h2>Solution</h2>
 * Queue operations and execute them in a safe context:
 * <pre>
 * EntityCommandBuffer commands = new EntityCommandBuffer();
 * 
 * // Queue operations during system update
 * commands.createEntity(builder -> builder
 *     .withTransform(x, y, z)
 *     .withHealth(100)
 * );
 * 
 * commands.destroyEntity(entity);
 * 
 * // Execute all at once (safe point)
 * commands.execute(world);
 * </pre>
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public class EntityCommandBuffer {
    
    private final List<Command> commands;
    private boolean executed;
    
    /**
     * Create empty command buffer.
     */
    public EntityCommandBuffer() {
        this.commands = new ArrayList<>();
        this.executed = false;
    }
    
    // ========================================================================
    // ENTITY CREATION
    // ========================================================================
    
    /**
     * Queue entity creation.
     */
    public EntityCommandBuffer createEntity(Consumer<EntityBuilder> builderFn) {
        ensureNotExecuted();
        commands.add(new CreateEntityCommand(builderFn));
        return this;
    }
    
    /**
     * Queue multiple entity creations.
     */
    public EntityCommandBuffer createEntities(int count, Consumer<EntityBuilder> builderFn) {
        ensureNotExecuted();
        for (int i = 0; i < count; i++) {
            commands.add(new CreateEntityCommand(builderFn));
        }
        return this;
    }
    
    // ========================================================================
    // ENTITY DESTRUCTION
    // ========================================================================
    
    /**
     * Queue entity destruction.
     */
    public EntityCommandBuffer destroyEntity(Entity entity) {
        ensureNotExecuted();
        commands.add(new DestroyEntityCommand(entity));
        return this;
    }
    
    /**
     * Queue multiple entity destructions.
     */
    public EntityCommandBuffer destroyEntities(List<Entity> entities) {
        ensureNotExecuted();
        for (Entity entity : entities) {
            commands.add(new DestroyEntityCommand(entity));
        }
        return this;
    }
    
    // ========================================================================
    // COMPONENT OPERATIONS
    // ========================================================================
    
    /**
     * Queue component addition.
     */
    public <T> EntityCommandBuffer addComponent(Entity entity, Class<T> type, T data) {
        ensureNotExecuted();
        commands.add(new AddComponentCommand(entity, type, data));
        return this;
    }
    
    /**
     * Queue component removal.
     */
    public EntityCommandBuffer removeComponent(Entity entity, Class<?> type) {
        ensureNotExecuted();
        commands.add(new RemoveComponentCommand(entity, type));
        return this;
    }
    
    /**
     * Queue component update.
     */
    public <T> EntityCommandBuffer updateComponent(Entity entity, Class<T> type, T data) {
        ensureNotExecuted();
        commands.add(new UpdateComponentCommand(entity, type, data));
        return this;
    }
    
    // ========================================================================
    // CUSTOM COMMANDS
    // ========================================================================
    
    /**
     * Queue custom operation.
     */
    public EntityCommandBuffer custom(Consumer<World> operation) {
        ensureNotExecuted();
        commands.add(new CustomCommand(operation));
        return this;
    }
    
    // ========================================================================
    // EXECUTION
    // ========================================================================
    
    /**
     * Execute all queued commands.
     */
    public void execute(World world) {
        if (executed) {
            throw new IllegalStateException("Commands already executed");
        }
        
        for (Command command : commands) {
            command.execute(world);
        }
        
        executed = true;
        commands.clear();
    }
    
    /**
     * Get number of queued commands.
     */
    public int size() {
        return commands.size();
    }
    
    /**
     * Check if buffer is empty.
     */
    public boolean isEmpty() {
        return commands.isEmpty();
    }
    
    /**
     * Clear all commands without executing.
     */
    public void clear() {
        commands.clear();
        executed = false;
    }
    
    private void ensureNotExecuted() {
        if (executed) {
            throw new IllegalStateException("Cannot modify executed command buffer");
        }
    }
    
    // ========================================================================
    // COMMAND TYPES
    // ========================================================================
    
    private interface Command {
        void execute(World world);
    }
    
    private record CreateEntityCommand(Consumer<EntityBuilder> builderFn) implements Command {
        @Override
        public void execute(World world) {
            EntityBuilder builder = EntityBuilder.create(world);
            builderFn.accept(builder);
            builder.build();
        }
    }
    
    private record DestroyEntityCommand(Entity entity) implements Command {
        @Override
        public void execute(World world) {
            world.destroyEntity(entity);
        }
    }
    
    private record AddComponentCommand(Entity entity, Class<?> type, Object data) implements Command {
        @Override
        public void execute(World world) {
            world.addComponent(entity, type, data);
        }
    }
    
    private record RemoveComponentCommand(Entity entity, Class<?> type) implements Command {
        @Override
        public void execute(World world) {
            world.removeComponent(entity, type);
        }
    }
    
    private record UpdateComponentCommand(Entity entity, Class<?> type, Object data) implements Command {
        @Override
        public void execute(World world) {
            world.removeComponent(entity, type);
            world.addComponent(entity, type, data);
        }
    }
    
    private record CustomCommand(Consumer<World> operation) implements Command {
        @Override
        public void execute(World world) {
            operation.accept(world);
        }
    }
}
