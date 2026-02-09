#version 460
#extension GL_EXT_mesh_shader : require
#extension GL_KHR_shader_subgroup_ballot : require
#extension GL_KHR_shader_subgroup_arithmetic : require
#extension GL_KHR_shader_subgroup_vote : require
#extension GL_KHR_shader_subgroup_shuffle : require
#extension GL_EXT_shader_explicit_arithmetic_types : require
#extension GL_EXT_shader_16bit_storage : require
#extension GL_EXT_shader_8bit_storage : require
#extension GL_EXT_control_flow_attributes : require

// ═══════════════════════════════════════════════════════════════════════════
// CONFIGURATION
// ═══════════════════════════════════════════════════════════════════════════

// Task shader workgroup size - process multiple meshlets per workgroup
layout(local_size_x = 32) in;

// Maximum meshlets that can be dispatched per task shader workgroup
#define MAX_MESHLETS_PER_TASK 32

// Culling feature flags (compile-time)
#ifndef ENABLE_FRUSTUM_CULLING
#define ENABLE_FRUSTUM_CULLING 1
#endif

#ifndef ENABLE_CONE_CULLING
#define ENABLE_CONE_CULLING 1
#endif

#ifndef ENABLE_HIZ_CULLING
#define ENABLE_HIZ_CULLING 1
#endif

#ifndef ENABLE_LOD
#define ENABLE_LOD 1
#endif

#ifndef ENABLE_INSTANCING
#define ENABLE_INSTANCING 1
#endif

#ifndef DEBUG_MODE
#define DEBUG_MODE 0
#endif

// ═══════════════════════════════════════════════════════════════════════════
// CONSTANTS
// ═══════════════════════════════════════════════════════════════════════════

const uint MESHLET_FLAG_DOUBLE_SIDED = 1u << 0;
const uint MESHLET_FLAG_ALPHA_TEST   = 1u << 1;
const uint MESHLET_FLAG_TRANSPARENT  = 1u << 2;

const uint INSTANCE_FLAG_ENABLED     = 1u << 0;
const uint INSTANCE_FLAG_CAST_SHADOW = 1u << 1;
const uint INSTANCE_FLAG_STATIC      = 1u << 2;

// ═══════════════════════════════════════════════════════════════════════════
// STRUCTURES
// ═══════════════════════════════════════════════════════════════════════════

// Meshlet descriptor - 64 bytes, cache-line aligned
struct MeshletDesc {
    // Bounding sphere (16 bytes)
    vec3 boundingSphereCenter;    // Local space
    float boundingSphereRadius;
    
    // Bounding cone for backface culling (16 bytes)
    // Apex is at boundingSphereCenter + coneApex
    vec3 coneAxis;                // Normalized direction
    float coneCutoff;             // cos(angle + 90°), negative = valid cone
    
    // Bounding box for tighter culling (24 bytes)
    vec3 bboxMin;
    vec3 bboxMax;
    
    // Offsets and counts (8 bytes)
    uint vertexOffset;            // Into global vertex buffer
    uint triangleOffset;          // Into meshlet triangle buffer
    uint8_t vertexCount;          // Max 255 vertices
    uint8_t triangleCount;        // Max 255 triangles
    uint8_t lodLevel;             // LOD this meshlet belongs to
    uint8_t flags;
    
    // Hierarchy info (8 bytes)
    uint parentClusterIndex;      // For hierarchical culling
    uint materialIndex;
};

// Meshlet LOD group - describes LOD levels for a mesh
struct MeshLODGroup {
    uint meshletOffsets[8];       // Start index for each LOD level
    uint meshletCounts[8];        // Number of meshlets per LOD
    vec4 lodDistances;            // Distance thresholds (4 LODs)
    vec4 lodErrors;               // Screen-space error thresholds
    uint lodCount;
    uint totalMeshlets;
    uint flags;
    uint _pad;
};

// Per-instance data
struct InstanceData {
    mat4 modelMatrix;
    mat4 prevModelMatrix;         // For motion vectors
    vec4 boundingSphere;          // World-space instance bounds
    uint meshLODGroupIndex;       // Which mesh/LOD group
    uint flags;
    uint customData;
    float lodBias;                // Per-instance LOD bias
};

// Cluster node for hierarchical culling
struct ClusterNode {
    vec4 boundingSphere;          // xyz = center, w = radius
    uint childOffset;             // First child or meshlet index
    uint childCount;              // Number of children (0 = leaf)
    uint meshletStart;            // For leaves: first meshlet
    uint meshletCount;            // For leaves: meshlet count
};

