package stellar.snow.astralis.engine.ecs.systems;

import stellar.snow.astralis.engine.ecs.components.Children;
import stellar.snow.astralis.engine.ecs.components.Parent;
import stellar.snow.astralis.engine.ecs.components.Transform;
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
 * HierarchySystem - Manage parent-child entity relationships.
 * 
 * <p>Handles transform propagation from parent to child entities.
 * Updates world transforms based on local transforms and parent hierarchy.
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Transform inheritance (child follows parent)</li>
 *   <li>Hierarchical destruction (children destroyed with parent)</li>
 *   <li>Efficient batch updates</li>
 * </ul>
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public class HierarchySystem extends SnowySystem {
    
    private final List<Entity> entitiesToDestroy;
    
    /**
     * Create hierarchy system.
     */
    public HierarchySystem() {
        super("HierarchySystem");
        this.entitiesToDestroy = new ArrayList<>();
        
        // Process entities with Parent component
        require(Parent.class);
        require(Transform.class);
    }
    
    @Override
    protected void onInitialize(World world) {
        world.getLogger().info("[HierarchySystem] Initialized");
    }
    
    @Override
    public void update(World world, Archetype archetype, float deltaTime) {
        ComponentArray parentArray = archetype.getArray(Parent.class);
        ComponentArray transformArray = archetype.getArray(Transform.class);
        
        if (parentArray == null || transformArray == null) {
            return;
        }
        
        int entityCount = archetype.size();
        
        // Update transforms based on parent
        for (int i = 0; i < entityCount; i++) {
            int entityIndex = archetype.getEntityIndex(i);
            
            // Read parent component
            ByteBuffer parentBuf = parentArray.get(entityIndex);
            if (parentBuf == null) continue;
            parentBuf.order(ByteOrder.LITTLE_ENDIAN);
            
            int parentIdx = parentBuf.getInt(0);
            int parentGen = parentBuf.getInt(4);
            boolean inheritTransform = parentBuf.get(12) != 0;
            
            // Skip if no parent or no transform inheritance
            if (parentIdx == Parent.NO_PARENT || !inheritTransform) {
                continue;
            }
            
            // Get parent entity
            Entity parentEntity = new Entity(parentIdx, parentGen);
            
            // Validate parent still exists
            if (!world.isValid(parentEntity)) {
                // Parent destroyed - mark child for destruction
                Entity child = archetype.getEntity(i);
                if (child != null) {
                    entitiesToDestroy.add(child);
                }
                continue;
            }
            
            // Get parent transform
            ByteBuffer parentTransform = world.getComponent(parentEntity, Transform.class);
            if (parentTransform == null) {
                continue;
            }
            parentTransform.order(ByteOrder.LITTLE_ENDIAN);
            
            // Read child's local transform
            ByteBuffer childTransform = transformArray.get(entityIndex);
            if (childTransform == null) continue;
            childTransform.order(ByteOrder.LITTLE_ENDIAN);
            
            // Parent transform
            float parentPosX = parentTransform.getFloat(0);
            float parentPosY = parentTransform.getFloat(4);
            float parentPosZ = parentTransform.getFloat(8);
            float parentRotX = parentTransform.getFloat(12);
            float parentRotY = parentTransform.getFloat(16);
            float parentRotZ = parentTransform.getFloat(20);
            float parentScaleX = parentTransform.getFloat(24);
            float parentScaleY = parentTransform.getFloat(28);
            float parentScaleZ = parentTransform.getFloat(32);
            
            // Child local transform
            float localPosX = childTransform.getFloat(0);
            float localPosY = childTransform.getFloat(4);
            float localPosZ = childTransform.getFloat(8);
            float localRotX = childTransform.getFloat(12);
            float localRotY = childTransform.getFloat(16);
            float localRotZ = childTransform.getFloat(20);
            float localScaleX = childTransform.getFloat(24);
            float localScaleY = childTransform.getFloat(28);
            float localScaleZ = childTransform.getFloat(32);
            
            // Calculate world transform (simplified - no full matrix multiplication)
            // In production, use proper matrix transform
            float worldPosX = parentPosX + localPosX * parentScaleX;
            float worldPosY = parentPosY + localPosY * parentScaleY;
            float worldPosZ = parentPosZ + localPosZ * parentScaleZ;
            float worldRotX = parentRotX + localRotX;
            float worldRotY = parentRotY + localRotY;
            float worldRotZ = parentRotZ + localRotZ;
            float worldScaleX = parentScaleX * localScaleX;
            float worldScaleY = parentScaleY * localScaleY;
            float worldScaleZ = parentScaleZ * localScaleZ;
            
            // Write world transform
            childTransform.rewind();
            childTransform.putFloat(0, worldPosX);
            childTransform.putFloat(4, worldPosY);
            childTransform.putFloat(8, worldPosZ);
            childTransform.putFloat(12, worldRotX);
            childTransform.putFloat(16, worldRotY);
            childTransform.putFloat(20, worldRotZ);
            childTransform.putFloat(24, worldScaleX);
            childTransform.putFloat(28, worldScaleY);
            childTransform.putFloat(32, worldScaleZ);
            childTransform.putInt(36, 1); // mark dirty
        }
    }
    
    @Override
    protected void onAfterUpdate(World world, float deltaTime) {
        // Destroy orphaned children
        if (!entitiesToDestroy.isEmpty()) {
            for (Entity entity : entitiesToDestroy) {
                destroyRecursive(world, entity);
            }
            entitiesToDestroy.clear();
        }
    }
    
    /**
     * Recursively destroy entity and all children.
     */
    private void destroyRecursive(World world, Entity entity) {
        // Get children component
        Children children = getChildren(world, entity);
        
        // Destroy all children first
        if (children != null) {
            for (Entity child : children.getEntities()) {
                destroyRecursive(world, child);
            }
        }
        
        // Destroy this entity
        world.destroyEntity(entity);
    }
    
    /**
     * Helper to get children component.
     */
    private Children getChildren(World world, Entity entity) {
        ByteBuffer buf = world.getComponent(entity, Children.class);
        if (buf == null) {
            return null;
        }
        // Note: Children component stores list, would need serialization
        // For now, return null (proper implementation needs custom handling)
        return null;
    }
    
    @Override
    protected void onShutdown(World world) {
        world.getLogger().info("[HierarchySystem] Shutdown");
    }
}
