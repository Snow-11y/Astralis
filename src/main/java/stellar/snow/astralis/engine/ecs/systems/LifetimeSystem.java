package stellar.snow.astralis.engine.ecs.systems;

import stellar.snow.astralis.engine.ecs.components.Lifetime;
import stellar.snow.astralis.engine.ecs.core.Archetype;
import stellar.snow.astralis.engine.ecs.core.Entity;
import stellar.snow.astralis.engine.ecs.core.SnowySystem;
import stellar.snow.astralis.engine.ecs.core.World;
import stellar.snow.astralis.engine.ecs.storage.ComponentArray;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * LifetimeSystem - Destroy entities when their lifetime expires.
 * 
 * <p>Automatically removes entities with Lifetime component when
 * their destroyAt timestamp is reached. Useful for:
 * <ul>
 *   <li>Particles that fade away</li>
 *   <li>Temporary visual effects</li>
 *   <li>Projectiles with range limits</li>
 *   <li>Timed powerups</li>
 *   <li>Despawning dropped items</li>
 * </ul>
 * 
 * <h2>Performance</h2>
 * Batches entity destruction to minimize archetype graph modifications.
 * Processes entities in a single pass.
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public class LifetimeSystem extends SnowySystem {
    
    private final List<Entity> entitiesToDestroy;
    private double currentTime;
    private long entitiesDestroyedTotal;
    
    /**
     * Create lifetime system.
     */
    public LifetimeSystem() {
        super("LifetimeSystem");
        this.entitiesToDestroy = new ArrayList<>();
        this.currentTime = 0;
        this.entitiesDestroyedTotal = 0;
        
        // Only process entities with Lifetime component
        require(Lifetime.class);
    }
    
    @Override
    protected void onInitialize(World world) {
        world.getLogger().info("[LifetimeSystem] Initialized");
    }
    
    @Override
    protected void onBeforeUpdate(World world, float deltaTime) {
        // Update current time
        currentTime += deltaTime;
        entitiesToDestroy.clear();
    }
    
    @Override
    public void update(World world, Archetype archetype, float deltaTime) {
        ComponentArray lifetimeArray = archetype.getArray(Lifetime.class);
        if (lifetimeArray == null) {
            return;
        }
        
        int entityCount = archetype.size();
        
        // Check each entity's lifetime
        for (int i = 0; i < entityCount; i++) {
            int entityIndex = archetype.getEntityIndex(i);
            
            ByteBuffer lifetimeBuf = lifetimeArray.get(entityIndex);
            if (lifetimeBuf == null) continue;
            lifetimeBuf.order(ByteOrder.LITTLE_ENDIAN);
            
            double createdAt = lifetimeBuf.getDouble(0);
            double destroyAt = lifetimeBuf.getDouble(8);
            
            // Check if expired
            if (currentTime >= destroyAt) {
                // Get the entity and mark for destruction
                Entity entity = archetype.getEntity(i);
                if (entity != null) {
                    entitiesToDestroy.add(entity);
                }
            }
        }
    }
    
    @Override
    protected void onAfterUpdate(World world, float deltaTime) {
        // Destroy all expired entities in batch
        if (!entitiesToDestroy.isEmpty()) {
            int count = entitiesToDestroy.size();
            
            for (Entity entity : entitiesToDestroy) {
                world.destroyEntity(entity);
            }
            
            entitiesDestroyedTotal += count;
            
            if (count > 0) {
                world.getLogger().trace("[LifetimeSystem] Destroyed {} expired entities (total: {})", 
                    count, entitiesDestroyedTotal);
            }
            
            entitiesToDestroy.clear();
        }
    }
    
    /**
     * Get total number of entities destroyed by this system.
     */
    public long getEntitiesDestroyedTotal() {
        return entitiesDestroyedTotal;
    }
    
    /**
     * Get current simulation time.
     */
    public double getCurrentTime() {
        return currentTime;
    }
    
    /**
     * Reset simulation time.
     */
    public void resetTime() {
        currentTime = 0;
    }
    
    @Override
    protected void onShutdown(World world) {
        world.getLogger().info("[LifetimeSystem] Shutdown - Total entities destroyed: {}", 
            entitiesDestroyedTotal);
    }
}
