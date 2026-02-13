package stellar.snow.astralis.engine.render.meshlet;

import java.nio.ByteBuffer;

/**
 * MeshletBounds - Separate bounds structure for GPU culling passes.
 * 
 * <p>This compact structure is used for hierarchical culling in the task shader,
 * before loading full meshlet data.</p>
 * 
 * <p>Memory Layout (32 bytes):</p>
 * <pre>
 * - vec4 boundingSphere (16 bytes)
 * - vec4 coneCullData   (16 bytes: apex.xyz + cutoff, axis.xy packed)
 * </pre>
 */
public final class MeshletBounds {
    
    public static final int SIZE_BYTES = 32;
    
    // Bounding sphere
    public float centerX, centerY, centerZ, radius;
    
    // Cone culling
    public float coneApexX, coneApexY, coneApexZ, coneCutoff;
    public float coneAxisX, coneAxisY;
    
    // Padding for alignment
    private float pad0, pad1;
    
    public void write(ByteBuffer buffer) {
        buffer.putFloat(centerX).putFloat(centerY).putFloat(centerZ).putFloat(radius);
        buffer.putFloat(coneApexX).putFloat(coneApexY).putFloat(coneApexZ).putFloat(coneCutoff);
    }
    
    public void read(ByteBuffer buffer) {
        centerX = buffer.getFloat();
        centerY = buffer.getFloat();
        centerZ = buffer.getFloat();
        radius = buffer.getFloat();
        coneApexX = buffer.getFloat();
        coneApexY = buffer.getFloat();
        coneApexZ = buffer.getFloat();
        coneCutoff = buffer.getFloat();
    }
    
    public static MeshletBounds fromMeshletData(MeshletData data) {
        MeshletBounds bounds = new MeshletBounds();
        bounds.centerX = data.centerX;
        bounds.centerY = data.centerY;
        bounds.centerZ = data.centerZ;
        bounds.radius = data.radius;
        bounds.coneApexX = data.coneApexX;
        bounds.coneApexY = data.coneApexY;
        bounds.coneApexZ = data.coneApexZ;
        bounds.coneCutoff = data.coneCutoff;
        bounds.coneAxisX = data.coneAxisX;
        bounds.coneAxisY = data.coneAxisY;
        return bounds;
    }
}
