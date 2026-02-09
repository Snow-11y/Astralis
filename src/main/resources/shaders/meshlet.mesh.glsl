#version 460
#extension GL_EXT_mesh_shader : require
#extension GL_KHR_shader_subgroup_ballot : require
#extension GL_KHR_shader_subgroup_arithmetic : require
#extension GL_KHR_shader_subgroup_shuffle : require
#extension GL_KHR_shader_subgroup_vote : require
#extension GL_EXT_shader_explicit_arithmetic_types : require
#extension GL_EXT_shader_16bit_storage : require
#extension GL_EXT_shader_8bit_storage : require
#extension GL_EXT_control_flow_attributes : require
#extension GL_EXT_multiview : enable
#extension GL_EXT_fragment_shading_rate : enable

// ═══════════════════════════════════════════════════════════════════════════
// COMPILE-TIME CONFIGURATION
// ═══════════════════════════════════════════════════════════════════════════

// Meshlet limits - tune based on hardware capabilities
#ifndef MAX_MESHLET_VERTICES
#define MAX_MESHLET_VERTICES 64
#endif

#ifndef MAX_MESHLET_TRIANGLES
#define MAX_MESHLET_TRIANGULAR 124
#endif

#ifndef WORKGROUP_SIZE
#define WORKGROUP_SIZE 32
#endif

// Feature toggles
#ifndef ENABLE_VERTEX_COMPRESSION
#define ENABLE_VERTEX_COMPRESSION 1
#endif

#ifndef ENABLE_SKINNING
#define ENABLE_SKINNING 0
#endif

#ifndef ENABLE_MORPH_TARGETS
#define ENABLE_MORPH_TARGETS 0
#endif

#ifndef ENABLE_DISPLACEMENT
#define ENABLE_DISPLACEMENT 0
#endif

#ifndef ENABLE_MOTION_VECTORS
#define ENABLE_MOTION_VECTORS 1
#endif

#ifndef ENABLE_MICRO_TRIANGLE_CULLING
#define ENABLE_MICRO_TRIANGLE_CULLING 1
#endif

#ifndef ENABLE_BACKFACE_CULLING
#define ENABLE_BACKFACE_CULLING 1
#endif

#ifndef ENABLE_FRUSTUM_CULLING_TRIANGLES
#define ENABLE_FRUSTUM_CULLING_TRIANGLES 0
#endif

#ifndef ENABLE_VARIABLE_RATE_SHADING
#define ENABLE_VARIABLE_RATE_SHADING 0
#endif

#ifndef ENABLE_MULTI_VIEW
#define ENABLE_MULTI_VIEW 0
#endif

#ifndef VISIBILITY_BUFFER_MODE
#define VISIBILITY_BUFFER_MODE 0
#endif

#ifndef SHADOW_PASS
#define SHADOW_PASS 0
#endif

#ifndef DEBUG_MODE
#define DEBUG_MODE 0
#endif

// ═══════════════════════════════════════════════════════════════════════════
// LAYOUT DECLARATIONS
// ═══════════════════════════════════════════════════════════════════════════

layout(local_size_x = WORKGROUP_SIZE) in;
layout(triangles, max_vertices = MAX_MESHLET_VERTICES, max_primitives = MAX_MESHLET_TRIANGLES) out;

// ═══════════════════════════════════════════════════════════════════════════
// STRUCTURES
// ═══════════════════════════════════════════════════════════════════════════

// Meshlet descriptor (must match task shader exactly)
struct MeshletDesc {
    // Bounding volumes (32 bytes)
    vec3 boundingSphereCenter;
    float boundingSphereRadius;
    vec3 coneAxis;
    float coneCutoff;
    
    // Extended bounds (24 bytes)
    vec3 bboxMin;
    vec3 bboxMax;
    
    // Data offsets (8 bytes)  
    uint vertexOffset;
    uint triangleOffset;
    
    // Counts and flags (4 bytes)
    uint8_t vertexCount;
    uint8_t triangleCount;
    uint8_t lodLevel;
    uint8_t flags;
    
    // Additional info (8 bytes)
    uint parentClusterIndex;
    uint materialIndex;
};

// Task payload from task shader
struct MeshletTask {
    uint meshletIndex;
    uint instanceIndex;
    uint lodLevel;
    uint materialIndex;
};

struct TaskPayload {
    MeshletTask tasks[32];
    uint taskCount;
    
    // Cached camera data
    mat4 viewProj;
    vec3 cameraPosition;
    uint frameIndex;
    
    // For multi-view
#if ENABLE_MULTI_VIEW
    mat4 viewProjMultiView[2];
#endif
};

