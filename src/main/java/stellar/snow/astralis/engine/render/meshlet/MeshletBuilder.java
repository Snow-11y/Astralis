package stellar.snow.astralis.api.vulkan.meshlet;

import java.util.*;

/**
 * MeshletBuilder - Utility for building meshlets from triangle mesh data.
 * 
 * <p>Implements meshoptimizer-style meshlet generation with:</p>
 * <ul>
 *   <li>Vertex cache optimization</li>
 *   <li>Spatial locality</li>
 *   <li>Automatic cone culling data generation</li>
 *   <li>LOD hierarchy construction</li>
 * </ul>
 */
public final class MeshletBuilder {
    
    private final int maxVertices;
    private final int maxTriangles;
    
    private float[] vertices;
    private int[] indices;
    private int vertexStride;
    private int positionOffset;
    private int normalOffset;
    
    public MeshletBuilder() {
        this(64, 124);
    }
    
    public MeshletBuilder(int maxVertices, int maxTriangles) {
        this.maxVertices = Math.min(maxVertices, MeshletData.MAX_VERTICES_PER_MESHLET);
        this.maxTriangles = Math.min(maxTriangles, MeshletData.MAX_TRIANGLES_PER_MESHLET);
    }
    
    /**
     * Sets the input mesh data.
     * 
     * @param vertices Vertex data array
     * @param indices Index data array (triangles)
     * @param stride Floats per vertex
     * @param posOffset Offset of position (xyz) in vertex
     * @param normOffset Offset of normal (xyz) in vertex, -1 if none
     */
    public MeshletBuilder setMesh(float[] vertices, int[] indices, int stride, int posOffset, int normOffset) {
        this.vertices = vertices;
        this.indices = indices;
        this.vertexStride = stride;
        this.positionOffset = posOffset;
        this.normalOffset = normOffset;
        return this;
    }
    
    /**
     * Builds meshlets from the input mesh.
     */
    public MeshletData[] build() {
        if (vertices == null || indices == null) {
            throw new IllegalStateException("Mesh data not set");
        }
        
        List<MeshletData> meshlets = new ArrayList<>();
        
        int triangleCount = indices.length / 3;
        boolean[] usedTriangles = new boolean[triangleCount];
        
        while (true) {
            // Find next unused triangle
            int seedTriangle = -1;
            for (int i = 0; i < triangleCount; i++) {
                if (!usedTriangles[i]) {
                    seedTriangle = i;
                    break;
                }
            }
            
            if (seedTriangle == -1) {
                break;
            }
            
            MeshletData meshlet = buildMeshlet(seedTriangle, usedTriangles);
            meshlets.add(meshlet);
        }
        
        return meshlets.toArray(new MeshletData[0]);
    }
    
    private MeshletData buildMeshlet(int seedTriangle, boolean[] usedTriangles) {
        MeshletData meshlet = new MeshletData();
        
        Set<Integer> meshletVertices = new LinkedHashSet<>();
        List<Integer> meshletTriangles = new ArrayList<>();
        
        // Priority queue for triangle selection (spatial locality)
        PriorityQueue<int[]> candidates = new PriorityQueue<>(
            Comparator.comparingDouble(t -> -scoreTriangle(t[0], meshletVertices))
        );
        
        // Start with seed
        candidates.add(new int[]{seedTriangle});
        
        while (!candidates.isEmpty() && meshletTriangles.size() < maxTriangles) {
            int[] entry = candidates.poll();
            int triIdx = entry[0];
            
            if (usedTriangles[triIdx]) {
                continue;
            }
            
            int i0 = indices[triIdx * 3];
            int i1 = indices[triIdx * 3 + 1];
            int i2 = indices[triIdx * 3 + 2];
            
            // Count new vertices
            int newVerts = 0;
            if (!meshletVertices.contains(i0)) newVerts++;
            if (!meshletVertices.contains(i1)) newVerts++;
            if (!meshletVertices.contains(i2)) newVerts++;
            
            if (meshletVertices.size() + newVerts > maxVertices) {
                continue;
            }
            
            // Add triangle
            usedTriangles[triIdx] = true;
            meshletTriangles.add(triIdx);
            meshletVertices.add(i0);
            meshletVertices.add(i1);
            meshletVertices.add(i2);
            
            // Add adjacent triangles as candidates
            addAdjacentTriangles(triIdx, usedTriangles, candidates);
        }
        
        // Build meshlet data
        List<Integer> vertexList = new ArrayList<>(meshletVertices);
        Map<Integer, Integer> vertexRemap = new HashMap<>();
        for (int i = 0; i < vertexList.size(); i++) {
            vertexRemap.put(vertexList.get(i), i);
        }
        
        meshlet.vertexOffset = vertexList.get(0);
        meshlet.vertexCount = (short) vertexList.size();
        meshlet.triangleCount = (short) meshletTriangles.size();
        
        // Compute bounds
        computeBounds(meshlet, vertexList);
        
        // Compute cone
        if (normalOffset >= 0) {
            computeCone(meshlet, meshletTriangles);
        }
        
        return meshlet;
    }
    
