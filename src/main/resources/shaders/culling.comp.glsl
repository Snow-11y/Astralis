#version 460
#extension GL_EXT_shader_16bit_storage : require
#extension GL_EXT_shader_8bit_storage : require
#extension GL_KHR_shader_subgroup_arithmetic : require
#extension GL_KHR_shader_subgroup_ballot : require
#extension GL_EXT_shader_atomic_float : enable

layout(local_size_x = 256) in;

// ═══════════════════════════════════════════════════════════════════════════
// STRUCTS
// ═══════════════════════════════════════════════════════════════════════════

// VkDrawIndexedIndirectCommand (20 bytes, padded to 32 for alignment)
struct DrawCommand {
    uint indexCount;
    uint instanceCount;
    uint firstIndex;
    int  vertexOffset;
    uint firstInstance;
    uint _pad0;
    uint _pad1;
    uint _pad2;
};

// Per-mesh-type metadata for LOD selection
struct MeshLODInfo {
    vec4  lodDistances;      // Distance thresholds for 4 LOD levels
    uvec4 lodIndexCounts;    // Triangle counts per LOD
    uvec4 lodFirstIndices;   // Starting index per LOD
    uint  baseMaterialID;
    uint  lodCount;
    uint  flags;
    uint  _pad;
};

// Per-instance ECS data (96 bytes, cache-line friendly)
struct InstanceData {
    mat4 modelMatrix;        // 64 bytes
    vec4 boundingSphere;     // xyz = local center, w = radius (16 bytes)
    uint meshTypeIndex;      // Index into MeshLODInfo array
    uint flags;              // Instance flags
    uint customData;         // User-defined (e.g., animation index)
    float sortKey;           // For transparency sorting
};

// Compacted output for rendering
struct VisibleInstance {
    uint instanceID;         // Original ECS entity ID
    uint meshTypeAndLOD;     // Packed: meshType (24 bits) | LOD (8 bits)
    float depth;             // View-space depth for sorting
    uint batchID;            // Which draw command to use
};

// ═══════════════════════════════════════════════════════════════════════════
// CONSTANTS & FLAGS
// ═══════════════════════════════════════════════════════════════════════════

// Instance flags
const uint INSTANCE_FLAG_ENABLED        = 1u << 0;
const uint INSTANCE_FLAG_CAST_SHADOW    = 1u << 1;
const uint INSTANCE_FLAG_STATIC         = 1u << 2;
const uint INSTANCE_FLAG_ALWAYS_VISIBLE = 1u << 3;
const uint INSTANCE_FLAG_TWO_SIDED      = 1u << 4;
const uint INSTANCE_FLAG_TRANSPARENT    = 1u << 5;

// Culling mode flags
const uint CULL_FLAG_FRUSTUM            = 1u << 0;
const uint CULL_FLAG_HIZ_OCCLUSION      = 1u << 1;
const uint CULL_FLAG_CONTRIBUTION       = 1u << 2;
const uint CULL_FLAG_DISTANCE           = 1u << 3;
const uint CULL_FLAG_BACKFACE           = 1u << 4;
const uint CULL_FLAG_USE_LOD            = 1u << 5;
const uint CULL_FLAG_SHADOW_PASS        = 1u << 6;

// ═══════════════════════════════════════════════════════════════════════════
// BINDINGS
// ═══════════════════════════════════════════════════════════════════════════

// Input: All world instances
layout(std430, binding = 0) readonly buffer InstanceBuffer {
    InstanceData instances[];
};

// Output: Indirect draw commands (one per mesh type * LOD level)
layout(std430, binding = 1) buffer CommandBuffer {
    DrawCommand commands[];
};

// Output: Compacted visible instance list
layout(std430, binding = 2) writeonly buffer VisibilityBuffer {
    VisibleInstance visibleInstances[];
};

// Input: Per-mesh-type LOD information
layout(std430, binding = 3) readonly buffer MeshInfoBuffer {
    MeshLODInfo meshInfos[];
};

// Atomic counter for total visible instances
layout(std430, binding = 4) buffer CounterBuffer {
    uint totalVisibleCount;
    uint totalTestedCount;
    uint frustumCulledCount;
    uint occlusionCulledCount;
    uint contributionCulledCount;
    uint distanceCulledCount;
};

// Hierarchical Z-Buffer (max depth pyramid)
layout(binding = 5) uniform sampler2D hiZBuffer;

// Previous frame's depth for temporal reprojection
layout(binding = 6) uniform sampler2D prevDepthBuffer;