taskPayloadSharedEXT TaskPayload payload;

// Compressed vertex (32 bytes)
struct VertexCompressed {
    uint16_t posX, posY, posZ;    // Half-float position
    uint16_t posW;                 // Tangent sign + extra data
    uint packedNormalTangent;      // Octahedron encoded normal + tangent
    uint16_t uv0X, uv0Y;          // Primary UV
    uint16_t uv1X, uv1Y;          // Secondary UV (lightmap)
    uint8_t boneIndices[4];        // Skinning bones
    uint8_t boneWeights[4];        // Skinning weights
};

// Full vertex (for non-compressed mode)
struct VertexFull {
    vec3 position;
    vec3 normal;
    vec4 tangent;
    vec2 uv0;
    vec2 uv1;
    uvec4 boneIndices;
    vec4 boneWeights;
};

// Instance data
struct InstanceData {
    mat4 modelMatrix;
    mat4 prevModelMatrix;
    vec4 boundingSphere;
    uint meshLODGroupIndex;
    uint flags;
    uint skeletonOffset;          // For skinning
    float lodBias;
};

// Bone transform for skinning
struct BoneTransform {
    mat4 currentMatrix;
    mat4 previousMatrix;
};

// Morph target delta
struct MorphDelta {
    int16_t deltaX, deltaY, deltaZ;  // Position delta
    int16_t deltaNX, deltaNY, deltaNZ; // Normal delta
};

// ═══════════════════════════════════════════════════════════════════════════
// BINDINGS
// ═══════════════════════════════════════════════════════════════════════════

// Meshlet descriptors
layout(std430, binding = 0) readonly buffer MeshletBuffer {
    MeshletDesc meshlets[];
};

// Instance data
layout(std430, binding = 1) readonly buffer InstanceBuffer {
    InstanceData instances[];
};

// Vertex data
#if ENABLE_VERTEX_COMPRESSION
layout(std430, binding = 2) readonly buffer VertexBuffer {
    VertexCompressed vertices[];
};
#else
layout(std430, binding = 2) readonly buffer VertexBuffer {
    VertexFull vertices[];
};
#endif

// Meshlet vertex indices (local to global)
layout(std430, binding = 3) readonly buffer MeshletVertexBuffer {
    uint meshletVertices[];
};

// Meshlet triangle indices (local indices)
layout(std430, binding = 4) readonly buffer MeshletTriangleBuffer {
    uint8_t meshletTriangles[];
};

// Skinning bones
#if ENABLE_SKINNING
layout(std430, binding = 5) readonly buffer BoneBuffer {
    BoneTransform bones[];
};
#endif

// Morph targets
#if ENABLE_MORPH_TARGETS
layout(std430, binding = 6) readonly buffer MorphTargetBuffer {
    MorphDelta morphDeltas[];
};

layout(std430, binding = 7) readonly buffer MorphWeightBuffer {
    float morphWeights[];
};
#endif

// Displacement map
#if ENABLE_DISPLACEMENT
layout(binding = 8) uniform sampler2D displacementMap;
#endif

// Camera uniforms
layout(std140, binding = 9) uniform CameraUBO {
    mat4 view;
    mat4 proj;
    mat4 viewProj;
    mat4 prevViewProj;
    mat4 invViewProj;
    
#if ENABLE_MULTI_VIEW
    mat4 viewProjEye[2];
    mat4 prevViewProjEye[2];
#endif
    
    vec4 cameraPosition;
    vec4 cameraForward;
    vec4 cameraUp;
    vec4 cameraRight;
    
    vec4 frustumPlanes[6];
    
    vec2 viewportSize;
    vec2 invViewportSize;
    
    float nearPlane;
    float farPlane;
    float time;
    float deltaTime;
    
    // Culling thresholds
    float microTriangleThreshold;    // Pixels - cull triangles smaller than this
    float backfaceCullBias;          // Dot product threshold
    float displacementScale;
    uint frameIndex;
} camera;

// Push constants
layout(push_constant) uniform PushConstants {
    uint passFlags;
    uint debugFlags;
    float tessellationFactor;
    float displacementBias;
} push;

// ═══════════════════════════════════════════════════════════════════════════
// OUTPUTS
// ═══════════════════════════════════════════════════════════════════════════

#if SHADOW_PASS
    // Minimal outputs for shadow pass
    layout(location = 0) out ShadowOutput {
        float linearDepth;
    } vs_out[];
#elif VISIBILITY_BUFFER_MODE
    // Visibility buffer mode - minimal interpolants
    layout(location = 0) out VisibilityOutput {
        flat uint triangleID;         // Unique triangle identifier
        flat uint materialIndex;
        vec2 barycentrics;            // For attribute reconstruction
    } vs_out[];
