package stellar.snow.astralis.engine.render.debug;
import org.joml.*;
import java.lang.Math;
import java.util.*;
import java.util.concurrent.*;
/**
 * GizmosManager - Advanced 3D Visual Debugging
 * 
 * Allows developers to draw debug geometry directly in the 3D world:
 * - Bounding boxes (AABB, OBB)
 * - Normals and tangent spaces
 * - Paths and trajectories
 * - Frustums and visibility volumes
 * - Ray casts and physics queries
 * - Custom wireframe geometry
 * 
 * Features:
 * - Color-coded visualization
 * - Depth testing control
 * - Persistence (one-frame vs permanent)
 * - Layer system for filtering
 * - Performance-optimized batch rendering
 */
    
    /**
     * Gizmo types
     */
    public enum GizmoType {
        LINE,
        BOX,
        SPHERE,
        FRUSTUM,
        ARROW,
        GRID,
        CIRCLE,
        CYLINDER,
        CONE,
        CUSTOM_MESH
    }
    
    /**
     * Render layer for filtering
     */
    public enum Layer {
        PHYSICS(0),
        NAVMESH(1),
        AI(2),
        CAMERA(3),
        LIGHTING(4),
        CULLING(5),
        GENERAL(6);
        
        public final int mask;
        
        Layer(int bit) {
            this.mask = 1 << bit;
        }
    }
    
    /**
     * Single gizmo instance
     */
    public static class Gizmo {
        public final GizmoType type;
        public final Layer layer;
        public final Vector4f color; // RGBA
        public final boolean depthTest;
        public final int lifetime; // frames, -1 = permanent
        
        // Transform
        public final Vector3f position;
        public final Quaternionf rotation;
        public final Vector3f scale;
        
        // Type-specific data
        public Object data;
        
        // Metadata
        public String label;
        public long creationTime;
        public int framesAlive = 0;
        
        public Gizmo(GizmoType type, Layer layer, Vector3f pos, Quaternionf rot, Vector3f scale,
                    Vector4f color, boolean depthTest, int lifetime) {
            this.type = type;
            this.layer = layer;
            this.position = new Vector3f(pos);
            this.rotation = new Quaternionf(rot);
            this.scale = new Vector3f(scale);
            this.color = new Vector4f(color);
            this.depthTest = depthTest;
            this.lifetime = lifetime;
            this.creationTime = System.nanoTime();
        }
    }
    
    // Active gizmos
    private final List<Gizmo> gizmos = new CopyOnWriteArrayList<>();
    
    // Layer visibility mask
    private int visibleLayerMask = 0xFFFFFFFF; // All layers visible by default
    
    // Color palette
    public static final Vector4f COLOR_RED = new Vector4f(1, 0, 0, 1);
    public static final Vector4f COLOR_GREEN = new Vector4f(0, 1, 0, 1);
    public static final Vector4f COLOR_BLUE = new Vector4f(0, 0, 1, 1);
    public static final Vector4f COLOR_YELLOW = new Vector4f(1, 1, 0, 1);
    public static final Vector4f COLOR_CYAN = new Vector4f(0, 1, 1, 1);
    public static final Vector4f COLOR_MAGENTA = new Vector4f(1, 0, 1, 1);
    public static final Vector4f COLOR_WHITE = new Vector4f(1, 1, 1, 1);
    public static final Vector4f COLOR_BLACK = new Vector4f(0, 0, 0, 1);
    public static final Vector4f COLOR_ORANGE = new Vector4f(1, 0.5f, 0, 1);
    
    // Statistics
    private long totalGizmosDrawn = 0;
    private long totalGizmosCulled = 0;
    
    /**
     * Draw a line
     */
    public void drawLine(Vector3f start, Vector3f end, Vector4f color, Layer layer, boolean depthTest, int lifetime) {
        Gizmo gizmo = new Gizmo(GizmoType.LINE, layer, start, new Quaternionf(), new Vector3f(1),
            color, depthTest, lifetime);
        gizmo.data = new LineData(start, end);
        gizmos.add(gizmo);
    }
    
    /**
     * Draw a line (one frame)
     */
    public void drawLine(Vector3f start, Vector3f end, Vector4f color) {
        drawLine(start, end, color, Layer.GENERAL, true, 1);
    }
    
    /**
     * Draw an axis-aligned bounding box
     */
    public void drawAABB(Vector3f min, Vector3f max, Vector4f color, Layer layer, boolean depthTest, int lifetime) {
        Vector3f center = new Vector3f(min).add(max).mul(0.5f);
        Vector3f size = new Vector3f(max).sub(min);
        
        Gizmo gizmo = new Gizmo(GizmoType.BOX, layer, center, new Quaternionf(), size,
            color, depthTest, lifetime);
        gizmo.data = new BoxData(min, max, false);
        gizmos.add(gizmo);
    }
    
    /**
     * Draw AABB (one frame)
     */
    public void drawAABB(Vector3f min, Vector3f max, Vector4f color) {
        drawAABB(min, max, color, Layer.GENERAL, true, 1);
    }
    
    /**
     * Draw an oriented bounding box
     */
    public void drawOBB(Vector3f center, Vector3f halfExtents, Quaternionf rotation, 
                       Vector4f color, Layer layer, boolean depthTest, int lifetime) {
        Gizmo gizmo = new Gizmo(GizmoType.BOX, layer, center, rotation, 
            new Vector3f(halfExtents).mul(2), color, depthTest, lifetime);
        gizmo.data = new BoxData(center, halfExtents, true);
        gizmos.add(gizmo);
    }
    
    /**
     * Draw OBB (one frame)
     */
    public void drawOBB(Vector3f center, Vector3f halfExtents, Quaternionf rotation, Vector4f color) {
        drawOBB(center, halfExtents, rotation, color, Layer.GENERAL, true, 1);
    }
    
    /**
     * Draw a sphere
     */
    public void drawSphere(Vector3f center, float radius, Vector4f color, Layer layer, 
                          boolean depthTest, int lifetime) {
        Gizmo gizmo = new Gizmo(GizmoType.SPHERE, layer, center, new Quaternionf(), 
            new Vector3f(radius), color, depthTest, lifetime);
        gizmo.data = new SphereData(center, radius);
        gizmos.add(gizmo);
    }
    
    /**
     * Draw sphere (one frame)
     */
    public void drawSphere(Vector3f center, float radius, Vector4f color) {
        drawSphere(center, radius, color, Layer.GENERAL, true, 1);
    }
    
    /**
     * Draw a camera frustum
     */
    public void drawFrustum(Matrix4f viewProjection, Vector4f color, Layer layer, 
                           boolean depthTest, int lifetime) {
        // Extract frustum corners from inverse view-projection matrix
        Matrix4f invVP = new Matrix4f(viewProjection).invert();
        
        Vector3f[] corners = new Vector3f[8];
        Vector4f[] ndcCorners = {
            new Vector4f(-1, -1, -1, 1), new Vector4f( 1, -1, -1, 1),
            new Vector4f( 1,  1, -1, 1), new Vector4f(-1,  1, -1, 1),
            new Vector4f(-1, -1,  1, 1), new Vector4f( 1, -1,  1, 1),
            new Vector4f( 1,  1,  1, 1), new Vector4f(-1,  1,  1, 1)
        };
        
        for (int i = 0; i < 8; i++) {
            Vector4f worldPos = invVP.transform(ndcCorners[i]);
            worldPos.div(worldPos.w);
            corners[i] = new Vector3f(worldPos.x, worldPos.y, worldPos.z);
        }
        
        Gizmo gizmo = new Gizmo(GizmoType.FRUSTUM, layer, corners[0], new Quaternionf(), 
            new Vector3f(1), color, depthTest, lifetime);
        gizmo.data = new FrustumData(corners);
        gizmos.add(gizmo);
    }
    
    /**
     * Draw frustum (one frame)
     */
    public void drawFrustum(Matrix4f viewProjection, Vector4f color) {
        drawFrustum(viewProjection, color, Layer.CAMERA, true, 1);
    }
    
    /**
     * Draw an arrow
     */
    public void drawArrow(Vector3f start, Vector3f end, float headSize, Vector4f color, 
                         Layer layer, boolean depthTest, int lifetime) {
        Gizmo gizmo = new Gizmo(GizmoType.ARROW, layer, start, new Quaternionf(), 
            new Vector3f(1), color, depthTest, lifetime);
        gizmo.data = new ArrowData(start, end, headSize);
        gizmos.add(gizmo);
    }
    
    /**
     * Draw arrow (one frame)
     */
    public void drawArrow(Vector3f start, Vector3f end, float headSize, Vector4f color) {
        drawArrow(start, end, headSize, color, Layer.GENERAL, true, 1);
    }
    
    /**
     * Draw surface normal
     */
    public void drawNormal(Vector3f position, Vector3f normal, float length, Vector4f color) {
        Vector3f end = new Vector3f(position).add(new Vector3f(normal).mul(length));
        drawArrow(position, end, length * 0.1f, color, Layer.GENERAL, true, 1);
    }
    
    /**
     * Draw a grid
     */
    public void drawGrid(Vector3f center, Vector3f normal, float size, int divisions, 
                        Vector4f color, Layer layer, boolean depthTest, int lifetime) {
        Gizmo gizmo = new Gizmo(GizmoType.GRID, layer, center, new Quaternionf(), 
            new Vector3f(size), color, depthTest, lifetime);
        gizmo.data = new GridData(center, normal, size, divisions);
        gizmos.add(gizmo);
    }
    
    /**
     * Draw grid (one frame)
     */
    public void drawGrid(Vector3f center, Vector3f normal, float size, int divisions, Vector4f color) {
        drawGrid(center, normal, size, divisions, color, Layer.GENERAL, true, 1);
    }
    
    /**
     * Draw a circle
     */
    public void drawCircle(Vector3f center, Vector3f normal, float radius, Vector4f color,
                          Layer layer, boolean depthTest, int lifetime) {
        Gizmo gizmo = new Gizmo(GizmoType.CIRCLE, layer, center, new Quaternionf(),
            new Vector3f(radius), color, depthTest, lifetime);
        gizmo.data = new CircleData(center, normal, radius, 32);
        gizmos.add(gizmo);
    }
    
    /**
     * Draw circle (one frame)
     */
    public void drawCircle(Vector3f center, Vector3f normal, float radius, Vector4f color) {
        drawCircle(center, normal, radius, color, Layer.GENERAL, true, 1);
    }
    
    /**
     * Draw a path
     */
    public void drawPath(List<Vector3f> points, Vector4f color, Layer layer, 
                        boolean depthTest, int lifetime) {
        for (int i = 0; i < points.size() - 1; i++) {
            drawLine(points.get(i), points.get(i + 1), color, layer, depthTest, lifetime);
        }
    }
    
    /**
     * Draw path (one frame)
     */
    public void drawPath(List<Vector3f> points, Vector4f color) {
        drawPath(points, color, Layer.GENERAL, true, 1);
    }
    
    /**
     * Draw coordinate axes at position
     */
    public void drawAxes(Vector3f position, Quaternionf rotation, float size) {
        Matrix4f transform = new Matrix4f().translate(position).rotate(rotation);
        
        Vector3f right = transform.transformDirection(new Vector3f(size, 0, 0));
        Vector3f up = transform.transformDirection(new Vector3f(0, size, 0));
        Vector3f forward = transform.transformDirection(new Vector3f(0, 0, size));
        
        drawArrow(position, new Vector3f(position).add(right), size * 0.1f, COLOR_RED);
        drawArrow(position, new Vector3f(position).add(up), size * 0.1f, COLOR_GREEN);
        drawArrow(position, new Vector3f(position).add(forward), size * 0.1f, COLOR_BLUE);
    }
    
    /**
     * Draw a labeled point
     */
    public void drawPoint(Vector3f position, String label, Vector4f color, Layer layer, int lifetime) {
        drawSphere(position, 0.1f, color, layer, true, lifetime);
        
        Gizmo gizmo = gizmos.get(gizmos.size() - 1);
        gizmo.label = label;
    }
    
    /**
     * Draw point (one frame)
     */
    public void drawPoint(Vector3f position, String label, Vector4f color) {
        drawPoint(position, label, color, Layer.GENERAL, 1);
    }
    
    /**
     * Update gizmos (call once per frame)
     */
    public void update() {
        // Remove expired gizmos
        gizmos.removeIf(gizmo -> {
            if (gizmo.lifetime > 0) {
                gizmo.framesAlive++;
                if (gizmo.framesAlive >= gizmo.lifetime) {
                    return true;
                }
            }
            return false;
        });
    }
    
    /**
     * Get all visible gizmos
     */
    public List<Gizmo> getVisibleGizmos() {
        List<Gizmo> visible = new ArrayList<>();
        for (Gizmo gizmo : gizmos) {
            if ((gizmo.layer.mask & visibleLayerMask) != 0) {
                visible.add(gizmo);
                totalGizmosDrawn++;
            } else {
                totalGizmosCulled++;
            }
        }
        return visible;
    }
    
    /**
     * Set layer visibility
     */
    public void setLayerVisible(Layer layer, boolean visible) {
        if (visible) {
            visibleLayerMask |= layer.mask;
        } else {
            visibleLayerMask &= ~layer.mask;
        }
    }
    
    /**
     * Toggle layer visibility
     */
    public void toggleLayer(Layer layer) {
        visibleLayerMask ^= layer.mask;
    }
    
    /**
     * Check if layer is visible
     */
    public boolean isLayerVisible(Layer layer) {
        return (visibleLayerMask & layer.mask) != 0;
    }
    
    /**
     * Clear all gizmos
     */
    public void clear() {
        gizmos.clear();
    }
    
    /**
     * Clear gizmos on specific layer
     */
    public void clearLayer(Layer layer) {
        gizmos.removeIf(gizmo -> gizmo.layer == layer);
    }
    
    /**
     * Get gizmo count
     */
    public int getGizmoCount() {
        return gizmos.size();
    }
    
    /**
     * Get statistics
     */
    public String getStatistics() {
        return String.format("Gizmos: active=%d, drawn=%d, culled=%d",
            gizmos.size(), totalGizmosDrawn, totalGizmosCulled);
    }
    
    // Data classes for different gizmo types
    
    public static class LineData {
        public final Vector3f start, end;
        
        public LineData(Vector3f start, Vector3f end) {
            this.start = new Vector3f(start);
            this.end = new Vector3f(end);
        }
    }
    
    public static class BoxData {
        public final Vector3f min, max;
        public final boolean oriented;
        
        public BoxData(Vector3f min, Vector3f max, boolean oriented) {
            this.min = new Vector3f(min);
            this.max = new Vector3f(max);
            this.oriented = oriented;
        }
    }
    
    public static class SphereData {
        public final Vector3f center;
        public final float radius;
        
        public SphereData(Vector3f center, float radius) {
            this.center = new Vector3f(center);
            this.radius = radius;
        }
    }
    
    public static class FrustumData {
        public final Vector3f[] corners; // 8 corners
        
        public FrustumData(Vector3f[] corners) {
            this.corners = new Vector3f[8];
            for (int i = 0; i < 8; i++) {
                this.corners[i] = new Vector3f(corners[i]);
            }
        }
    }
    
    public static class ArrowData {
        public final Vector3f start, end;
        public final float headSize;
        
        public ArrowData(Vector3f start, Vector3f end, float headSize) {
            this.start = new Vector3f(start);
            this.end = new Vector3f(end);
            this.headSize = headSize;
        }
    }
    
    public static class GridData {
        public final Vector3f center, normal;
        public final float size;
        public final int divisions;
        
        public GridData(Vector3f center, Vector3f normal, float size, int divisions) {
            this.center = new Vector3f(center);
            this.normal = new Vector3f(normal);
            this.size = size;
            this.divisions = divisions;
        }
    }
    
    public static class CircleData {
        public final Vector3f center, normal;
        public final float radius;
        public final int segments;
        
        public CircleData(Vector3f center, Vector3f normal, float radius, int segments) {
            this.center = new Vector3f(center);
            this.normal = new Vector3f(normal);
            this.radius = radius;
            this.segments = segments;
        }
    }
}