// Uniforms
layout(std140, binding = 7) uniform CullingUniforms {
    // Matrices (64 * 4 = 256 bytes)
    mat4 view;
    mat4 proj;
    mat4 viewProj;
    mat4 prevViewProj;           // For temporal reprojection
    
    // Frustum planes (6 * 16 = 96 bytes)
    vec4 frustumPlanes[6];
    
    // Camera info (48 bytes)
    vec4 cameraPosition;         // xyz = pos, w = unused
    vec4 cameraForward;          // xyz = forward, w = unused
    vec2 viewportSize;
    float nearPlane;
    float farPlane;
    
    // Culling parameters (32 bytes)
    uint totalInstanceCount;
    uint meshTypeCount;
    uint cullingFlags;
    uint frameIndex;
    float lodBias;               // Global LOD bias multiplier
    float minPixelSize;          // Contribution cull threshold
    float maxCullDistance;       // Distance cull threshold
    float hiZBias;               // Depth bias for HiZ testing
} ubo;

// ═══════════════════════════════════════════════════════════════════════════
// SHARED MEMORY
// ═══════════════════════════════════════════════════════════════════════════

shared vec4 s_frustumPlanes[6];
shared mat4 s_viewProj;
shared vec4 s_cameraPos;

// Subgroup-level compaction scratch
shared uint s_groupVisibleCount;
shared uint s_groupBaseOffset;

// ═══════════════════════════════════════════════════════════════════════════
// UTILITY FUNCTIONS  
// ═══════════════════════════════════════════════════════════════════════════

// Transform bounding sphere to world space, accounting for non-uniform scale
vec4 transformBoundingSphere(mat4 modelMatrix, vec4 localSphere) {
    // Transform center to world space
    vec3 worldCenter = (modelMatrix * vec4(localSphere.xyz, 1.0)).xyz;
    
    // For radius, we need the maximum scale factor
    // Extract scale from matrix columns
    float scaleX = length(modelMatrix[0].xyz);
    float scaleY = length(modelMatrix[1].xyz);
    float scaleZ = length(modelMatrix[2].xyz);
    float maxScale = max(max(scaleX, scaleY), scaleZ);
    
    return vec4(worldCenter, localSphere.w * maxScale);
}

// Project sphere to screen space and return pixel diameter
float getProjectedPixelSize(vec3 worldCenter, float worldRadius) {
    float viewDist = distance(worldCenter, s_cameraPos.xyz);
    
    // Avoid division by zero for very close objects
    if (viewDist < worldRadius) return 1e6; // Treat as very large
    
    // Projected diameter in pixels
    // Using vertical FOV from projection matrix: proj[1][1] = 1/tan(fov/2)
    return (ubo.proj[1][1] * worldRadius * 2.0 * ubo.viewportSize.y) / viewDist;
}

// ═══════════════════════════════════════════════════════════════════════════
// CULLING FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

// Fast frustum-sphere test using shared memory planes
bool frustumCullSphere(vec3 center, float radius) {
    // Unrolled for better performance on most GPUs
    float d0 = dot(s_frustumPlanes[0].xyz, center) + s_frustumPlanes[0].w;
    float d1 = dot(s_frustumPlanes[1].xyz, center) + s_frustumPlanes[1].w;
    float d2 = dot(s_frustumPlanes[2].xyz, center) + s_frustumPlanes[2].w;
    float d3 = dot(s_frustumPlanes[3].xyz, center) + s_frustumPlanes[3].w;
    float d4 = dot(s_frustumPlanes[4].xyz, center) + s_frustumPlanes[4].w;
    float d5 = dot(s_frustumPlanes[5].xyz, center) + s_frustumPlanes[5].w;
    
    // Object is outside if completely behind any plane
    return !((d0 < -radius) || (d1 < -radius) || (d2 < -radius) ||
             (d3 < -radius) || (d4 < -radius) || (d5 < -radius));
}

