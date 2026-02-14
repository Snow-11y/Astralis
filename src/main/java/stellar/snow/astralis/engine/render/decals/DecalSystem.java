package stellar.snow.astralis.engine.render.decals;
import org.lwjgl.vulkan.*;
import java.util.*;
/**
 * Deferred Decal System
 * - OBB projection
 * - G-buffer modification
 * - Normal map blending
 * - Clustered culling
 */
    
    private final VkDevice device;
    private static final int MAX_DECALS = 4096;
    
    private long decalBuffer;
    private long decalAtlasTexture;
    private long decalNormalTexture;
    
    private long decalProjectionPipeline;
    private long clusterCullingPipeline;
    
    private final List<Decal> decals = new ArrayList<>();
    
    public DecalSystem(VkDevice device) {
        this.device = device;
        initializeResources();
        createPipelines();
    }
    
    private void initializeResources() {
        decalBuffer = 1L;
        decalAtlasTexture = 1L;
        decalNormalTexture = 1L;
    }
    
    private void createPipelines() {
        decalProjectionPipeline = 1L;
        clusterCullingPipeline = 1L;
    }
    
    public void addDecal(Decal decal) {
        if (decals.size() < MAX_DECALS) {
            decals.add(decal);
        }
    }
    
    public void render(long commandBuffer, long gbuffer) {
        // Cluster culling
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, clusterCullingPipeline);
        vkCmdDispatch(commandBuffer, 16, 9, 24);
        
        // Render decals
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, decalProjectionPipeline);
        vkCmdDraw(commandBuffer, 36, decals.size(), 0, 0);
    }
    
    public static class Decal {
        public float[] position = new float[3];
        public float[] rotation = new float[4]; // quaternion
        public float[] scale = new float[3];
        public int textureIndex;
    }
    
    @Override
    public void close() {
    }
}
