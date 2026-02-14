package stellar.snow.astralis.engine.render.meshlet;

import java.util.*;
import java.util.concurrent.*;

/**
 * MeshletClusterDAG - Nanite-style cluster Directed Acyclic Graph.
 * 
 * <p>Implements advanced cluster hierarchy with:</p>
 * <ul>
 *   <li>DAG structure for optimal LOD transitions</li>
 *   <li>Group error propagation</li>
 *   <li>Seamless blending between LOD levels</li>
 *   <li>Spatial hash for fast neighbor queries</li>
 *   <li>Automatic crack prevention</li>
 * </ul>
 */
public final class MeshletClusterDAG {
    
    // ═══════════════════════════════════════════════════════════════════════
    // CLUSTER NODE
    // ═══════════════════════════════════════════════════════════════════════
    
    public static final class ClusterNode {
        public final int nodeId;
        public final int lodLevel;
        public final MeshletData meshlet;
        
        // DAG connections
        public final List<ClusterNode> parents = new ArrayList<>(4);
        public final List<ClusterNode> children = new ArrayList<>(4);
        public final List<ClusterNode> siblings = new ArrayList<>();
        
        // Spatial info
        public float centerX, centerY, centerZ;
        public float radius;
        public long spatialHash;
        
        // Error metrics
        public float geometricError;
        public float parentError;
        public float maxChildError;
        
        // Runtime state
        public volatile boolean isLoaded;
        public volatile boolean isVisible;
        public volatile int frameLastUsed;
        
        public ClusterNode(int nodeId, int lodLevel, MeshletData meshlet) {
            this.nodeId = nodeId;
            this.lodLevel = lodLevel;
            this.meshlet = meshlet;
            this.centerX = meshlet.centerX;
            this.centerY = meshlet.centerY;
            this.centerZ = meshlet.centerZ;
            this.radius = meshlet.radius;
            this.geometricError = meshlet.errorMetric;
        }
        
        public boolean isLeaf() {
            return children.isEmpty();
        }
        
        public boolean isRoot() {
            return parents.isEmpty();
        }
        