// ═══════════════════════════════════════════════════════════════════════════
// TASK PAYLOAD - Passed to Mesh Shader
// ═══════════════════════════════════════════════════════════════════════════

struct MeshletTask {
    uint meshletIndex;            // Global meshlet index
    uint instanceIndex;           // Which instance
    uint lodLevel;                // Selected LOD
    uint materialIndex;           // For sorting/batching
};

struct TaskPayload {
    MeshletTask tasks[MAX_MESHLETS_PER_TASK];
    uint taskCount;
    
    // Shared data for mesh shader
    mat4 viewProj;
    vec3 cameraPosition;
    uint frameIndex;
};

taskPayloadSharedEXT TaskPayload payload;

// ═══════════════════════════════════════════════════════════════════════════
// BINDINGS
// ═══════════════════════════════════════════════════════════════════════════

// Meshlet descriptors
layout(std430, binding = 0) readonly buffer MeshletBuffer {
    MeshletDesc meshlets[];
};

// LOD groups per mesh type
layout(std430, binding = 1) readonly buffer LODGroupBuffer {
    MeshLODGroup lodGroups[];
};

// Instance data
layout(std430, binding = 2) readonly buffer InstanceBuffer {
    InstanceData instances[];
};

// Cluster hierarchy for hierarchical culling
layout(std430, binding = 3) readonly buffer ClusterBuffer {
    ClusterNode clusters[];
};

// Visible instance list (from compute pre-pass or CPU)
layout(std430, binding = 4) readonly buffer VisibleInstanceBuffer {
    uint visibleInstanceCount;
    uint visibleInstanceIDs[];
};

// Hierarchical Z-buffer for occlusion culling
layout(binding = 5) uniform sampler2D hiZBuffer;

// Culling statistics (debug)
layout(std430, binding = 6) buffer StatsBuffer {
    uint totalMeshletsProcessed;
    uint frustumCulled;
    uint coneCulled;
    uint occlusionCulled;
    uint lodSkipped;
    uint totalMeshletsDrawn;
};

// Uniforms
layout(std140, binding = 7) uniform CullingUniforms {
    mat4 view;
    mat4 proj;
    mat4 viewProj;
    mat4 prevViewProj;
    
    vec4 frustumPlanes[6];
    
    vec4 cameraPosition;
    vec4 cameraForward;
    
    vec2 viewportSize;
    float nearPlane;
    float farPlane;
    
    float lodBias;
    float hiZBias;
    uint cullingFlags;
    uint frameIndex;
    
    // Dispatch info
    uint meshletGroupOffset;      // For large meshlet counts
    uint meshletGroupCount;
    uint instanceOffset;
    uint instanceCount;
} ubo;

// Push constants for per-draw data
layout(push_constant) uniform PushConstants {
    uint batchOffset;
    uint batchCount;
    uint passFlags;              // Shadow pass, etc.
    uint debugFlags;
};

// ═══════════════════════════════════════════════════════════════════════════
// SHARED MEMORY
// ═══════════════════════════════════════════════════════════════════════════

// Cache frequently accessed data
shared vec4 s_frustumPlanes[6];
shared vec3 s_cameraPos;
shared mat4 s_viewProj;
shared uint s_visibleCount;

// ═══════════════════════════════════════════════════════════════════════════
// CULLING FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

// Fast frustum-sphere test
bool frustumCullSphere(vec3 center, float radius) {
    [[unroll]]
    for (int i = 0; i < 6; i++) {
        float dist = dot(s_frustumPlanes[i].xyz, center) + s_frustumPlanes[i].w;
        if (dist < -radius) {
            return false; // Outside this plane
        }
    }
    return true;
}

// Tighter frustum-AABB test
bool frustumCullAABB(vec3 bboxMin, vec3 bboxMax) {
    [[unroll]]
    for (int i = 0; i < 6; i++) {
        vec4 plane = s_frustumPlanes[i];
        
        // Find the corner most aligned with plane normal
        vec3 pVertex = mix(bboxMin, bboxMax, greaterThan(plane.xyz, vec3(0.0)));
        
        if (dot(plane.xyz, pVertex) + plane.w < 0.0) {
            return false;
        }
    }
    return true;
}

