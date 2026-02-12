package stellar.snow.astralis.engine.ecs.components;

import stellar.snow.astralis.engine.ecs.storage.StructFlatteningRegistryV2.*;

/**
 * Example nested struct components that showcase the superiority over Kirino's approach.
 * 
 * These demonstrate:
 * 1. Clean, intuitive POJO design
 * 2. Arbitrary nesting depth
 * 3. Automatic flattening to cache-friendly primitives
 * 4. Path-based access (e.g., "transform.position.x")
 */
public class ExampleNestedStructs {

    // ========================================================================
    // NESTED STRUCT DEFINITIONS
    // ========================================================================

    /**
     * 3D Vector - The fundamental building block.
     */
    @NestedStruct
    public static class Vec3 {
        public float x;
        public float y;
        public float z;

        public Vec3() {}

        public Vec3(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /**
     * 2D Vector.
     */
    @NestedStruct
    public static class Vec2 {
        public float x;
        public float y;

        public Vec2() {}

        public Vec2(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Quaternion for rotations.
     */
    @NestedStruct
    public static class Quaternion {
        public float x;
        public float y;
        public float z;
        public float w;

        public Quaternion() {}

        public Quaternion(float x, float y, float z, float w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }
    }

    /**
     * 4x4 Matrix - Nested Vec4s for each row.
     * This demonstrates DEEP NESTING.
     */
    @NestedStruct
    public static class Vec4 {
        public float x, y, z, w;

        public Vec4() {}

        public Vec4(float x, float y, float z, float w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }
    }

    @NestedStruct
    public static class Mat4 {
        public Vec4 row0;
        public Vec4 row1;
        public Vec4 row2;
        public Vec4 row3;

        public Mat4() {}
    }

    /**
     * Color with alpha.
     */
    @NestedStruct
    public static class Color {
        public float r;
        public float g;
        public float b;
        public float a;

        public Color() {}

        public Color(float r, float g, float b, float a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }
    }

    /**
     * Bounding box using two Vec3s.
     */
    @NestedStruct
    public static class AABB_Struct {
        public Vec3 min;
        public Vec3 max;

        public AABB_Struct() {}
    }

    // ========================================================================
    // COMPONENT DEFINITIONS - THE KIRINO CRUSHERS
    // ========================================================================

    /**
     * Transform component with nested position, rotation, scale.
     * 
     * Flattened layout (10 floats):
     * - position.x, position.y, position.z
     * - rotation.x, rotation.y, rotation.z, rotation.w
     * - scale.x, scale.y, scale.z
     * 
     * Access examples:
     * - get(entity, "position.x")
     * - get(entity, "rotation.w")
     * - get(entity, "scale.y")
     */
    @Flattened(cacheAlign = true, vectorized = true)
    public static class TransformComponent {
        public Vec3 position;
        public Quaternion rotation;
        public Vec3 scale;

        public TransformComponent() {}
    }

    /**
     * Physics component with nested velocity and acceleration.
     * 
     * Flattened layout (9 floats + 1 int + 1 boolean):
     * - velocity.x, velocity.y, velocity.z
     * - acceleration.x, acceleration.y, acceleration.z
     * - angularVelocity.x, angularVelocity.y, angularVelocity.z
     * - mass (float)
     * - collisionLayer (int)
     * - isKinematic (boolean)
     */
    @Flattened(vectorized = true)
    public static class PhysicsComponent {
        public Vec3 velocity;
        public Vec3 acceleration;
        public Vec3 angularVelocity;
        public float mass;
        public int collisionLayer;
        public boolean isKinematic;

        public PhysicsComponent() {}
    }

    /**
     * Render component with nested transform matrix and color.
     * 
     * This demonstrates TRIPLE NESTING:
     * - Mat4 contains Vec4s
     * - Vec4 contains floats
     * 
     * Flattened layout (16 + 4 + 1 = 21 fields):
     * - modelMatrix.row0.x, modelMatrix.row0.y, modelMatrix.row0.z, modelMatrix.row0.w
     * - modelMatrix.row1.x, modelMatrix.row1.y, modelMatrix.row1.z, modelMatrix.row1.w
     * - modelMatrix.row2.x, modelMatrix.row2.y, modelMatrix.row2.z, modelMatrix.row2.w
     * - modelMatrix.row3.x, modelMatrix.row3.y, modelMatrix.row3.z, modelMatrix.row3.w
     * - tint.r, tint.g, tint.b, tint.a
     * - visible (boolean)
     */
    @Flattened(cacheAlign = true, vectorized = true)
    public static class RenderComponent {
        public Mat4 modelMatrix;
        public Color tint;
        public boolean visible;

        public RenderComponent() {}
    }

    /**
     * Particle component with nested properties.
     * 
     * Demonstrates mixing nested structs and primitives.
     */
    @Flattened(vectorized = true)
    public static class ParticleComponent {
        public Vec3 position;
        public Vec3 velocity;
        public Color color;
        public float lifetime;
        public float age;
        public float size;
        public boolean alive;

        public ParticleComponent() {}
    }

    /**
     * Light component with nested position and color.
     */
    @Flattened
    public static class LightComponent {
        public Vec3 position;
        public Color color;
        public float intensity;
        public float radius;
        public int lightType;  // 0=point, 1=spot, 2=directional

        public LightComponent() {}
    }

    /**
     * Camera component with nested position and target.
     */
    @Flattened(cacheAlign = true)
    public static class CameraComponent {
        public Vec3 position;
        public Vec3 target;
        public Vec3 up;
        public float fov;
        public float nearClip;
        public float farClip;
        public boolean isActive;

        public CameraComponent() {}
    }

    /**
     * Collider component with nested AABB.
     */
    @Flattened
    public static class ColliderComponent {
        public AABB_Struct bounds;
        public int layer;
        public boolean isTrigger;

        public ColliderComponent() {}
    }

    /**
     * Sprite component with texture coordinates.
     */
    @Flattened(vectorized = true)
    public static class SpriteComponent {
        public Vec2 uvMin;
        public Vec2 uvMax;
        public Color tint;
        public int textureId;
        public float depth;

        public SpriteComponent() {}
    }

    /**
     * Advanced: Nested hierarchy component.
     * 
     * This shows QUADRUPLE nesting:
     * - localTransform (Vec3 + Quaternion + Vec3)
     * - worldTransform (Vec3 + Quaternion + Vec3)
     * - bounds (AABB with Vec3 min/max)
     * 
     * Total: 3+4+3 + 3+4+3 + 3+3 = 26 floats
     */
    @Flattened(cacheAlign = true, vectorized = true)
    public static class HierarchyComponent {
        // Local space transform (relative to parent)
        public Vec3 localPosition;
        public Quaternion localRotation;
        public Vec3 localScale;
        
        // World space transform (computed)
        public Vec3 worldPosition;
        public Quaternion worldRotation;
        public Vec3 worldScale;
        
        // World-space bounds
        public AABB_Struct worldBounds;
        
        // Hierarchy info (primitives)
        public int parentId;
        public int childCount;
        public boolean isDirty;

        public HierarchyComponent() {}
    }
}
