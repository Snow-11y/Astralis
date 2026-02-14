package stellar.snow.astralis.engine.render.ocean;

import java.nio.*;

public final class OceanMesh {
    private final OceanRenderingSystem.OceanConfig config;
    
    public OceanMesh(OceanRenderingSystem.OceanConfig config) {
        this.config = config;
    }
    
    public MeshData generate() {
        int gridSize = config.mobileOptimized ? 128 : 256;
        float tileSize = 1000.0f;
        
        ByteBuffer vertices = ByteBuffer.allocateDirect(gridSize * gridSize * 5 * 4);
        IntBuffer indices = IntBuffer.allocate((gridSize - 1) * (gridSize - 1) * 6);
        
        // Generate grid vertices
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                float fx = (float)x / (gridSize - 1);
                float fy = (float)y / (gridSize - 1);
                
                vertices.putFloat((fx - 0.5f) * tileSize);
                vertices.putFloat(0.0f);
                vertices.putFloat((fy - 0.5f) * tileSize);
                vertices.putFloat(fx);
                vertices.putFloat(fy);
            }
        }
        
        // Generate indices
        for (int y = 0; y < gridSize - 1; y++) {
            for (int x = 0; x < gridSize - 1; x++) {
                int i0 = y * gridSize + x;
                int i1 = i0 + 1;
                int i2 = i0 + gridSize;
                int i3 = i2 + 1;
                
                indices.put(i0);
                indices.put(i2);
                indices.put(i1);
                indices.put(i1);
                indices.put(i2);
                indices.put(i3);
            }
        }
        
        vertices.flip();
        indices.flip();
        
        return new MeshData(vertices, indices, indices.remaining());
    }
    
    public static class MeshData {
        public final ByteBuffer vertices;
        public final IntBuffer indices;
        public final int indexCount;
        
        public MeshData(ByteBuffer v, IntBuffer i, int count) {
            this.vertices = v;
            this.indices = i;
            this.indexCount = count;
        }
    }
}