// Cone culling - cull meshlets facing away from camera
// This is a powerful optimization for closed surfaces
bool coneCullMeshlet(vec3 meshletCenter, vec3 coneAxis, float coneCutoff, vec3 cameraPos) {
    // If coneCutoff >= 1.0, cone culling is disabled for this meshlet
    if (coneCutoff >= 1.0) {
        return true; // Don't cull
    }
    
    // Vector from meshlet to camera
    vec3 toCamera = normalize(cameraPos - meshletCenter);
    
    // If camera is "behind" the cone (all triangles face away), cull
    // The cone axis points in the average normal direction
    // coneCutoff = cos(cone_half_angle + 90°) = -sin(cone_half_angle)
    
    float dotProduct = dot(toCamera, coneAxis);
    
    // Visible if camera is within the cone spread
    return dotProduct > coneCutoff;
}

// Hierarchical Z-buffer occlusion test
bool hiZOcclusionCull(vec3 center, float radius, mat4 mvp) {
    // Project bounding sphere to screen
    vec4 clipCenter = mvp * vec4(center, 1.0);
    
    // Behind camera
    if (clipCenter.w <= 0.0) {
        return false;
    }
    
    vec3 ndc = clipCenter.xyz / clipCenter.w;
    
    // Calculate projected radius
    float projRadius = (ubo.proj[1][1] * radius) / clipCenter.w;
    
    // Screen-space bounds
    vec2 screenCenter = ndc.xy * 0.5 + 0.5;
    vec2 minUV = clamp(screenCenter - projRadius, vec2(0.0), vec2(1.0));
    vec2 maxUV = clamp(screenCenter + projRadius, vec2(0.0), vec2(1.0));
    
    // Select mip level based on screen size
    vec2 screenSize = (maxUV - minUV) * ubo.viewportSize;
    float mipLevel = ceil(log2(max(screenSize.x, screenSize.y)));
    mipLevel = clamp(mipLevel, 0.0, float(textureQueryLevels(hiZBuffer) - 1));
    
    // Sample HiZ at multiple points
    float maxHiZDepth = 0.0;
    vec2 samplePoints[4] = vec2[4](minUV, maxUV, vec2(minUV.x, maxUV.y), vec2(maxUV.x, minUV.y));
    
    [[unroll]]
    for (int i = 0; i < 4; i++) {
        maxHiZDepth = max(maxHiZDepth, textureLod(hiZBuffer, samplePoints[i], mipLevel).r);
    }
    
    // Calculate closest depth of sphere
    float closestDepth = (ndc.z * 0.5 + 0.5) - projRadius * 0.5;
    closestDepth -= ubo.hiZBias;
    
    return closestDepth <= maxHiZDepth;
}

// Screen-space error based LOD selection
uint selectLOD(vec3 worldCenter, float worldRadius, MeshLODGroup lodGroup, float lodBias) {
    float distance = length(worldCenter - s_cameraPos);
    
    // Calculate screen-space size in pixels
    float screenSize = (ubo.proj[1][1] * worldRadius * 2.0 * ubo.viewportSize.y) / distance;
    
    // Apply LOD bias
    screenSize *= lodBias * ubo.lodBias;
    
    // Select LOD based on screen-space error thresholds
    for (uint lod = 0; lod < lodGroup.lodCount - 1; lod++) {
        if (screenSize >= lodGroup.lodErrors[lod]) {
            return lod;
        }
    }
    
    return lodGroup.lodCount - 1;
}

// Transform bounding data to world space
void transformBounds(mat4 modelMatrix, vec3 localCenter, float localRadius,
                     out vec3 worldCenter, out float worldRadius) {
    worldCenter = (modelMatrix * vec4(localCenter, 1.0)).xyz;
    
    // Conservative radius scaling
    float scaleX = length(modelMatrix[0].xyz);
    float scaleY = length(modelMatrix[1].xyz);
    float scaleZ = length(modelMatrix[2].xyz);
    worldRadius = localRadius * max(max(scaleX, scaleY), scaleZ);
}

// ═══════════════════════════════════════════════════════════════════════════
// HIERARCHICAL CULLING (Optional two-phase approach)
// ═══════════════════════════════════════════════════════════════════════════

