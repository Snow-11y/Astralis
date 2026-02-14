package stellar.snow.astralis.engine.render.decals;
import org.joml.*;
public final class DecalProjection {
    public static Matrix4f createProjectionMatrix(Vector3f position, Quaternionf rotation, Vector3f scale) {
        Matrix4f mat = new Matrix4f();
        mat.translate(position);
        mat.rotate(rotation);
        mat.scale(scale);
        return mat.invert();
    }
    public static boolean intersectsAABB(Matrix4f projection, Vector3f aabbMin, Vector3f aabbMax) {
        Vector3f[] corners = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = new Vector3f(
                (i & 1) != 0 ? aabbMax.x : aabbMin.x,
                (i & 2) != 0 ? aabbMax.y : aabbMin.y,
                (i & 4) != 0 ? aabbMax.z : aabbMin.z
            );
        }
        for (Vector3f corner : corners) {
            Vector4f projected = new Vector4f(corner, 1.0f);
            projection.transform(projected);
            if (Math.abs(projected.x) <= 1 && Math.abs(projected.y) <= 1 && Math.abs(projected.z) <= 1) {
                return true;
            }
        }
        return false;
    }
}
