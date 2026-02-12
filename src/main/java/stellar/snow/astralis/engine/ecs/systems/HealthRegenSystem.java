package stellar.snow.astralis.engine.ecs.systems;

import stellar.snow.astralis.engine.ecs.components.Health;
import stellar.snow.astralis.engine.ecs.core.Archetype;
import stellar.snow.astralis.engine.ecs.core.SnowySystem;
import stellar.snow.astralis.engine.ecs.core.World;
import stellar.snow.astralis.engine.ecs.storage.ComponentArray;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * HealthRegenSystem - Regenerate health over time.
 * 
 * <p>Automatically restores health for entities with positive regenRate.
 * Stops regenerating when health reaches maximum.
 * Does not regenerate dead entities.
 * 
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Player health regeneration</li>
 *   <li>Enemy self-healing</li>
 *   <li>Shield recharge</li>
 *   <li>Building repair</li>
 * </ul>
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public class HealthRegenSystem extends SnowySystem {
    
    private long entitiesRegeneratedTotal;
    private float healthRegeneratedTotal;
    
    /**
     * Create health regeneration system.
     */
    public HealthRegenSystem() {
        super("HealthRegenSystem");
        this.entitiesRegeneratedTotal = 0;
        this.healthRegeneratedTotal = 0;
        
        // Only process entities with Health component
        require(Health.class);
    }
    
    @Override
    protected void onInitialize(World world) {
        world.getLogger().info("[HealthRegenSystem] Initialized");
    }
    
    @Override
    public void update(World world, Archetype archetype, float deltaTime) {
        ComponentArray healthArray = archetype.getArray(Health.class);
        if (healthArray == null) {
            return;
        }
        
        int entityCount = archetype.size();
        int regeneratedThisFrame = 0;
        float healedThisFrame = 0;
        
        // Process each entity with health
        for (int i = 0; i < entityCount; i++) {
            int entityIndex = archetype.getEntityIndex(i);
            
            ByteBuffer healthBuf = healthArray.get(entityIndex);
            if (healthBuf == null) continue;
            healthBuf.order(ByteOrder.LITTLE_ENDIAN);
            
            float current = healthBuf.getFloat(0);
            float maximum = healthBuf.getFloat(4);
            float shield = healthBuf.getFloat(8);
            float regenRate = healthBuf.getFloat(12);
            float invulnerableUntil = healthBuf.getFloat(16);
            boolean isDead = healthBuf.get(20) != 0;
            
            // Skip if dead, full health, or no regen
            if (isDead || current >= maximum || regenRate <= 0) {
                continue;
            }
            
            // Calculate regen amount
            float regenAmount = regenRate * deltaTime;
            float newHealth = Math.min(maximum, current + regenAmount);
            float actualHealed = newHealth - current;
            
            // Only update if we actually healed
            if (actualHealed > 0.0001f) {
                // Write back new health
                healthBuf.putFloat(0, newHealth);
                
                // Track stats
                regeneratedThisFrame++;
                healedThisFrame += actualHealed;
            }
        }
        
        // Update totals
        if (regeneratedThisFrame > 0) {
            entitiesRegeneratedTotal += regeneratedThisFrame;
            healthRegeneratedTotal += healedThisFrame;
        }
    }
    
    /**
     * Get total number of entities that have regenerated health.
     */
    public long getEntitiesRegeneratedTotal() {
        return entitiesRegeneratedTotal;
    }
    
    /**
     * Get total health regenerated across all entities.
     */
    public float getHealthRegeneratedTotal() {
        return healthRegeneratedTotal;
    }
    
    @Override
    protected void onShutdown(World world) {
        world.getLogger().info("[HealthRegenSystem] Shutdown - " +
            "Entities regenerated: {}, Total HP restored: {:.1f}",
            entitiesRegeneratedTotal, healthRegeneratedTotal);
    }
}
