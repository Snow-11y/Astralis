package stellar.snow.astralis.engine.render.raytracing;
import org.joml.*;
import java.util.*;
/**
 * Bounding Volume Hierarchy builder for ray tracing acceleration
 * Uses Surface Area Heuristic (SAH) for optimal tree construction
 */
    
    public static class BVHNode {
        Vector3f aabbMin, aabbMax;
        BVHNode left, right;
        int firstPrimitive, primitiveCount;
        
        public BVHNode() {
            aabbMin = new Vector3f(Float.MAX_VALUE);
            aabbMax = new Vector3f(-Float.MAX_VALUE);
        }
        
        public boolean isLeaf() {
            return left == null && right == null;
        }
        
        public float surfaceArea() {
            Vector3f extent = new Vector3f(aabbMax).sub(aabbMin);
            return 2.0f * (extent.x * extent.y + extent.y * extent.z + extent.z * extent.x);
        }
    }
    
    private static class Triangle {
        Vector3f v0, v1, v2;
        Vector3f centroid;
        int index;
        
        public Triangle(Vector3f v0, Vector3f v1, Vector3f v2, int index) {
            this.v0 = v0;
            this.v1 = v1;
            this.v2 = v2;
            this.index = index;
            this.centroid = new Vector3f(v0).add(v1).add(v2).div(3.0f);
        }
        
        public Vector3f getMin() {
            return new Vector3f(
                Math.min(v0.x, Math.min(v1.x, v2.x)),
                Math.min(v0.y, Math.min(v1.y, v2.y)),
                Math.min(v0.z, Math.min(v1.z, v2.z))
            );
        }
        
        public Vector3f getMax() {
            return new Vector3f(
                Math.max(v0.x, Math.max(v1.x, v2.x)),
                Math.max(v0.y, Math.max(v1.y, v2.y)),
                Math.max(v0.z, Math.max(v1.z, v2.z))
            );
        }
    }
    
    private int maxLeafSize = 4;  // Max triangles per leaf
    private float traversalCost = 1.0f;
    private float intersectionCost = 1.2f;
    
    /**
     * Build BVH from vertices and indices
     */
    public BVHNode build(float[] vertices, int[] indices) {
        // Convert to triangles
        List<Triangle> triangles = new ArrayList<>();
        for (int i = 0; i < indices.length; i += 3) {
            Vector3f v0 = getVertex(vertices, indices[i]);
            Vector3f v1 = getVertex(vertices, indices[i + 1]);
            Vector3f v2 = getVertex(vertices, indices[i + 2]);
            triangles.add(new Triangle(v0, v1, v2, i / 3));
        }
        
        return buildRecursive(triangles, 0);
    }
    
    private Vector3f getVertex(float[] vertices, int index) {
        int offset = index * 3;
        return new Vector3f(vertices[offset], vertices[offset + 1], vertices[offset + 2]);
    }
    
    /**
     * Recursive BVH construction using SAH
     */
    private BVHNode buildRecursive(List<Triangle> triangles, int depth) {
        BVHNode node = new BVHNode();
        
        // Calculate bounding box
        for (Triangle tri : triangles) {
            Vector3f min = tri.getMin();
            Vector3f max = tri.getMax();
            node.aabbMin.min(min);
            node.aabbMax.max(max);
        }
        
        // Create leaf if small enough
        if (triangles.size() <= maxLeafSize) {
            node.firstPrimitive = triangles.get(0).index;
            node.primitiveCount = triangles.size();
            return node;
        }
        
        // Find best split using SAH
        SplitResult split = findBestSplit(triangles, node);
        
        if (split == null || split.cost >= triangles.size() * intersectionCost) {
            // No good split found, create leaf
            node.firstPrimitive = triangles.get(0).index;
            node.primitiveCount = triangles.size();
            return node;
        }
        
        // Partition triangles
        List<Triangle> leftTris = new ArrayList<>();
        List<Triangle> rightTris = new ArrayList<>();
        
        for (Triangle tri : triangles) {
            float centroid = getCentroidComponent(tri.centroid, split.axis);
            if (centroid < split.position) {
                leftTris.add(tri);
            } else {
                rightTris.add(tri);
            }
        }
        
        // Handle edge case where all triangles go to one side
        if (leftTris.isEmpty() || rightTris.isEmpty()) {
            node.firstPrimitive = triangles.get(0).index;
            node.primitiveCount = triangles.size();
            return node;
        }
        
        // Recursively build children
        node.left = buildRecursive(leftTris, depth + 1);
        node.right = buildRecursive(rightTris, depth + 1);
        
        return node;
    }
    
    private static class SplitResult {
        int axis;           // 0=X, 1=Y, 2=Z
        float position;     // Split position
        float cost;         // SAH cost
    }
    
    /**
     * Find best split using Surface Area Heuristic
     */
    private SplitResult findBestSplit(List<Triangle> triangles, BVHNode node) {
        SplitResult best = null;
        float bestCost = Float.MAX_VALUE;
        
        Vector3f extent = new Vector3f(node.aabbMax).sub(node.aabbMin);
        
        // Try each axis
        for (int axis = 0; axis < 3; axis++) {
            // Skip if extent is too small on this axis
            if (getComponent(extent, axis) < 0.0001f) continue;
            
            // Try different split positions
            int numBins = 16;
            for (int bin = 1; bin < numBins; bin++) {
                float t = (float)bin / numBins;
                float splitPos = getComponent(node.aabbMin, axis) + 
                               getComponent(extent, axis) * t;
                
                // Evaluate this split
                float cost = evaluateSAH(triangles, axis, splitPos, node);
                
                if (cost < bestCost) {
                    bestCost = cost;
                    best = new SplitResult();
                    best.axis = axis;
                    best.position = splitPos;
                    best.cost = cost;
                }
            }
        }
        
        return best;
    }
    
    /**
     * Evaluate SAH cost for a split
     */
    private float evaluateSAH(List<Triangle> triangles, int axis, float splitPos, BVHNode parent) {
        BVHNode left = new BVHNode();
        BVHNode right = new BVHNode();
        int leftCount = 0, rightCount = 0;
        
        // Calculate bounds for each side
        for (Triangle tri : triangles) {
            float centroid = getCentroidComponent(tri.centroid, axis);
            
            if (centroid < splitPos) {
                left.aabbMin.min(tri.getMin());
                left.aabbMax.max(tri.getMax());
                leftCount++;
            } else {
                right.aabbMin.min(tri.getMin());
                right.aabbMax.max(tri.getMax());
                rightCount++;
            }
        }
        
        // SAH cost = traversalCost + (leftArea/totalArea * leftCount + rightArea/totalArea * rightCount) * intersectionCost
        float parentArea = parent.surfaceArea();
        float leftArea = left.surfaceArea();
        float rightArea = right.surfaceArea();
        
        if (leftCount == 0 || rightCount == 0) {
            return Float.MAX_VALUE;
        }
        
        return traversalCost + intersectionCost * 
               (leftArea / parentArea * leftCount + rightArea / parentArea * rightCount);
    }
    
    private float getComponent(Vector3f v, int axis) {
        return switch (axis) {
            case 0 -> v.x;
            case 1 -> v.y;
            case 2 -> v.z;
            default -> 0;
        };
    }
    
    private float getCentroidComponent(Vector3f centroid, int axis) {
        return getComponent(centroid, axis);
    }
    
    public void setMaxLeafSize(int size) {
        this.maxLeafSize = Math.max(1, size);
    }
    
    public void setCosts(float traversal, float intersection) {
        this.traversalCost = traversal;
        this.intersectionCost = intersection;
    }
}
