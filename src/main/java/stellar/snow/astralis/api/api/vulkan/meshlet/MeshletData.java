package stellar.snow.astralis.api.vulkan.meshlet;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * MeshletData - Represents a cluster of geometry optimized for GPU mesh shading.
 * 
 * <p>Memory Layout (std430/scalar packing, 64-byte aligned for optimal cache):</p>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ Offset │ Size │ Field              │ Description                        │
 * ├────────┼──────┼────────────────────┼────────────────────────────────────┤
 * │ 0      │ 12   │ boundingSphere.xyz │ Bounding sphere center             │
 * │ 12     │ 4    │ boundingSphere.w   │ Bounding sphere radius             │
 * │ 16     │ 12   │ coneApex.xyz       │ Normal cone apex position          │
 * │ 28     │ 4    │ coneApex.w         │ Cone cutoff (cos(angle + 90°))     │
 * │ 32     │ 8    │ coneAxis.xy        │ Octahedron-encoded cone axis       │
 * │ 40     │ 4    │ vertexOffset       │ Offset into vertex buffer          │
 * │ 44     │ 4    │ indexOffset        │ Offset into index buffer           │
 * │ 48     │ 1    │ vertexCount        │ Number of vertices (max 64/128)    │
 * │ 49     │ 1    │ triangleCount      │ Number of triangles (max 64/126)   │
 * │ 50     │ 2    │ materialId         │ Material/texture index             │
 * │ 52     │ 2    │ lodLevel           │ LOD level for this meshlet         │
 * │ 54     │ 2    │ flags              │ Visibility/culling flags           │
 * │ 56     │ 4    │ parentOffset       │ Parent meshlet for LOD streaming   │
 * │ 60     │ 4    │ errorMetric        │ LOD error for selection            │
 * └────────┴──────┴────────────────────┴────────────────────────────────────┘
 * Total: 64 bytes (cache line aligned)
 * </pre>
 * 
 * <p>Supports:</p>
 * <ul>
 *   <li>Two-phase occlusion culling with bounding spheres</li>
 *   <li>Cone culling for backface rejection</li>
 *   <li>Hierarchical LOD with error metrics</li>
 *   <li>Nanite-style virtualized geometry concepts</li>
 * </ul>
 */
public final class MeshletData {
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════
    
    public static final int SIZE_BYTES = 64;
    public static final int ALIGNMENT = 64; // Cache line alignment
    
    public static final int MAX_VERTICES_PER_MESHLET = 64;
    public static final int MAX_TRIANGLES_PER_MESHLET = 124; // 126 for some implementations
    public static final int MAX_INDICES_PER_MESHLET = MAX_TRIANGLES_PER_MESHLET * 3;
    
    // ═══════════════════════════════════════════════════════════════════════
    // BOUNDING VOLUME
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Bounding sphere center X */
    public float centerX;
    /** Bounding sphere center Y */
    public float centerY;
    /** Bounding sphere center Z */
    public float centerZ;
    /** Bounding sphere radius */
    public float radius;
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONE CULLING (Backface cluster culling)
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Cone apex X (offset from center for better precision) */
    public float coneApexX;
    /** Cone apex Y */
    public float coneApexY;
    /** Cone apex Z */
    public float coneApexZ;
    /** Cone cutoff: cos(cone_angle + 90°), negative means > 90° spread */
    public float coneCutoff;
    
    /** Cone axis X (octahedron encoded for compactness) */
    public float coneAxisX;
    /** Cone axis Y (octahedron encoded) */
    public float coneAxisY;
    
    // ═══════════════════════════════════════════════════════════════════════
    // GEOMETRY REFERENCES
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Offset into the global vertex buffer */
    public int vertexOffset;
    /** Offset into the meshlet's local index buffer */
    public int indexOffset;
    /** Number of unique vertices in this meshlet (max 64 or 128) */
    public short vertexCount;
    /** Number of triangles in this meshlet (max 124 or 126) */
    public short triangleCount;
    
    // ═══════════════════════════════════════════════════════════════════════
    // MATERIAL & LOD
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Material/texture array index for bindless rendering */
    public short materialId;
    /** LOD level (0 = highest detail) */
    public short lodLevel;
    /** Visibility and culling flags */
    public short flags;
    /** Reserved for alignment */
    private short reserved;
    
    /** Parent meshlet offset for LOD hierarchy (-1 if root) */
    public int parentOffset;
    /** Screen-space error metric for LOD selection */
    public float errorMetric;
    