#else
    // Full deferred/forward outputs
    layout(location = 0) out VertexOutput {
        vec3 worldPosition;
        vec3 worldNormal;
        vec4 worldTangent;            // xyz = tangent, w = bitangent sign
        vec2 texCoord0;
        vec2 texCoord1;
        
    #if ENABLE_MOTION_VECTORS
        vec4 currentClipPos;
        vec4 previousClipPos;
    #endif
        
        flat uint materialIndex;
        flat uint instanceID;
        
    #if DEBUG_MODE
        flat uint meshletID;
        flat uint triangleIndex;
        flat vec3 debugColor;
    #endif
    } vs_out[];
#endif

// Per-primitive outputs
layout(location = 8) perprimitiveEXT out PerPrimitiveData {
    flat uint primitiveFlags;         // Backface bit, edge flags, etc.
#if ENABLE_VARIABLE_RATE_SHADING
    flat uint shadingRate;            // VRS hint
#endif
} prim_out[];

// ═══════════════════════════════════════════════════════════════════════════
// SHARED MEMORY
// ═══════════════════════════════════════════════════════════════════════════

// Vertex attributes in shared memory for efficient access
shared vec4 s_clipPositions[MAX_MESHLET_VERTICES];
shared vec3 s_worldPositions[MAX_MESHLET_VERTICES];

#if !SHADOW_PASS
shared vec3 s_worldNormals[MAX_MESHLET_VERTICES];
shared vec4 s_worldTangents[MAX_MESHLET_VERTICES];
shared vec4 s_texCoords[MAX_MESHLET_VERTICES];      // xy = uv0, zw = uv1
#endif

#if ENABLE_MOTION_VECTORS
shared vec4 s_prevClipPositions[MAX_MESHLET_VERTICES];
#endif

// Triangle culling results
shared uint s_triangleVisible[MAX_MESHLET_TRIANGLES / 32 + 1];
shared uint s_visibleTriangleCount;
shared uint s_triangleIndices[MAX_MESHLET_TRIANGLES];
shared uvec3 s_triangleData[MAX_MESHLET_TRIANGLES];

// ═══════════════════════════════════════════════════════════════════════════
// VERTEX DECODING FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

// Decode octahedron-encoded normal
vec3 octDecode(vec2 f) {
    f = f * 2.0 - 1.0;
    vec3 n = vec3(f.x, f.y, 1.0 - abs(f.x) - abs(f.y));
    float t = max(-n.z, 0.0);
    n.xy += mix(vec2(t), vec2(-t), greaterThanEqual(n.xy, vec2(0.0)));
    return normalize(n);
}

// Unpack snorm8 to float
float unpackSnorm8(int8_t v) {
    return max(float(v) / 127.0, -1.0);
}

// Half to float conversion
float halfToFloat(uint16_t h) {
    return unpackHalf2x16(uint(h)).x;
}

// Full vertex unpacking for compressed format
void unpackVertex(VertexCompressed v, 
                  out vec3 position, 
                  out vec3 normal, 
                  out vec4 tangent,
                  out vec2 uv0, 
                  out vec2 uv1,
                  out uvec4 boneIdx,
                  out vec4 boneWgt) {
    // Position from half-floats
    position = vec3(
        halfToFloat(v.posX),
        halfToFloat(v.posY),
        halfToFloat(v.posZ)
    );
    
    // Tangent sign encoded in posW
    float tangentSign = halfToFloat(v.posW) >= 0.0 ? 1.0 : -1.0;
    
    // Unpack normal and tangent from octahedron encoding
    int8_t nx = int8_t(v.packedNormalTangent & 0xFF);
    int8_t ny = int8_t((v.packedNormalTangent >> 8) & 0xFF);
    int8_t tx = int8_t((v.packedNormalTangent >> 16) & 0xFF);
    int8_t ty = int8_t((v.packedNormalTangent >> 24) & 0xFF);
    
    normal = octDecode(vec2(unpackSnorm8(nx), unpackSnorm8(ny)) * 0.5 + 0.5);
    vec3 tang = octDecode(vec2(unpackSnorm8(tx), unpackSnorm8(ty)) * 0.5 + 0.5);
    tangent = vec4(tang, tangentSign);
    
    // UVs from half-floats
    uv0 = vec2(halfToFloat(v.uv0X), halfToFloat(v.uv0Y));
    uv1 = vec2(halfToFloat(v.uv1X), halfToFloat(v.uv1Y));
    
    // Bone data
    boneIdx = uvec4(v.boneIndices[0], v.boneIndices[1], 
                    v.boneIndices[2], v.boneIndices[3]);
    boneWgt = vec4(v.boneWeights[0], v.boneWeights[1],
                   v.boneWeights[2], v.boneWeights[3]) / 255.0;
}