// For very large scenes, first cull cluster hierarchy, then individual meshlets
bool cullClusterNode(ClusterNode node, mat4 modelMatrix) {
    vec3 worldCenter;
    float worldRadius;
    transformBounds(modelMatrix, node.boundingSphere.xyz, node.boundingSphere.w,
                    worldCenter, worldRadius);
    
    #if ENABLE_FRUSTUM_CULLING
        if (!frustumCullSphere(worldCenter, worldRadius)) {
            return false;
        }
    #endif
    
    #if ENABLE_HIZ_CULLING
        mat4 mvp = s_viewProj * modelMatrix;
        if (!hiZOcclusionCull(worldCenter, worldRadius, mvp)) {
            return false;
        }
    #endif
    
    return true;
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN TASK SHADER
// ═══════════════════════════════════════════════════════════════════════════

void main() {
    uint lID = gl_LocalInvocationID.x;
    uint gID = gl_GlobalInvocationID.x;
    uint wgID = gl_WorkGroupID.x;
    
    // ─────────────────────────────────────────────────────────────────────
    // PHASE 1: Initialize shared memory (first few threads)
    // ─────────────────────────────────────────────────────────────────────
    
    if (lID < 6) {
        s_frustumPlanes[lID] = ubo.frustumPlanes[lID];
    }
    if (lID == 0) {
        s_cameraPos = ubo.cameraPosition.xyz;
        s_viewProj = ubo.viewProj;
        s_visibleCount = 0;
    }
    
    barrier();
    
    // ─────────────────────────────────────────────────────────────────────
    // PHASE 2: Determine what this thread processes
    // ─────────────────────────────────────────────────────────────────────
    
    // Each workgroup processes one instance's meshlets
    // Or processes a batch of meshlets across instances
    
    #if ENABLE_INSTANCING
        // Mode 1: One workgroup per instance, threads process meshlets
        uint instanceIdx = wgID + ubo.instanceOffset;
        bool validInstance = instanceIdx < ubo.instanceCount;
        
        InstanceData instance;
        MeshLODGroup lodGroup;
        uint selectedLOD = 0;
        uint meshletBaseIndex = 0;
        uint meshletCount = 0;
        
        if (validInstance) {
            // Fetch instance (could also use visibility buffer indirection)
            instance = instances[instanceIdx];
            validInstance = (instance.flags & INSTANCE_FLAG_ENABLED) != 0u;
            
            if (validInstance) {
                lodGroup = lodGroups[instance.meshLODGroupIndex];
                
                // Instance-level frustum cull
                vec3 worldCenter = instance.boundingSphere.xyz;
                float worldRadius = instance.boundingSphere.w;
                
                #if ENABLE_FRUSTUM_CULLING
                    validInstance = frustumCullSphere(worldCenter, worldRadius);
                #endif
                
                #if ENABLE_LOD
                    if (validInstance) {
                        selectedLOD = selectLOD(worldCenter, worldRadius, lodGroup, instance.lodBias);
                    }
                #endif
                
                if (validInstance) {
                    meshletBaseIndex = lodGroup.meshletOffsets[selectedLOD];
                    meshletCount = lodGroup.meshletCounts[selectedLOD];
                }
            }
        }
    #else
        // Mode 2: Process meshlets directly (pre-culled instances)
        uint meshletGlobalIndex = gID + ubo.meshletGroupOffset;
        bool validMeshlet = meshletGlobalIndex < ubo.meshletGroupCount;
        uint instanceIdx = 0; // Single instance or provided differently
        InstanceData instance;
        uint selectedLOD = 0;
        
        if (validMeshlet) {
            instance = instances[instanceIdx];
        }
    #endif
    
    // ─────────────────────────────────────────────────────────────────────
    // PHASE 3: Per-meshlet culling
    // ─────────────────────────────────────────────────────────────────────
    
    bool visible = false;
    MeshletDesc meshlet;
    uint meshletIndex = 0;
    
    #if ENABLE_INSTANCING
        // Each thread in the workgroup processes different meshlets of the same instance
        uint localMeshletIdx = lID;
        
        if (validInstance && localMeshletIdx < meshletCount) {
            meshletIndex = meshletBaseIndex + localMeshletIdx;
            meshlet = meshlets[meshletIndex];
            
            // Transform meshlet bounds to world space
            vec3 worldCenter;
            float worldRadius;
            transformBounds(instance.modelMatrix, 
                           meshlet.boundingSphereCenter, 
                           meshlet.boundingSphereRadius,
                           worldCenter, worldRadius);
            
            visible = true;
            
            // Frustum culling
            #if ENABLE_FRUSTUM_CULLING
                if (visible) {
                    // Use AABB for tighter culling if available
                    vec3 worldBBoxMin = (instance.modelMatrix * vec4(meshlet.bboxMin, 1.0)).xyz;
                    vec3 worldBBoxMax = (instance.modelMatrix * vec4(meshlet.bboxMax, 1.0)).xyz;
                    
                    // Ensure min/max after transform (non-uniform scale can flip)
                    vec3 actualMin = min(worldBBoxMin, worldBBoxMax);
                    vec3 actualMax = max(worldBBoxMin, worldBBoxMax);
                    
                    visible = frustumCullAABB(actualMin, actualMax);
                    
                    #if DEBUG_MODE
                        if (!visible) atomicAdd(frustumCulled, 1);
                    #endif
                }
            #endif
            
            // Cone culling (backface cluster culling)
            #if ENABLE_CONE_CULLING
                if (visible && (meshlet.flags & uint8_t(MESHLET_FLAG_DOUBLE_SIDED)) == 0u) {
                    // Transform cone axis to world space
                    mat3 normalMatrix = mat3(instance.modelMatrix);
                    vec3 worldConeAxis = normalize(normalMatrix * meshlet.coneAxis);
                    
                    visible = coneCullMeshlet(worldCenter, worldConeAxis, meshlet.coneCutoff, s_cameraPos);
                    
                    #if DEBUG_MODE
                        if (!visible) atomicAdd(coneCulled, 1);
                    #endif
                }
            #endif
            
            // Hierarchical-Z occlusion culling
            #if ENABLE_HIZ_CULLING
                if (visible) {
                    mat4 mvp = s_viewProj * instance.modelMatrix;
                    visible = hiZOcclusionCull(worldCenter, worldRadius, mvp);
                    
                    #if DEBUG_MODE
                        if (!visible) atomicAdd(occlusionCulled, 1);
                    #endif
                }
            #endif
            
            #if DEBUG_MODE
                atomicAdd(totalMeshletsProcessed, 1);
            #endif
        }
    #else
        // Direct meshlet mode
        if (validMeshlet) {
            meshletIndex = meshletGlobalIndex;
            meshlet = meshlets[meshletIndex];
            
            // Similar culling logic as above...
            visible = true;
            // ... (culling code)
        }
    #endif
    
    // ─────────────────────────────────────────────────────────────────────
    // PHASE 4: Subgroup compaction
    // ─────────────────────────────────────────────────────────────────────
    
    // Ballot to find visible meshlets
    uvec4 visibleBallot = subgroupBallot(visible);
    uint subgroupVisibleCount = subgroupBallotBitCount(visibleBallot);
    uint indexInSubgroup = subgroupBallotExclusiveBitCount(visibleBallot);
    
    // Get base offset in payload for this subgroup
    uint subgroupBaseOffset = 0;
    if (subgroupElect()) {
        subgroupBaseOffset = atomicAdd(s_visibleCount, subgroupVisibleCount);
    }
    subgroupBaseOffset = subgroupBroadcastFirst(subgroupBaseOffset);
    
    barrier();
    
    // ─────────────────────────────────────────────────────────────────────
    // PHASE 5: Write payload
    // ─────────────────────────────────────────────────────────────────────
    
    // Check for overflow
    uint totalVisible = s_visibleCount;
    
    if (totalVisible > MAX_MESHLETS_PER_TASK) {
        // TODO: Handle overflow - could emit multiple task dispatches
        // For now, clamp
        totalVisible = MAX_MESHLETS_PER_TASK;
    }
    
    if (visible) {
        uint payloadIndex = subgroupBaseOffset + indexInSubgroup;
        
        if (payloadIndex < MAX_MESHLETS_PER_TASK) {
            payload.tasks[payloadIndex] = MeshletTask(
                meshletIndex,
                instanceIdx,
                selectedLOD,
                meshlet.materialIndex
            );
            
            #if DEBUG_MODE
                atomicAdd(totalMeshletsDrawn, 1);
            #endif
        }
    }
    
    // First thread writes shared payload data
    if (lID == 0) {
        payload.taskCount = min(s_visibleCount, MAX_MESHLETS_PER_TASK);
        payload.viewProj = s_viewProj;
        payload.cameraPosition = s_cameraPos;
        payload.frameIndex = ubo.frameIndex;
    }
    
    barrier();
    
    // ─────────────────────────────────────────────────────────────────────
    // PHASE 6: Emit mesh tasks
    // ─────────────────────────────────────────────────────────────────────
    
    // Launch one mesh shader workgroup per visible meshlet
    EmitMeshTasksEXT(min(s_visibleCount, MAX_MESHLETS_PER_TASK), 1, 1);
}
