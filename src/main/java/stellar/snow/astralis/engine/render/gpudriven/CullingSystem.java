package stellar.snow.astralis.engine.render.gpudriven;
import stellar.snow.astralis.engine.gpu.compute.CullingManager;
import stellar.snow.astralis.engine.gpu.compute.CullingTier;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.joml.*;
/**
 * GPU-driven culling integration wrapper.
 * 
 * This delegates to LO's production-grade CullingManager instead of
 * reimplementing weak culling logic. CullingManager provides:
 * - Temporal smoothing with hysteresis
 * - FMA precision calculations
 * - Intelligent cache eviction
 * - Per-category culling policies
 * 
 * This wrapper adapts the rendering system to use those capabilities.
 */
    
    private final CullingManager cullingManager;
    
    public CullingSystem() {
        this.cullingManager = CullingManager.getInstance();
    }
    
    /**
     * Perform entity culling using LO's CullingManager.
     * Returns entities that should be rendered based on their tier.
     */
    public CullingResult cullEntities(World world, Iterable<Entity> entities) {
        CullingResult result = new CullingResult();
        
        for (Entity entity : entities) {
            CullingTier tier = cullingManager.calculateTier(entity, world);
            CullingManager.CullingPolicy policy = cullingManager.resolvePolicy(entity);
            
            switch (policy) {
                case FULL_RENDER -> result.fullDetail.add(entity);
                case REDUCED_DETAIL -> result.reducedDetail.add(entity);
                case MINIMAL_RENDER -> result.minimal.add(entity);
                case SKIP_RENDER -> result.culled.add(entity);
            }
        }
        
        return result;
    }
    
    /**
     * Set HUD context for culling decisions.
     */
    public void setHudContext(CullingManager.HudContext context) {
        cullingManager.setHudContext(context);
    }
    
    /**
     * Get culling statistics.
     */
    public CullingManager.Statistics getStatistics() {
        return cullingManager.getStatistics();
    }
    
    /**
     * Result of culling operation.
     */
    public static class CullingResult {
        public final java.util.List<Entity> fullDetail = new java.util.ArrayList<>();
        public final java.util.List<Entity> reducedDetail = new java.util.ArrayList<>();
        public final java.util.List<Entity> minimal = new java.util.ArrayList<>();
        public final java.util.List<Entity> culled = new java.util.ArrayList<>();
        
        public int getTotalVisible() {
            return fullDetail.size() + reducedDetail.size() + minimal.size();
        }
        
        public int getTotalCulled() {
            return culled.size();
        }
    }
    
    /**
     * Legacy frustum class for compatibility.
     * Prefer using CullingManager for actual culling.
     */
    @Deprecated
    public static class Frustum {
        Vector4f[] planes = new Vector4f[6];
        
        public Frustum(Matrix4f viewProj) {
            Matrix4f vp = new Matrix4f(viewProj);
            for (int i = 0; i < 6; i++) planes[i] = new Vector4f();
            vp.frustumPlane(0, planes[0]);
            vp.frustumPlane(1, planes[1]);
            vp.frustumPlane(2, planes[2]);
            vp.frustumPlane(3, planes[3]);
            vp.frustumPlane(4, planes[4]);
            vp.frustumPlane(5, planes[5]);
        }
        
        public boolean testSphere(Vector3f center, float radius) {
            for (Vector4f plane : planes) {
                float dist = plane.x * center.x + plane.y * center.y + plane.z * center.z + plane.w;
                if (dist < -radius) return false;
            }
            return true;
        }
    }
}
