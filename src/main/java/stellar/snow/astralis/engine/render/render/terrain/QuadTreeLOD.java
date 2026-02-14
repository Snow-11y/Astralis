package stellar.snow.astralis.engine.render.terrain;
import org.joml.*;
public final class QuadTreeLOD {
    public static class QuadNode {
        Vector2f center;
        float size;
        int lod;
        QuadNode[] children;
    }
    public QuadNode build(Vector3f cameraPos, float maxDistance) {
        // Build quadtree based on distance
        return new QuadNode();
    }
}