// ═══════════════════════════════════════════════════════════════════════════
// VERTEX PROCESSING FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

// Skeletal animation
#if ENABLE_SKINNING
void applySkinning(uint skeletonOffset, uvec4 boneIdx, vec4 boneWgt,
                   inout vec3 position, inout vec3 normal, inout vec3 tangent,
                   out vec3 prevPosition) {
    mat4 skinMatrix = mat4(0.0);
    mat4 prevSkinMatrix = mat4(0.0);
    
    float totalWeight = 0.0;
    
    [[unroll]]
    for (int i = 0; i < 4; i++) {
        float w = boneWgt[i];
        if (w > 0.001) {
            uint boneIndex = skeletonOffset + boneIdx[i];
            skinMatrix += bones[boneIndex].currentMatrix * w;
            prevSkinMatrix += bones[boneIndex].previousMatrix * w;
            totalWeight += w;
        }
    }
    
    // Normalize if weights don't sum to 1
    if (totalWeight > 0.0 && abs(totalWeight - 1.0) > 0.001) {
        skinMatrix /= totalWeight;
        prevSkinMatrix /= totalWeight;
    }
    
    // Apply skinning transformation
    position = (skinMatrix * vec4(position, 1.0)).xyz;
    prevPosition = (prevSkinMatrix * vec4(position, 1.0)).xyz;
    
    // Transform normals and tangents
    mat3 skinMatrix3 = mat3(skinMatrix);
    normal = normalize(skinMatrix3 * normal);
    tangent = normalize(skinMatrix3 * tangent);
}
#endif

// Morph target blending
#if ENABLE_MORPH_TARGETS
void applyMorphTargets(uint vertexIndex, uint morphTargetOffset, uint morphTargetCount,
                       inout vec3 position, inout vec3 normal) {
    for (uint m = 0; m < morphTargetCount; m++) {
        float weight = morphWeights[morphTargetOffset + m];
        if (abs(weight) < 0.001) continue;
        
        uint deltaIndex = (morphTargetOffset + m) * /* vertexCount */ + vertexIndex;
        MorphDelta delta = morphDeltas[deltaIndex];
        
        // Apply position delta (stored as fixed-point)
        position += vec3(delta.deltaX, delta.deltaY, delta.deltaZ) / 32767.0 * weight;
        
        // Apply normal delta
        vec3 normalDelta = vec3(delta.deltaNX, delta.deltaNY, delta.deltaNZ) / 32767.0;
        normal = normalize(normal + normalDelta * weight);
    }
}
#endif

// Displacement mapping
#if ENABLE_DISPLACEMENT
vec3 applyDisplacement(vec3 position, vec3 normal, vec2 uv) {
    float displacement = texture(displacementMap, uv).r;
    displacement = displacement * 2.0 - 1.0;  // Assume [0,1] -> [-1,1]
    displacement = displacement * camera.displacementScale + push.displacementBias;
    
    return position + normal * displacement;
}
#endif

