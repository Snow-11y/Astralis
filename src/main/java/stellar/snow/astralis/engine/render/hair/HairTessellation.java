package stellar.snow.astralis.engine.render.hair;
import org.joml.*;
/**
 * Hair strand tessellation using smooth curve interpolation
 * Converts low-poly control points into smooth, high-resolution curves
 */
    
    /**
     * Generate smooth tessellated strand from control points using Catmull-Rom splines
     */
    public static float[] generateTessellatedStrand(float[] controlPoints, int tessellationLevel) {
        if (controlPoints.length < 12) { // Need at least 4 points (3 floats each)
            return controlPoints;
        }
        
        int numControlPoints = controlPoints.length / 3;
        int segments = numControlPoints - 1;
        int outputPoints = segments * tessellationLevel;
        float[] result = new float[outputPoints * 3];
        
        int writeIndex = 0;
        
        // For each segment between control points
        for (int i = 0; i < segments; i++) {
            // Get 4 control points for Catmull-Rom (p0, p1, p2, p3)
            Vector3f p0 = getControlPoint(controlPoints, i - 1, numControlPoints);
            Vector3f p1 = getControlPoint(controlPoints, i, numControlPoints);
            Vector3f p2 = getControlPoint(controlPoints, i + 1, numControlPoints);
            Vector3f p3 = getControlPoint(controlPoints, i + 2, numControlPoints);
            
            // Tessellate this segment
            for (int t = 0; t < tessellationLevel; t++) {
                float u = (float)t / tessellationLevel;
                Vector3f point = catmullRom(p0, p1, p2, p3, u);
                
                result[writeIndex++] = point.x;
                result[writeIndex++] = point.y;
                result[writeIndex++] = point.z;
            }
        }
        
        return result;
    }
    
    /**
     * Get control point with clamping at boundaries
     */
    private static Vector3f getControlPoint(float[] points, int index, int count) {
        // Clamp to valid range
        index = Math.max(0, Math.min(count - 1, index));
        
        int offset = index * 3;
        return new Vector3f(
            points[offset],
            points[offset + 1],
            points[offset + 2]
        );
    }
    
    /**
     * Catmull-Rom spline interpolation
     * Creates smooth curve passing through p1 and p2, using p0 and p3 for tangents
     */
    private static Vector3f catmullRom(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        
        // Catmull-Rom basis functions
        float b0 = -0.5f * t3 + t2 - 0.5f * t;
        float b1 = 1.5f * t3 - 2.5f * t2 + 1.0f;
        float b2 = -1.5f * t3 + 2.0f * t2 + 0.5f * t;
        float b3 = 0.5f * t3 - 0.5f * t2;
        
        Vector3f result = new Vector3f();
        result.add(new Vector3f(p0).mul(b0));
        result.add(new Vector3f(p1).mul(b1));
        result.add(new Vector3f(p2).mul(b2));
        result.add(new Vector3f(p3).mul(b3));
        
        return result;
    }
    
    /**
     * Generate tangent vectors for smooth shading
     */
    public static float[] generateTangents(float[] tessellatedPoints) {
        int numPoints = tessellatedPoints.length / 3;
        float[] tangents = new float[numPoints * 3];
        
        for (int i = 0; i < numPoints; i++) {
            Vector3f tangent;
            
            if (i == 0) {
                // First point: use forward difference
                Vector3f p0 = getPoint(tessellatedPoints, i);
                Vector3f p1 = getPoint(tessellatedPoints, i + 1);
                tangent = new Vector3f(p1).sub(p0).normalize();
            } else if (i == numPoints - 1) {
                // Last point: use backward difference
                Vector3f p0 = getPoint(tessellatedPoints, i - 1);
                Vector3f p1 = getPoint(tessellatedPoints, i);
                tangent = new Vector3f(p1).sub(p0).normalize();
            } else {
                // Middle points: use central difference
                Vector3f p0 = getPoint(tessellatedPoints, i - 1);
                Vector3f p2 = getPoint(tessellatedPoints, i + 1);
                tangent = new Vector3f(p2).sub(p0).normalize();
            }
            
            int offset = i * 3;
            tangents[offset] = tangent.x;
            tangents[offset + 1] = tangent.y;
            tangents[offset + 2] = tangent.z;
        }
        
        return tangents;
    }
    
    private static Vector3f getPoint(float[] points, int index) {
        int offset = index * 3;
        return new Vector3f(
            points[offset],
            points[offset + 1],
            points[offset + 2]
        );
    }
    
    /**
     * Calculate adaptive tessellation level based on distance to camera
     */
    public static int getAdaptiveTessellation(float distanceToCamera, int minLevel, int maxLevel) {
        // LOD based on distance
        float lodFactor = Math.max(0.0f, Math.min(1.0f, (distanceToCamera - 5.0f) / 50.0f));
        int level = (int)((1.0f - lodFactor) * maxLevel + lodFactor * minLevel);
        return Math.max(minLevel, Math.min(maxLevel, level));
    }
}
