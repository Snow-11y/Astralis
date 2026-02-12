package stellar.snow.astralis.engine.ecs.systems;

import stellar.snow.astralis.engine.ecs.components.Transform;
import stellar.snow.astralis.engine.ecs.components.Velocity;
import stellar.snow.astralis.engine.ecs.core.Archetype;
import stellar.snow.astralis.engine.ecs.core.SnowySystem;
import stellar.snow.astralis.engine.ecs.core.World;
import stellar.snow.astralis.engine.ecs.storage.ComponentArray;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * PhysicsSystem - Integrate velocity into position.
 * 
 * <p>This system handles basic physics integration for all entities
 * that have both Transform and Velocity components. It applies:
 * <ul>
 *   <li>Linear velocity → position</li>
 *   <li>Angular velocity → rotation</li>
 *   <li>Optional gravity</li>
 *   <li>Optional drag/air resistance</li>
 * </ul>
 * 
 * <h2>Performance</h2>
 * Processes entities in tight loops for maximum cache efficiency.
 * Uses SIMD-friendly data access patterns where possible.
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public class PhysicsSystem extends SnowySystem {
    
    private final float gravityY;
    private final float airDrag;
    
    /**
     * Create physics system with default gravity and no drag.
     */
    public PhysicsSystem() {
        this(-9.81f, 0.0f);
    }
    
    /**
     * Create physics system with custom gravity and drag.
     * 
     * @param gravity Gravity acceleration (m/s²), typically -9.81
     * @param drag Air resistance coefficient (0 = none, 1 = instant stop)
     */
    public PhysicsSystem(float gravity, float drag) {
        super("PhysicsSystem");
        this.gravityY = gravity;
        this.airDrag = Math.max(0, Math.min(1, drag));
        
        // This system needs entities with both Transform and Velocity
        require(Transform.class);
        require(Velocity.class);
    }
    
    @Override
    protected void onInitialize(World world) {
        world.getLogger().info("[PhysicsSystem] Initialized with gravity={}, drag={}", 
            gravityY, airDrag);
    }
    
    @Override
    public void update(World world, Archetype archetype, float deltaTime) {
        // Get component arrays
        ComponentArray transformArray = archetype.getArray(Transform.class);
        ComponentArray velocityArray = archetype.getArray(Velocity.class);
        
        if (transformArray == null || velocityArray == null) {
            return; // Shouldn't happen due to require(), but be safe
        }
        
        int entityCount = archetype.size();
        
        // Process all entities in this archetype
        for (int i = 0; i < entityCount; i++) {
            int entityIndex = archetype.getEntityIndex(i);
            
            // Read current transform
            ByteBuffer transformBuf = transformArray.get(entityIndex);
            if (transformBuf == null) continue;
            transformBuf.order(ByteOrder.LITTLE_ENDIAN);
            
            float posX = transformBuf.getFloat(0);
            float posY = transformBuf.getFloat(4);
            float posZ = transformBuf.getFloat(8);
            float rotX = transformBuf.getFloat(12);
            float rotY = transformBuf.getFloat(16);
            float rotZ = transformBuf.getFloat(20);
            float scaleX = transformBuf.getFloat(24);
            float scaleY = transformBuf.getFloat(28);
            float scaleZ = transformBuf.getFloat(32);
            
            // Read current velocity
            ByteBuffer velocityBuf = velocityArray.get(entityIndex);
            if (velocityBuf == null) continue;
            velocityBuf.order(ByteOrder.LITTLE_ENDIAN);
            
            float vx = velocityBuf.getFloat(0);
            float vy = velocityBuf.getFloat(4);
            float vz = velocityBuf.getFloat(8);
            float angularX = velocityBuf.getFloat(12);
            float angularY = velocityBuf.getFloat(16);
            float angularZ = velocityBuf.getFloat(20);
            
            // Apply gravity
            vy += gravityY * deltaTime;
            
            // Apply air drag
            if (airDrag > 0) {
                float dragFactor = (float) Math.pow(1.0 - airDrag, deltaTime);
                vx *= dragFactor;
                vy *= dragFactor;
                vz *= dragFactor;
                angularX *= dragFactor;
                angularY *= dragFactor;
                angularZ *= dragFactor;
            }
            
            // Integrate position
            posX += vx * deltaTime;
            posY += vy * deltaTime;
            posZ += vz * deltaTime;
            
            // Integrate rotation
            rotX += angularX * deltaTime;
            rotY += angularY * deltaTime;
            rotZ += angularZ * deltaTime;
            
            // Normalize angles to [-π, π]
            rotX = normalizeAngle(rotX);
            rotY = normalizeAngle(rotY);
            rotZ = normalizeAngle(rotZ);
            
            // Write back transform (mark as dirty)
            transformBuf.rewind();
            transformBuf.putFloat(0, posX);
            transformBuf.putFloat(4, posY);
            transformBuf.putFloat(8, posZ);
            transformBuf.putFloat(12, rotX);
            transformBuf.putFloat(16, rotY);
            transformBuf.putFloat(20, rotZ);
            transformBuf.putFloat(24, scaleX);
            transformBuf.putFloat(28, scaleY);
            transformBuf.putFloat(32, scaleZ);
            transformBuf.putInt(36, 1); // dirty = true
            
            // Write back velocity
            velocityBuf.rewind();
            velocityBuf.putFloat(0, vx);
            velocityBuf.putFloat(4, vy);
            velocityBuf.putFloat(8, vz);
            velocityBuf.putFloat(12, angularX);
            velocityBuf.putFloat(16, angularY);
            velocityBuf.putFloat(20, angularZ);
        }
    }
    
    /**
     * Normalize angle to [-π, π] range.
     */
    private float normalizeAngle(float angle) {
        angle = angle % (float)(2 * Math.PI);
        if (angle > Math.PI) {
            angle -= 2 * Math.PI;
        } else if (angle < -Math.PI) {
            angle += 2 * Math.PI;
        }
        return angle;
    }
    
    @Override
    protected void onShutdown(World world) {
        world.getLogger().info("[PhysicsSystem] Shutdown");
    }
}