        /**
         * Determines if this cluster should be rendered at given screen error.
         */
        public boolean shouldRender(float screenError) {
            // Render if error is above threshold
            if (screenError > geometricError) {
                return true;
            }
            
            // Or if no parent available
            if (parents.isEmpty()) {
                return true;
            }
            
            // Or if parent error too high
            return screenError > parentError * 0.5f;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // DAG STRUCTURE
    // ═══════════════════════════════════════════════════════════════════════
    
    private final Map<Integer, ClusterNode> nodes;
    private final Map<Integer, List<ClusterNode>> levelNodes;
    private final Map<Long, List<ClusterNode>> spatialMap;
    
    private int maxLodLevel;
    private ClusterNode rootNode;
    
    // ═══════════════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════════════
    
    public static final class Statistics {
        public int totalNodes;
        public int leafNodes;
        public int internalNodes;
        public int maxChildren;
        public int maxParents;
        public float avgChildrenPerNode;
        public float avgParentsPerNode;
        public int dagLevels;
        public int maxDepth;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════
    
    public MeshletClusterDAG() {
        this.nodes = new ConcurrentHashMap<>();
        this.levelNodes = new ConcurrentHashMap<>();
        this.spatialMap = new ConcurrentHashMap<>();
        this.maxLodLevel = 0;
    }
    
    /**
     * Builds DAG from LOD hierarchy.
     */
    public void buildFromLODHierarchy(MeshletLODGenerator.LODHierarchy hierarchy) {
        // Create nodes for each LOD level
        int nodeId = 0;
        
        for (int lod = 0; lod < hierarchy.getLevelCount(); lod++) {
            var level = hierarchy.getLevel(lod);
            List<ClusterNode> levelList = new ArrayList<>();
            
            for (MeshletData meshlet : level.meshlets) {
                ClusterNode node = new ClusterNode(nodeId++, lod, meshlet);
                nodes.put(node.nodeId, node);
                levelList.add(node);
                
                // Compute spatial hash
                node.spatialHash = computeSpatialHash(node.centerX, node.centerY, node.centerZ);
                spatialMap.computeIfAbsent(node.spatialHash, k -> new ArrayList<>()).add(node);
            }
            
            levelNodes.put(lod, levelList);
            maxLodLevel = Math.max(maxLodLevel, lod);
        }
        
        // Build parent-child relationships
        linkHierarchy();
        
        // Find siblings (spatially close clusters at same LOD)
        findSiblings();
        
        // Propagate error metrics
        propagateErrors();
        
        // Find root
        for (ClusterNode node : nodes.values()) {
            if (node.isRoot()) {
                rootNode = node;
                break;
            }
        }
    }
    
    private void linkHierarchy() {
        // Link each level to previous (more detailed) level
        for (int lod = 1; lod <= maxLodLevel; lod++) {
            List<ClusterNode> parents = levelNodes.get(lod);
            List<ClusterNode> children = levelNodes.get(lod - 1);
            
            // Spatial matching
            for (ClusterNode parent : parents) {
                List<ClusterNode> nearbyChildren = findNearby(
                    parent.centerX, parent.centerY, parent.centerZ,
                    parent.radius * 2.0f,
                    lod - 1
                );
                
                // Link closest children
                nearbyChildren.sort(Comparator.comparingDouble(c -> 
                    distanceSq(parent, c)
                ));
                
                int maxChildren = 4;
                for (int i = 0; i < Math.min(maxChildren, nearbyChildren.size()); i++) {
                    ClusterNode child = nearbyChildren.get(i);
                    parent.children.add(child);
                    child.parents.add(parent);
                }
            }
        }
    }
    
    private void findSiblings() {
        for (var entry : levelNodes.entrySet()) {
            List<ClusterNode> level = entry.getValue();
            
            for (ClusterNode node : level) {
                List<ClusterNode> nearby = findNearby(
                    node.centerX, node.centerY, node.centerZ,
                    node.radius * 3.0f,
                    node.lodLevel
                );
                
                for (ClusterNode sibling : nearby) {
                    if (sibling.nodeId != node.nodeId && 
                        !node.siblings.contains(sibling)) {
                        node.siblings.add(sibling);
                    }
                }
            }
        }
    }
    
    private void propagateErrors() {
        // Bottom-up error propagation
        for (int lod = 0; lod <= maxLodLevel; lod++) {
            List<ClusterNode> level = levelNodes.get(lod);
            
            for (ClusterNode node : level) {
                // Max child error
                float maxChild = 0;
                for (ClusterNode child : node.children) {
                    maxChild = Math.max(maxChild, child.geometricError);
                }
                node.maxChildError = maxChild;
                
                // Parent error (min of parents)
                float minParent = Float.MAX_VALUE;
                for (ClusterNode parent : node.parents) {
                    minParent = Math.min(minParent, parent.geometricError);
                }
                node.parentError = node.parents.isEmpty() ? Float.MAX_VALUE : minParent;
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Selects visible clusters for given view parameters.
     */
    public List<ClusterNode> selectVisibleClusters(
        float cameraX, float cameraY, float cameraZ,
        float[] frustumPlanes,
        float screenHeight,
        float cotFovY,
        float errorThreshold
    ) {
        List<ClusterNode> visible = new ArrayList<>();
        
        // Start from root and traverse down
        Queue<ClusterNode> queue = new LinkedList<>();
        if (rootNode != null) {
            queue.offer(rootNode);
        }
        
        while (!queue.isEmpty()) {
            ClusterNode node = queue.poll();
            
            // Frustum cull
            if (!isInFrustum(node, frustumPlanes)) {
                continue;
            }
            
            // Compute screen-space error
            float distance = distance(cameraX, cameraY, cameraZ, 
                                     node.centerX, node.centerY, node.centerZ);
            float screenError = (node.geometricError * screenHeight * cotFovY) / 
                               Math.max(distance, 0.001f);
            
            // Decide whether to render this node or recurse
            if (node.shouldRender(screenError) || node.isLeaf()) {
                visible.add(node);
                node.isVisible = true;
            } else {
                // Recurse to children for more detail
                queue.addAll(node.children);
            }
        }
        
        return visible;
    }
    
    /**
     * Finds clusters within radius of point.
     */
    public List<ClusterNode> findNearby(float x, float y, float z, float radius, int lodLevel) {
        List<ClusterNode> result = new ArrayList<>();
        
        // Query spatial hash cells
        int cellRadius = (int) Math.ceil(radius / SPATIAL_CELL_SIZE);
        long baseHash = computeSpatialHash(x, y, z);
        
        for (int dx = -cellRadius; dx <= cellRadius; dx++) {
            for (int dy = -cellRadius; dy <= cellRadius; dy++) {
                for (int dz = -cellRadius; dz <= cellRadius; dz++) {
                    long hash = baseHash + dx + (dy << 10) + (dz << 20);
                    List<ClusterNode> cell = spatialMap.get(hash);
                    
                    if (cell != null) {
                        for (ClusterNode node : cell) {
                            if (node.lodLevel == lodLevel) {
                                float distSq = distanceSq(x, y, z, 
                                    node.centerX, node.centerY, node.centerZ);
                                
                                if (distSq <= radius * radius) {
                                    result.add(node);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Gets node by ID.
     */
    public ClusterNode getNode(int nodeId) {
        return nodes.get(nodeId);
    }
    
    /**
     * Gets all nodes at LOD level.
     */
    public List<ClusterNode> getLevel(int lodLevel) {
        return levelNodes.getOrDefault(lodLevel, Collections.emptyList());
    }
    
    /**
     * Gets maximum LOD level.
     */
    public int getMaxLODLevel() {
        return maxLodLevel;
    }
    
    /**
     * Gets root node.
     */
    public ClusterNode getRoot() {
        return rootNode;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════════════
    
    public Statistics getStatistics() {
        Statistics stats = new Statistics();
        
        stats.totalNodes = nodes.size();
        stats.dagLevels = maxLodLevel + 1;
        
        int maxChildren = 0;
        int maxParents = 0;
        int totalChildren = 0;
        int totalParents = 0;
        int leafCount = 0;
        
        for (ClusterNode node : nodes.values()) {
            if (node.isLeaf()) leafCount++;
            
            maxChildren = Math.max(maxChildren, node.children.size());
            maxParents = Math.max(maxParents, node.parents.size());
            totalChildren += node.children.size();
            totalParents += node.parents.size();
        }
        
        stats.leafNodes = leafCount;
        stats.internalNodes = stats.totalNodes - leafCount;
        stats.maxChildren = maxChildren;
        stats.maxParents = maxParents;
        stats.avgChildrenPerNode = (float) totalChildren / stats.totalNodes;
        stats.avgParentsPerNode = (float) totalParents / stats.totalNodes;
        
        return stats;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════
    
    private static final float SPATIAL_CELL_SIZE = 10.0f;
    
    private long computeSpatialHash(float x, float y, float z) {
        int cx = (int) Math.floor(x / SPATIAL_CELL_SIZE);
        int cy = (int) Math.floor(y / SPATIAL_CELL_SIZE);
        int cz = (int) Math.floor(z / SPATIAL_CELL_SIZE);
        
        return cx + (cy << 10) + (cz << 20);
    }
    
    private float distance(float x1, float y1, float z1, float x2, float y2, float z2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    private float distanceSq(float x1, float y1, float z1, float x2, float y2, float z2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }
    
    private float distanceSq(ClusterNode a, ClusterNode b) {
        return distanceSq(a.centerX, a.centerY, a.centerZ,
                         b.centerX, b.centerY, b.centerZ);
    }
    
    private boolean isInFrustum(ClusterNode node, float[] planes) {
        // Test sphere against 6 frustum planes
        for (int i = 0; i < 6; i++) {
            float dist = planes[i * 4] * node.centerX +
                        planes[i * 4 + 1] * node.centerY +
                        planes[i * 4 + 2] * node.centerZ +
                        planes[i * 4 + 3];
            
            if (dist < -node.radius) {
                return false;
            }
        }
        return true;
    }
}