// Hierarchical Z-Buffer occlusion culling
bool hiZOcclusionTest(vec3 worldCenter, float worldRadius, out float outDepth) {
    // Project sphere center to clip space
    vec4 clipPos = s_viewProj * vec4(worldCenter, 1.0);
    
    // Behind camera check
    if (clipPos.w <= 0.0) {
        outDepth = 0.0;
        return false;
    }
    
    // NDC coordinates
    vec3 ndc = clipPos.xyz / clipPos.w;
    outDepth = ndc.z * 0.5 + 0.5; // Convert to [0,1]
    
    // Outside NDC bounds (frustum should have caught this, but double-check)
    if (any(greaterThan(abs(ndc.xy), vec2(1.3)))) {
        return true; // Partially visible, don't cull
    }
    
    // Calculate projected radius in NDC
    float viewDist = clipPos.w;
    float projectedRadius = (ubo.proj[1][1] * worldRadius) / viewDist;
    
    // Convert to texture coordinates
    vec2 centerUV = ndc.xy * 0.5 + 0.5;
    
    // Calculate screen-space bounding box
    vec2 minUV = clamp(centerUV - projectedRadius, vec2(0.0), vec2(1.0));
    vec2 maxUV = clamp(centerUV + projectedRadius, vec2(0.0), vec2(1.0));
    
    // Determine mip level based on bounding box size in pixels
    vec2 boxSize = (maxUV - minUV) * ubo.viewportSize;
    float mipLevel = ceil(log2(max(boxSize.x, boxSize.y)));
    mipLevel = clamp(mipLevel, 0.0, float(textureQueryLevels(hiZBuffer) - 1));
    
    // Sample HiZ at corners and center for better coverage
    vec2 sampleUVs[5] = vec2[5](
        (minUV + maxUV) * 0.5,   // Center
        minUV,                    // Bottom-left
        maxUV,                    // Top-right
        vec2(minUV.x, maxUV.y),  // Top-left
        vec2(maxUV.x, minUV.y)   // Bottom-right
    );
    
    float maxHiZDepth = 0.0;
    for (int i = 0; i < 5; i++) {
        maxHiZDepth = max(maxHiZDepth, textureLod(hiZBuffer, sampleUVs[i], mipLevel).r);
    }
    
    // Calculate closest depth of the bounding sphere
    // Subtract radius in view space, converted to depth
    float closestDepth = outDepth - (projectedRadius / viewDist) * 0.5;
    closestDepth -= ubo.hiZBias; // Apply bias
    
    // Visible if object's closest depth is in front of or at the HiZ depth
    return closestDepth <= maxHiZDepth;
}

// Simple backface culling for closed convex objects
bool backfaceCull(vec3 worldCenter, mat4 modelMatrix) {
    // Get the object's primary facing direction (assumes +Z forward in local space)
    vec3 objectForward = normalize(modelMatrix[2].xyz);
    vec3 toCamera = normalize(s_cameraPos.xyz - worldCenter);
    
    // Cull if facing away (with some threshold for edge cases)
    return dot(objectForward, toCamera) > -0.2;
}