    // ═══════════════════════════════════════════════════════════════════════
    // FLAG DEFINITIONS
    // ═══════════════════════════════════════════════════════════════════════
    
    public static final class Flags {
        public static final short NONE = 0;
        public static final short VISIBLE = 1;
        public static final short SHADOW_CASTER = 1 << 1;
        public static final short TWO_SIDED = 1 << 2;
        public static final short ALPHA_TESTED = 1 << 3;
        public static final short TRANSPARENT = 1 << 4;
        public static final short STATIC = 1 << 5;
        public static final short DYNAMIC = 1 << 6;
        public static final short OCCLUDER = 1 << 7;
        public static final short LEAF_NODE = 1 << 8;
        public static final short HAS_CHILDREN = 1 << 9;
        public static final short FORCE_RENDER = 1 << 10;
        public static final short SKIP_CONE_CULL = 1 << 11;
        
        private Flags() {}
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTORS
    // ═══════════════════════════════════════════════════════════════════════
    
    public MeshletData() {
        this.coneCutoff = 1.0f; // No cone culling by default
        this.parentOffset = -1;
        this.flags = Flags.VISIBLE;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // SERIALIZATION
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Writes this meshlet to a ByteBuffer at the current position.
     * Buffer must have at least SIZE_BYTES remaining.
     */
    public void write(ByteBuffer buffer) {
        // Bounding sphere (16 bytes)
        buffer.putFloat(centerX);
        buffer.putFloat(centerY);
        buffer.putFloat(centerZ);
        buffer.putFloat(radius);
        
        // Cone culling (16 bytes)
        buffer.putFloat(coneApexX);
        buffer.putFloat(coneApexY);
        buffer.putFloat(coneApexZ);
        buffer.putFloat(coneCutoff);
        
        // Cone axis (8 bytes)
        buffer.putFloat(coneAxisX);
        buffer.putFloat(coneAxisY);
        
        // Geometry references (8 bytes)
        buffer.putInt(vertexOffset);
        buffer.putInt(indexOffset);
        
        // Counts (4 bytes)
        buffer.putShort(vertexCount);
        buffer.putShort(triangleCount);
        
        // Material & LOD (8 bytes)
        buffer.putShort(materialId);
        buffer.putShort(lodLevel);
        buffer.putShort(flags);
        buffer.putShort(reserved);
        
        // Hierarchy (8 bytes)
        buffer.putInt(parentOffset);
        buffer.putFloat(errorMetric);
    }
    
    /**
     * Reads meshlet data from a ByteBuffer at the current position.
     */
    public void read(ByteBuffer buffer) {
        centerX = buffer.getFloat();
        centerY = buffer.getFloat();
        centerZ = buffer.getFloat();
        radius = buffer.getFloat();
        
        coneApexX = buffer.getFloat();
        coneApexY = buffer.getFloat();
        coneApexZ = buffer.getFloat();
        coneCutoff = buffer.getFloat();
        
        coneAxisX = buffer.getFloat();
        coneAxisY = buffer.getFloat();
        
        vertexOffset = buffer.getInt();
        indexOffset = buffer.getInt();
        
        vertexCount = buffer.getShort();
        triangleCount = buffer.getShort();
        
        materialId = buffer.getShort();
        lodLevel = buffer.getShort();
        flags = buffer.getShort();
        reserved = buffer.getShort();
        
        parentOffset = buffer.getInt();
        errorMetric = buffer.getFloat();
    }
    
    /**
     * Writes to a specific offset in a ByteBuffer.
     */
    public void writeAt(ByteBuffer buffer, int offset) {
        int oldPos = buffer.position();
        buffer.position(offset);
        write(buffer);
        buffer.position(oldPos);
    }
    
    /**
     * Reads from a specific offset in a ByteBuffer.
     */
    public void readAt(ByteBuffer buffer, int offset) {
        int oldPos = buffer.position();
        buffer.position(offset);
        read(buffer);
        buffer.position(oldPos);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // BUILDERS & HELPERS
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Sets the bounding sphere from min/max AABB.
     */
    public MeshletData setBoundsFromAABB(float minX, float minY, float minZ,
                                          float maxX, float maxY, float maxZ) {
        centerX = (minX + maxX) * 0.5f;
        centerY = (minY + maxY) * 0.5f;
        centerZ = (minZ + maxZ) * 0.5f;
        
        float dx = maxX - centerX;
        float dy = maxY - centerY;
        float dz = maxZ - centerZ;
        radius = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        return this;
    }
    
    /**
     * Sets the bounding sphere directly.
     */
    public MeshletData setBounds(float cx, float cy, float cz, float r) {
        this.centerX = cx;
        this.centerY = cy;
        this.centerZ = cz;
        this.radius = r;
        return this;
    }
    
    /**
     * Computes and sets the normal cone from triangle normals.
     * 
     * @param normals Array of normal vectors [nx0, ny0, nz0, nx1, ny1, nz1, ...]
     * @param normalCount Number of normals
     */
    public MeshletData computeNormalCone(float[] normals, int normalCount) {
        if (normalCount == 0) {
            coneAxisX = 0;
            coneAxisY = 1;
            coneCutoff = 1.0f; // Disable culling
            return this;
        }
        
        // Compute average normal
        float avgX = 0, avgY = 0, avgZ = 0;
        for (int i = 0; i < normalCount; i++) {
            avgX += normals[i * 3];
            avgY += normals[i * 3 + 1];
            avgZ += normals[i * 3 + 2];
        }
        
        float invCount = 1.0f / normalCount;
        avgX *= invCount;
        avgY *= invCount;
        avgZ *= invCount;
        
        // Normalize
        float len = (float) Math.sqrt(avgX * avgX + avgY * avgY + avgZ * avgZ);
        if (len < 1e-6f) {
            coneCutoff = 1.0f;
            return this;
        }
        
        float invLen = 1.0f / len;
        avgX *= invLen;
        avgY *= invLen;
        avgZ *= invLen;
        
        // Encode to octahedron
        float[] encoded = encodeOctahedron(avgX, avgY, avgZ);
        coneAxisX = encoded[0];
        coneAxisY = encoded[1];
        
        // Find minimum dot product (maximum deviation angle)
        float minDot = 1.0f;
        for (int i = 0; i < normalCount; i++) {
            float dot = avgX * normals[i * 3] + avgY * normals[i * 3 + 1] + avgZ * normals[i * 3 + 2];
            minDot = Math.min(minDot, dot);
        }
        
        // Cone cutoff is cos(angle + 90°) = -sin(angle)
        // angle = acos(minDot)
        float angle = (float) Math.acos(Math.max(-1, Math.min(1, minDot)));
        coneCutoff = (float) -Math.sin(angle);
        
        // Compute apex offset
        if (coneCutoff < 0) {
            // Wide cone, apex is behind center
            float apexOffset = radius / (float) Math.tan(angle);
            coneApexX = centerX - avgX * apexOffset;
            coneApexY = centerY - avgY * apexOffset;
            coneApexZ = centerZ - avgZ * apexOffset;
        } else {
            coneApexX = centerX;
            coneApexY = centerY;
            coneApexZ = centerZ;
        }
        
        return this;
    }
    
    /**
     * Sets geometry references.
     */
    public MeshletData setGeometry(int vertOffset, int idxOffset, int vCount, int tCount) {
        this.vertexOffset = vertOffset;
        this.indexOffset = idxOffset;
        this.vertexCount = (short) Math.min(vCount, MAX_VERTICES_PER_MESHLET);
        this.triangleCount = (short) Math.min(tCount, MAX_TRIANGLES_PER_MESHLET);
        return this;
    }
    
    /**
     * Sets material and LOD information.
     */
    public MeshletData setMaterial(int matId, int lod, float error) {
        this.materialId = (short) matId;
        this.lodLevel = (short) lod;
        this.errorMetric = error;
        return this;
    }
    
    /**
     * Sets flags.
     */
    public MeshletData setFlags(short flags) {
        this.flags = flags;
        return this;
    }
    
    public MeshletData addFlag(short flag) {
        this.flags |= flag;
        return this;
    }
    
    public MeshletData removeFlag(short flag) {
        this.flags &= ~flag;
        return this;
    }
    
    public boolean hasFlag(short flag) {
        return (this.flags & flag) != 0;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CULLING UTILITIES
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Tests if this meshlet is potentially visible from a camera position.
     * Uses cone culling - returns false if all triangles are definitely back-facing.
     */
    public boolean isConePotentiallyVisible(float eyeX, float eyeY, float eyeZ) {
        if (coneCutoff >= 1.0f || hasFlag(Flags.SKIP_CONE_CULL) || hasFlag(Flags.TWO_SIDED)) {
            return true;
        }
        
        // Decode cone axis
        float[] axis = decodeOctahedron(coneAxisX, coneAxisY);
        
        // Vector from apex to eye
        float dx = eyeX - coneApexX;
        float dy = eyeY - coneApexY;
        float dz = eyeZ - coneApexZ;
        
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) {
            return true;
        }
        
        float invLen = 1.0f / len;
        dx *= invLen;
        dy *= invLen;
        dz *= invLen;
        
        // Dot product with cone axis
        float dot = dx * axis[0] + dy * axis[1] + dz * axis[2];
        
        return dot >= coneCutoff;
    }
    
    /**
     * Tests if this meshlet's bounding sphere intersects a frustum plane.
     * 
     * @param planeA Plane equation coefficient A
     * @param planeB Plane equation coefficient B
     * @param planeC Plane equation coefficient C
     * @param planeD Plane equation coefficient D
     * @return true if sphere is on positive side or intersecting
     */
    public boolean isSphereInFrustumPlane(float planeA, float planeB, float planeC, float planeD) {
        float dist = planeA * centerX + planeB * centerY + planeC * centerZ + planeD;
        return dist >= -radius;
    }
    
    /**
     * Computes the screen-space projected radius for LOD selection.
     */
    public float computeProjectedRadius(float distanceToCamera, float cotFovY, float screenHeight) {
        if (distanceToCamera <= 0) {
            return Float.MAX_VALUE;
        }
        return (radius * cotFovY * screenHeight) / distanceToCamera;
    }
    
    /**
     * Determines if this meshlet should be rendered based on LOD error.
     */
    public boolean shouldRenderAtDistance(float distance, float errorThreshold) {
        // Project error to screen space
        float projectedError = errorMetric / Math.max(distance, 0.001f);
        return projectedError > errorThreshold;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // OCTAHEDRON ENCODING
    // ═══════════════════════════════════════════════════════════════════════
    
    private static float[] encodeOctahedron(float x, float y, float z) {
        float invL1Norm = 1.0f / (Math.abs(x) + Math.abs(y) + Math.abs(z));
        float ox = x * invL1Norm;
        float oy = y * invL1Norm;
        
        if (z < 0) {
            float tempX = (1.0f - Math.abs(oy)) * (ox >= 0 ? 1 : -1);
            float tempY = (1.0f - Math.abs(ox)) * (oy >= 0 ? 1 : -1);
            ox = tempX;
            oy = tempY;
        }
        
        return new float[]{ox, oy};
    }
    
    private static float[] decodeOctahedron(float ox, float oy) {
        float z = 1.0f - Math.abs(ox) - Math.abs(oy);
        float x, y;
        
        if (z < 0) {
            x = (1.0f - Math.abs(oy)) * (ox >= 0 ? 1 : -1);
            y = (1.0f - Math.abs(ox)) * (oy >= 0 ? 1 : -1);
        } else {
            x = ox;
            y = oy;
        }
        
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        float invLen = 1.0f / len;
        return new float[]{x * invLen, y * invLen, z * invLen};
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // OBJECT METHODS
    // ═══════════════════════════════════════════════════════════════════════
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeshletData that)) return false;
        return vertexOffset == that.vertexOffset 
            && indexOffset == that.indexOffset
            && vertexCount == that.vertexCount
            && triangleCount == that.triangleCount;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(vertexOffset, indexOffset, vertexCount, triangleCount);
    }
    
    @Override
    public String toString() {
        return "MeshletData{center=(%.2f,%.2f,%.2f), r=%.2f, verts=%d, tris=%d, mat=%d, lod=%d}"
            .formatted(centerX, centerY, centerZ, radius, vertexCount, triangleCount, materialId, lodLevel);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // STATIC UTILITIES
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Allocates a properly aligned ByteBuffer for meshlet storage.
     */
    public static ByteBuffer allocateBuffer(int meshletCount) {
        int size = meshletCount * SIZE_BYTES;
        // Ensure 64-byte alignment
        size = ((size + ALIGNMENT - 1) / ALIGNMENT) * ALIGNMENT;
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }
    
    /**
     * Creates an array of meshlet data from a ByteBuffer.
     */
    public static MeshletData[] readAll(ByteBuffer buffer, int count) {
        MeshletData[] result = new MeshletData[count];
        buffer.rewind();
        for (int i = 0; i < count; i++) {
            result[i] = new MeshletData();
            result[i].read(buffer);
        }
        return result;
    }
    
    /**
     * Writes an array of meshlets to a ByteBuffer.
     */
    public static void writeAll(ByteBuffer buffer, MeshletData[] meshlets) {
        buffer.rewind();
        for (MeshletData meshlet : meshlets) {
            meshlet.write(buffer);
        }
        buffer.flip();
    }
}