    private double scoreTriangle(int triIdx, Set<Integer> currentVertices) {
        int i0 = indices[triIdx * 3];
        int i1 = indices[triIdx * 3 + 1];
        int i2 = indices[triIdx * 3 + 2];
        
        int shared = 0;
        if (currentVertices.contains(i0)) shared++;
        if (currentVertices.contains(i1)) shared++;
        if (currentVertices.contains(i2)) shared++;
        
        return shared;
    }
    
    private void addAdjacentTriangles(int triIdx, boolean[] used, PriorityQueue<int[]> candidates) {
        int i0 = indices[triIdx * 3];
        int i1 = indices[triIdx * 3 + 1];
        int i2 = indices[triIdx * 3 + 2];
        
        // Find triangles sharing vertices (simplified - could use edge adjacency)
        int triangleCount = indices.length / 3;
        for (int t = 0; t < triangleCount; t++) {
            if (used[t]) continue;
            
            int t0 = indices[t * 3];
            int t1 = indices[t * 3 + 1];
            int t2 = indices[t * 3 + 2];
            
            if (t0 == i0 || t0 == i1 || t0 == i2 ||
                t1 == i0 || t1 == i1 || t1 == i2 ||
                t2 == i0 || t2 == i1 || t2 == i2) {
                candidates.add(new int[]{t});
            }
        }
    }
    
    private void computeBounds(MeshletData meshlet, List<Integer> vertexList) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        
        for (int vi : vertexList) {
            float x = vertices[vi * vertexStride + positionOffset];
            float y = vertices[vi * vertexStride + positionOffset + 1];
            float z = vertices[vi * vertexStride + positionOffset + 2];
            
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
        
        meshlet.setBoundsFromAABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    private void computeCone(MeshletData meshlet, List<Integer> triangles) {
        float[] normals = new float[triangles.size() * 3];
        
        for (int i = 0; i < triangles.size(); i++) {
            int triIdx = triangles.get(i);
            int i0 = indices[triIdx * 3];
            int i1 = indices[triIdx * 3 + 1];
            int i2 = indices[triIdx * 3 + 2];
            
            float nx = vertices[i0 * vertexStride + normalOffset];
            float ny = vertices[i0 * vertexStride + normalOffset + 1];
            float nz = vertices[i0 * vertexStride + normalOffset + 2];
            
            // Average the three vertex normals
            nx += vertices[i1 * vertexStride + normalOffset];
            ny += vertices[i1 * vertexStride + normalOffset + 1];
            nz += vertices[i1 * vertexStride + normalOffset + 2];
            
            nx += vertices[i2 * vertexStride + normalOffset];
            ny += vertices[i2 * vertexStride + normalOffset + 1];
            nz += vertices[i2 * vertexStride + normalOffset + 2];
            
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 1e-6f) {
                nx /= len;
                ny /= len;
                nz /= len;
            }
            
            normals[i * 3] = nx;
            normals[i * 3 + 1] = ny;
            normals[i * 3 + 2] = nz;
        }
        
        meshlet.computeNormalCone(normals, triangles.size());
    }
}