// LOD selection based on projected size and distance
uint selectLOD(float pixelSize, float distance, MeshLODInfo meshInfo) {
    // Apply global LOD bias
    float biasedDistance = distance * ubo.lodBias;
    
    // Use distance thresholds from mesh info
    for (uint lod = 0; lod < meshInfo.lodCount - 1; lod++) {
        if (biasedDistance < meshInfo.lodDistances[lod]) {
            return lod;
        }
    }
    
    return meshInfo.lodCount - 1;
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN CULLING KERNEL
// ═══════════════════════════════════════════════════════════════════════════

void main() {
    uint gID = gl_GlobalInvocationID.x;
    uint lID = gl_LocalInvocationID.x;
    
    // ─────────────────────────────────────────────────────────────────────
    // PHASE 1: Load shared data (first few threads)
    // ─────────────────────────────────────────────────────────────────────
    
    if (lID < 6) {
        s_frustumPlanes[lID] = ubo.frustumPlanes[lID];
    }
    if (lID == 0) {
        s_viewProj = ubo.viewProj;
        s_cameraPos = ubo.cameraPosition;
        s_groupVisibleCount = 0;
    }
    
    barrier();
    memoryBarrierShared();
    
    // ─────────────────────────────────────────────────────────────────────
    // PHASE 2: Early exit for out-of-bounds threads
    // ─────────────────────────────────────────────────────────────────────
    
    bool valid = gID < ubo.totalInstanceCount;
    InstanceData data;
    vec4 worldSphere;
    
    if (valid) {
        data = instances[gID];
        valid = (data.flags & INSTANCE_FLAG_ENABLED) != 0u;
    }
    
    // ─────────────────────────────────────────────────────────────────────
    // PHASE 3: Multi-stage culling pipeline
    // ─────────────────────────────────────────────────────────────────────
    
    bool visible = false;
    float depth = 1.0;
    uint selectedLOD = 0;
    float pixelSize = 0.0;
    
    if (valid) {
        // Transform bounding sphere to world space
        worldSphere = transformBoundingSphere(data.modelMatrix, data.boundingSphere);
        
        // Check always-visible flag
        if ((data.flags & INSTANCE_FLAG_ALWAYS_VISIBLE) != 0u) {
            visible = true;
        } else {
            visible = true;
            
            // Stage 1: Frustum Culling (cheapest)
            if (visible && (ubo.cullingFlags & CULL_FLAG_FRUSTUM) != 0u) {
                visible = frustumCullSphere(worldSphere.xyz, worldSphere.w);
                if (!visible) atomicAdd(frustumCulledCount, 1);
            }
            
            // Stage 2: Distance Culling
            float distance = length(worldSphere.xyz - s_cameraPos.xyz);
            if (visible && (ubo.cullingFlags & CULL_FLAG_DISTANCE) != 0u) {
                visible = distance < ubo.maxCullDistance;
                if (!visible) atomicAdd(distanceCulledCount, 1);
            }
            
            // Stage 3: Contribution/Small-Object Culling  
            if (visible && (ubo.cullingFlags & CULL_FLAG_CONTRIBUTION) != 0u) {
                pixelSize = getProjectedPixelSize(worldSphere.xyz, worldSphere.w);
                visible = pixelSize >= ubo.minPixelSize;
                if (!visible) atomicAdd(contributionCulledCount, 1);
            }
            
            // Stage 4: Backface Culling (for closed convex objects)
            if (visible && (ubo.cullingFlags & CULL_FLAG_BACKFACE) != 0u) {
                if ((data.flags & INSTANCE_FLAG_TWO_SIDED) == 0u) {
                    visible = backfaceCull(worldSphere.xyz, data.modelMatrix);
                }
            }
            
            // Stage 5: Hierarchical-Z Occlusion Culling (most expensive)
            if (visible && (ubo.cullingFlags & CULL_FLAG_HIZ_OCCLUSION) != 0u) {
                visible = hiZOcclusionTest(worldSphere.xyz, worldSphere.w, depth);
                if (!visible) atomicAdd(occlusionCulledCount, 1);
            }
        }
        
        // LOD Selection
        if (visible && (ubo.cullingFlags & CULL_FLAG_USE_LOD) != 0u) {
            MeshLODInfo meshInfo = meshInfos[data.meshTypeIndex];
            float distance = length(worldSphere.xyz - s_cameraPos.xyz);
            if (pixelSize == 0.0) {
                pixelSize = getProjectedPixelSize(worldSphere.xyz, worldSphere.w);
            }
            selectedLOD = selectLOD(pixelSize, distance, meshInfo);
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────
    // PHASE 4: Subgroup-optimized compaction
    // ─────────────────────────────────────────────────────────────────────
    
    // Use subgroup ballot to count visible instances
    uvec4 visibleMask = subgroupBallot(visible);
    uint subgroupVisibleCount = subgroupBallotBitCount(visibleMask);
    uint subgroupLocalIndex = subgroupBallotExclusiveBitCount(visibleMask);
    
    // First lane in each subgroup atomically reserves slots
    uint subgroupBaseOffset = 0;
    if (subgroupElect()) {
        subgroupBaseOffset = atomicAdd(s_groupVisibleCount, subgroupVisibleCount);
    }
    subgroupBaseOffset = subgroupBroadcastFirst(subgroupBaseOffset);
    
    barrier();
    
    // First thread in workgroup reserves global slots
    if (lID == 0) {
        s_groupBaseOffset = atomicAdd(totalVisibleCount, s_groupVisibleCount);
    }
    
    barrier();
    
    // ─────────────────────────────────────────────────────────────────────
    // PHASE 5: Write compacted output
    // ─────────────────────────────────────────────────────────────────────
    
    if (visible) {
        uint outputIndex = s_groupBaseOffset + subgroupBaseOffset + subgroupLocalIndex;
        
        // Pack mesh type and LOD into single uint
        uint meshTypeAndLOD = (data.meshTypeIndex & 0x00FFFFFFu) | (selectedLOD << 24);
        
        // Calculate batch ID (which draw command to use)
        // Assumes draw commands are organized as: meshType * maxLODs + lodLevel
        uint maxLODs = 4;
        uint batchID = data.meshTypeIndex * maxLODs + selectedLOD;
        
        // Write visibility data
        visibleInstances[outputIndex] = VisibleInstance(
            gID,
            meshTypeAndLOD,
            depth,
            batchID
        );
        
        // Increment instance count in the appropriate draw command
        atomicAdd(commands[batchID].instanceCount, 1);
    }
    
    // Update statistics (optional, for debugging)
    if (valid) {
        atomicAdd(totalTestedCount, 1);
    }
}