// Proper normal matrix calculation
mat3 computeNormalMatrix(mat4 modelMatrix) {
    // For uniform scale, use upper 3x3
    // For non-uniform scale, use inverse transpose
    mat3 m = mat3(modelMatrix);
    
    // Check for uniform scale by comparing column lengths
    float sx = length(m[0]);
    float sy = length(m[1]);
    float sz = length(m[2]);
    
    float avgScale = (sx + sy + sz) / 3.0;
    bool uniformScale = abs(sx - avgScale) < 0.001 && 
                        abs(sy - avgScale) < 0.001 && 
                        abs(sz - avgScale) < 0.001;
    
    if (uniformScale) {
        return m / sx;  // Normalize by uniform scale
    } else {
        // Full inverse transpose for non-uniform scale
        return transpose(inverse(m));
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// TRIANGLE CULLING FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

// Check if triangle is backfacing
bool isBackface(vec4 p0, vec4 p1, vec4 p2) {
    // NDC positions
    vec2 ndc0 = p0.xy / p0.w;
    vec2 ndc1 = p1.xy / p1.w;
    vec2 ndc2 = p2.xy / p2.w;
    
    // 2D cross product (signed area)
    vec2 e0 = ndc1 - ndc0;
    vec2 e1 = ndc2 - ndc0;
    float signedArea = e0.x * e1.y - e0.y * e1.x;
    
    // Negative = backface (assuming CCW front)
    return signedArea < camera.backfaceCullBias;
}

// Check if triangle is too small to render (micro-triangle)
bool isMicroTriangle(vec4 p0, vec4 p1, vec4 p2) {
    // Convert to screen space
    vec2 ss0 = (p0.xy / p0.w) * 0.5 + 0.5;
    vec2 ss1 = (p1.xy / p1.w) * 0.5 + 0.5;
    vec2 ss2 = (p2.xy / p2.w) * 0.5 + 0.5;
    
    ss0 *= camera.viewportSize;
    ss1 *= camera.viewportSize;
    ss2 *= camera.viewportSize;
    
    // Calculate bounding box
    vec2 minBB = min(ss0, min(ss1, ss2));
    vec2 maxBB = max(ss0, max(ss1, ss2));
    vec2 size = maxBB - minBB;
    
    // Cull if smaller than threshold
    return max(size.x, size.y) < camera.microTriangleThreshold;
}

// Check if triangle is completely outside frustum
bool isOutsideFrustum(vec4 p0, vec4 p1, vec4 p2) {
    // Check if all vertices are outside the same clip plane
    
    // Left plane: x > -w
    if (p0.x < -p0.w && p1.x < -p1.w && p2.x < -p2.w) return true;
    // Right plane: x < w
    if (p0.x > p0.w && p1.x > p1.w && p2.x > p2.w) return true;
    // Bottom plane: y > -w
    if (p0.y < -p0.w && p1.y < -p1.w && p2.y < -p2.w) return true;
    // Top plane: y < w
    if (p0.y > p0.w && p1.y > p1.w && p2.y > p2.w) return true;
    // Near plane: z > 0 (reversed-z) or z > -w (standard)
    if (p0.z < 0.0 && p1.z < 0.0 && p2.z < 0.0) return true;
    // Far plane: z < w
    if (p0.z > p0.w && p1.z > p1.w && p2.z > p2.w) return true;
    
    return false;
}

// Check if triangle is degenerate
bool isDegenerate(vec4 p0, vec4 p1, vec4 p2) {
    // Behind camera
    if (p0.w <= 0.0 && p1.w <= 0.0 && p2.w <= 0.0) return true;
    
    // Zero area (in world space would be better but this is fast)
    vec2 ndc0 = p0.xy / max(p0.w, 0.0001);
    vec2 ndc1 = p1.xy / max(p1.w, 0.0001);
    vec2 ndc2 = p2.xy / max(p2.w, 0.0001);
    
    vec2 e0 = ndc1 - ndc0;
    vec2 e1 = ndc2 - ndc0;
    
    float area = abs(e0.x * e1.y - e0.y * e1.x);
    return area < 1e-8;
}

// Combined triangle culling
bool shouldCullTriangle(vec4 p0, vec4 p1, vec4 p2, bool isDoubleSided) {
    // Check for degenerate triangles first
    if (isDegenerate(p0, p1, p2)) {
        return true;
    }
    
#if ENABLE_FRUSTUM_CULLING_TRIANGLES
    if (isOutsideFrustum(p0, p1, p2)) {
        return true;
    }
#endif
    
#if ENABLE_BACKFACE_CULLING
    if (!isDoubleSided && isBackface(p0, p1, p2)) {
        return true;
    }
#endif
    
#if ENABLE_MICRO_TRIANGLE_CULLING
    if (isMicroTriangle(p0, p1, p2)) {
        return true;
    }
#endif
    
    return false;
}

// Calculate VRS shading rate based on triangle properties
#if ENABLE_VARIABLE_RATE_SHADING
uint calculateShadingRate(vec4 p0, vec4 p1, vec4 p2, vec3 n0, vec3 n1, vec3 n2) {
    // Screen-space size
    vec2 ss0 = (p0.xy / p0.w + 1.0) * 0.5 * camera.viewportSize;
    vec2 ss1 = (p1.xy / p1.w + 1.0) * 0.5 * camera.viewportSize;
    vec2 ss2 = (p2.xy / p2.w + 1.0) * 0.5 * camera.viewportSize;
    
    vec2 minBB = min(ss0, min(ss1, ss2));
    vec2 maxBB = max(ss0, max(ss1, ss2));
    float maxDim = max(maxBB.x - minBB.x, maxBB.y - minBB.y);
    
    // Normal variance (flat surfaces can use coarser shading)
    float normalVar = length(n0 - n1) + length(n1 - n2) + length(n2 - n0);
    
    // Combine heuristics
    // VK_FRAGMENT_SHADING_RATE_*
    const uint RATE_1X1 = 0;
    const uint RATE_1X2 = 1;
    const uint RATE_2X1 = 4;
    const uint RATE_2X2 = 5;
    const uint RATE_2X4 = 6;
    const uint RATE_4X2 = 9;
    const uint RATE_4X4 = 10;
    
    if (maxDim > 64.0 && normalVar < 0.1) {
        return RATE_4X4;  // Large, flat triangles
    } else if (maxDim > 32.0 && normalVar < 0.2) {
        return RATE_2X2;
    } else if (maxDim > 16.0) {
        return RATE_1X2;  // or RATE_2X1 based on orientation
    }
    
    return RATE_1X1;  // Full rate for small/detailed triangles
}
#endif

// ═══════════════════════════════════════════════════════════════════════════
// DEBUG VISUALIZATION
// ═══════════════════════════════════════════════════════════════════════════

#if DEBUG_MODE
vec3 getMeshletColor(uint meshletID) {
    // Generate consistent pseudo-random color per meshlet
    float r = fract(sin(float(meshletID) * 12.9898) * 43758.5453);
    float g = fract(sin(float(meshletID) * 78.233 + 1.0) * 43758.5453);
    float b = fract(sin(float(meshletID) * 45.164 + 2.0) * 43758.5453);
    return vec3(r, g, b);
}

vec3 getLODColor(uint lod) {
    const vec3 colors[5] = vec3[5](
        vec3(0.2, 0.8, 0.2),   // LOD 0 - Green
        vec3(0.6, 0.9, 0.2),   // LOD 1 - Yellow-green
        vec3(0.9, 0.9, 0.2),   // LOD 2 - Yellow
        vec3(0.9, 0.5, 0.2),   // LOD 3 - Orange
        vec3(0.9, 0.2, 0.2)    // LOD 4 - Red
    );
    return colors[min(lod, 4u)];
}

vec3 getTriangleBarycentricColor(uint triangleIndex) {
    // Alternating colors for triangle visualization
    uint pattern = triangleIndex % 3;
    if (pattern == 0) return vec3(1.0, 0.3, 0.3);
    if (pattern == 1) return vec3(0.3, 1.0, 0.3);
    return vec3(0.3, 0.3, 1.0);
}
#endif

// ═══════════════════════════════════════════════════════════════════════════
// MAIN MESH SHADER
// ═══════════════════════════════════════════════════════════════════════════

void main() {
    uint laneID = gl_LocalInvocationID.x;
    uint workgroupID = gl_WorkGroupID.x;
    
    // ─────────────────────────────────────────────────────────────────────
    // PHASE 1: Early exit if no work
    // ─────────────────────────────────────────────────────────────────────
    
    if (workgroupID >= payload.taskCount) {
        SetMeshOutputsEXT(0, 0);
        return;
    }
    
    // ─────────────────────────────────────────────────────────────────────
    // PHASE 2: Load task and meshlet data
    // ─────────────────────────────────────────────────────────────────────
    
    MeshletTask task = payload.tasks[workgroupID];
    MeshletDesc meshlet = meshlets[task.meshletIndex];
    InstanceData instance = instances[task.instanceIndex];
    
    uint vertexCount = uint(meshlet.vertexCount);
    uint triangleCount = uint(meshlet.triangleCount);
    
    // Compute matrices
    mat4 modelMatrix = instance.modelMatrix;
    mat3 normalMatrix = computeNormalMatrix(modelMatrix);
    mat4 mvp = payload.viewProj * modelMatrix;
    
#if ENABLE_MOTION_VECTORS
    mat4 prevModelMatrix = instance.prevModelMatrix;
    mat4 prevMVP = camera.prevViewProj * prevModelMatrix;
#endif
    
    // Check meshlet flags
    bool isDoubleSided = (meshlet.flags & 1u) != 0u;
    bool hasAlphaTest = (meshlet.flags & 2u) != 0u;
    
    // Initialize shared memory counters
    if (laneID == 0) {
        s_visibleTriangleCount = 0;
        for (uint i = 0; i < (MAX_MESHLET_TRIANGLES / 32 + 1); i++) {
            s_triangleVisible[i] = 0;
        }
    }
    
    barrier();
    
    // ─────────────────────────────────────────────────────────────────────
    // PHASE 3: Vertex loading and transformation (collaborative)
    // ─────────────────────────────────────────────────────────────────────
    
    for (uint vIdx = laneID; vIdx < vertexCount; vIdx += WORKGROUP_SIZE) {
        // Fetch global vertex index
        uint globalVertexIndex = meshletVertices[meshlet.vertexOffset + vIdx];
        
        // Decode vertex
        vec3 localPosition;
        vec3 localNormal;
        vec4 localTangent;
        vec2 uv0, uv1;
        uvec4 boneIdx;
        vec4 boneWgt;
        
#if ENABLE_VERTEX_COMPRESSION
        VertexCompressed compVertex = vertices[globalVertexIndex];
        unpackVertex(compVertex, localPosition, localNormal, localTangent, 
                     uv0, uv1, boneIdx, boneWgt);
#else
        VertexFull fullVertex = vertices[globalVertexIndex];
        localPosition = fullVertex.position;
        localNormal = fullVertex.normal;
        localTangent = fullVertex.tangent;
        uv0 = fullVertex.uv0;
        uv1 = fullVertex.uv1;
        boneIdx = fullVertex.boneIndices;
        boneWgt = fullVertex.boneWeights;
#endif
        
        vec3 prevLocalPosition = localPosition;
        
        // Apply skeletal animation
#if ENABLE_SKINNING
        if ((instance.flags & 0x100u) != 0u) {  // Has skeleton
            applySkinning(instance.skeletonOffset, boneIdx, boneWgt,
                         localPosition, localNormal, localTangent.xyz,
                         prevLocalPosition);
        }
#endif
        
        // Apply morph targets
#if ENABLE_MORPH_TARGETS
        if ((instance.flags & 0x200u) != 0u) {  // Has morphs
            uint morphOffset = instance.customData & 0xFFFF;
            uint morphCount = (instance.customData >> 16) & 0xFF;
            applyMorphTargets(globalVertexIndex, morphOffset, morphCount,
                             localPosition, localNormal);
        }
#endif
        
        // Transform to world space
        vec3 worldPosition = (modelMatrix * vec4(localPosition, 1.0)).xyz;
        vec3 worldNormal = normalize(normalMatrix * localNormal);
        vec3 worldTangent = normalize(normalMatrix * localTangent.xyz);
        
        // Apply displacement
#if ENABLE_DISPLACEMENT
        worldPosition = applyDisplacement(worldPosition, worldNormal, uv0);
#endif
        
        // Re-orthogonalize tangent (Gram-Schmidt)
        worldTangent = normalize(worldTangent - dot(worldTangent, worldNormal) * worldNormal);
        
        // Transform to clip space
        vec4 clipPosition = mvp * vec4(localPosition, 1.0);
        
        // Store in shared memory
        s_clipPositions[vIdx] = clipPosition;
        s_worldPositions[vIdx] = worldPosition;
        
#if !SHADOW_PASS
        s_worldNormals[vIdx] = worldNormal;
        s_worldTangents[vIdx] = vec4(worldTangent, localTangent.w);
        s_texCoords[vIdx] = vec4(uv0, uv1);
#endif
        
#if ENABLE_MOTION_VECTORS
        vec4 prevClipPos = prevMVP * vec4(prevLocalPosition, 1.0);
        s_prevClipPositions[vIdx] = prevClipPos;
#endif
    }
    
    barrier();
    
    // ─────────────────────────────────────────────────────────────────────
    // PHASE 4: Triangle culling (collaborative)
    // ─────────────────────────────────────────────────────────────────────
    
#if ENABLE_MICRO_TRIANGLE_CULLING || ENABLE_BACKFACE_CULLING
    for (uint triIdx = laneID; triIdx < triangleCount; triIdx += WORKGROUP_SIZE) {
        // Load triangle indices
        uint triOffset = meshlet.triangleOffset + triIdx * 3;
        uint i0 = uint(meshletTriangles[triOffset + 0]);
        uint i1 = uint(meshletTriangles[triOffset + 1]);
        uint i2 = uint(meshletTriangles[triOffset + 2]);
        
        // Fetch clip positions
        vec4 p0 = s_clipPositions[i0];
        vec4 p1 = s_clipPositions[i1];
        vec4 p2 = s_clipPositions[i2];
        
        // Perform culling
        bool visible = !shouldCullTriangle(p0, p1, p2, isDoubleSided);
        
        // Store triangle visibility using ballot
        uvec4 ballot = subgroupBallot(visible);
        uint visibleCount = subgroupBallotBitCount(ballot);
        uint localIdx = subgroupBallotExclusiveBitCount(ballot);
        
        // First lane in subgroup reserves slots
        uint baseOffset = 0;
        if (subgroupElect()) {
            baseOffset = atomicAdd(s_visibleTriangleCount, visibleCount);
        }
        baseOffset = subgroupBroadcastFirst(baseOffset);
        
        // Store visible triangle
        if (visible) {
            uint outputIdx = baseOffset + localIdx;
            if (outputIdx < MAX_MESHLET_TRIANGLES) {
                s_triangleIndices[outputIdx] = triIdx;
                s_triangleData[outputIdx] = uvec3(i0, i1, i2);
            }
        }
    }
    
    barrier();
    
    uint finalTriangleCount = min(s_visibleTriangleCount, uint(MAX_MESHLET_TRIANGLES));
#else
    // No culling - all triangles visible
    for (uint triIdx = laneID; triIdx < triangleCount; triIdx += WORKGROUP_SIZE) {
        uint triOffset = meshlet.triangleOffset + triIdx * 3;
        s_triangleIndices[triIdx] = triIdx;
        s_triangleData[triIdx] = uvec3(
            uint(meshletTriangles[triOffset + 0]),
            uint(meshletTriangles[triOffset + 1]),
            uint(meshletTriangles[triOffset + 2])
        );
    }
    
    barrier();
    
    uint finalTriangleCount = triangleCount;
#endif
    
    // ─────────────────────────────────────────────────────────────────────
    // PHASE 5: Output mesh data
    // ─────────────────────────────────────────────────────────────────────
    
    // Set output counts
    SetMeshOutputsEXT(vertexCount, finalTriangleCount);
    
    // Output vertices
    for (uint vIdx = laneID; vIdx < vertexCount; vIdx += WORKGROUP_SIZE) {
        // Position
        gl_MeshVerticesEXT[vIdx].gl_Position = s_clipPositions[vIdx];
        
#if SHADOW_PASS
        // Minimal output for shadows
        vs_out[vIdx].linearDepth = s_clipPositions[vIdx].z / s_clipPositions[vIdx].w;
#elif VISIBILITY_BUFFER_MODE
        // Visibility buffer needs minimal data
        vs_out[vIdx].triangleID = task.meshletIndex * MAX_MESHLET_TRIANGLES + vIdx;
        vs_out[vIdx].materialIndex = task.materialIndex;
        vs_out[vIdx].barycentrics = vec2(0.0); // Computed in fragment
#else
        // Full vertex output
        vs_out[vIdx].worldPosition = s_worldPositions[vIdx];
        vs_out[vIdx].worldNormal = s_worldNormals[vIdx];
        vs_out[vIdx].worldTangent = s_worldTangents[vIdx];
        vs_out[vIdx].texCoord0 = s_texCoords[vIdx].xy;
        vs_out[vIdx].texCoord1 = s_texCoords[vIdx].zw;
        
    #if ENABLE_MOTION_VECTORS
        vs_out[vIdx].currentClipPos = s_clipPositions[vIdx];
        vs_out[vIdx].previousClipPos = s_prevClipPositions[vIdx];
    #endif
        
        vs_out[vIdx].materialIndex = task.materialIndex;
        vs_out[vIdx].instanceID = task.instanceIndex;
        
    #if DEBUG_MODE
        vs_out[vIdx].meshletID = task.meshletIndex;
        vs_out[vIdx].triangleIndex = 0; // Set per-triangle
        vs_out[vIdx].debugColor = getMeshletColor(task.meshletIndex);
    #endif
#endif
    }
    
    // Output triangles
    for (uint triIdx = laneID; triIdx < finalTriangleCount; triIdx += WORKGROUP_SIZE) {
        uvec3 indices = s_triangleData[triIdx];
        gl_PrimitiveTriangleIndicesEXT[triIdx] = indices;
        
        // Per-primitive data
        uint primFlags = 0;
        
#if ENABLE_VARIABLE_RATE_SHADING
        vec4 p0 = s_clipPositions[indices.x];
        vec4 p1 = s_clipPositions[indices.y];
        vec4 p2 = s_clipPositions[indices.z];
        
    #if !SHADOW_PASS
        vec3 n0 = s_worldNormals[indices.x];
        vec3 n1 = s_worldNormals[indices.y];
        vec3 n2 = s_worldNormals[indices.z];
        prim_out[triIdx].shadingRate = calculateShadingRate(p0, p1, p2, n0, n1, n2);
    #else
        prim_out[triIdx].shadingRate = 0; // Full rate for shadows
    #endif
#endif
        
        prim_out[triIdx].primitiveFlags = primFlags;
    }
    
    // ─────────────────────────────────────────────────────────────────────
    // PHASE 6: Multi-view rendering (VR)
    // ─────────────────────────────────────────────────────────────────────
    
#if ENABLE_MULTI_VIEW
    // For VR: duplicate vertices for both eyes
    // This is a simplified version - full implementation would need
    // separate vertex buffers or geometry amplification
    
    // Alternative: Use gl_ViewIndex in fragment shader and 
    // pass both viewProj matrices to reconstruct per-eye
#endif
}
