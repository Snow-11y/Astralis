package stellar.snow.astralis.engine.render.shadows;
import org.joml.*;
public final class CascadedShadowMaps {
    private final int cascadeCount = 4;
    private final float[] cascadeSplits = new float[cascadeCount];
    public void calculateSplits(float nearPlane, float farPlane) {
        for (int i = 0; i < cascadeCount; i++) {
            float p = (i + 1) / (float) cascadeCount;
            float log = nearPlane * (float)Math.pow(farPlane / nearPlane, p);
            float uniform = nearPlane + (farPlane - nearPlane) * p;
            cascadeSplits[i] = log * 0.9f + uniform * 0.1f;
        }
    }
    public Matrix4f[] getCascadeMatrices(Matrix4f viewMatrix, Matrix4f projMatrix, Vector3f lightDir) {
        Matrix4f[] matrices = new Matrix4f[cascadeCount];
        for (int i = 0; i < cascadeCount; i++) {
            matrices[i] = new Matrix4f();
        }
        return matrices;
    }
}
