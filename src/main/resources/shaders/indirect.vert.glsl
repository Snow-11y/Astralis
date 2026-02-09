#version 460
#extension GL_EXT_shader_16bit_storage : require
#extension GL_EXT_shader_8bit_storage : require
#extension GL_ARB_shader_draw_parameters : require

// ═══════════════════════════════════════════════════════════════════════════
// SHADER VARIANTS (Compile-time flags)
// ═══════════════════════════════════════════════════════════════════════════

#ifndef SHADOW_PASS
#define SHADOW_PASS 0
#endif

#ifndef ENABLE_SKINNING
#define ENABLE_SKINNING 0
#endif

#ifndef ENABLE_MOTION_VECTORS
#define ENABLE_MOTION_VECTORS 1
#endif

#ifndef ENABLE_VERTEX_COMPRESSION
#define ENABLE_VERTEX_COMPRESSION 1
#endif

#ifndef DEBUG_MODE
#define DEBUG_MODE 0
#endif

// ═══════════════════════════════════════════════════════════════════════════
// OUTPUTS
// ═══════════════════════════════════════════════════════════════════════════

layout(location = 0) out VertexOutput {
    vec3 worldPosition;
    vec3 worldNormal;
    vec4 worldTangent;      // xyz = tangent, w = bitangent sign
    vec2 texCoord0;
    vec2 texCoord1;
    
#if ENABLE_MOTION_VECTORS
    vec4 currentClipPos;
    vec4 previousClipPos;
#endif
    
    flat uint materialIndex;
    flat uint instanceID;
    
#if DEBUG_MODE
    flat uint lodLevel;
    flat vec3 debugColor;
#endif
} vs_out;

// ═══════════════════════════════════════════════════════════════════════════
// STRUCTS - Must match compute shader
// ═══════════════════════════════════════════════════════════════════════════

// Per-instance ECS data (must match compute shader exactly)
struct InstanceData {
    mat4 modelMatrix;
    vec4 boundingSphere;     // xyz = center, w = radius
    uint meshTypeIndex;
    uint flags;
    uint customData;         // Animation index, etc.
    float sortKey;
};

// Previous frame transform for motion vectors
struct PreviousTransform {
    mat4 prevModelMatrix;
};

// Compacted visibility output from compute shader
struct VisibleInstance {
    uint instanceID;
    uint meshTypeAndLOD;     // Packed: meshType (24 bits) | LOD (8 bits)
    float depth;
    uint batchID;
};

// Mesh metadata for vertex buffer offsets
struct MeshInfo {
    uint vertexOffset;       // Base vertex in global vertex buffer
    uint indexOffset;        // Base index in global index buffer
    uint vertexCount;
    uint indexCount;
    uint materialIndex;
    uint flags;
    vec2 uvScale;
};

// Compressed vertex format (32 bytes)
struct VertexCompressed {
    // Position: 3x float16 + padding = 8 bytes
    uint16_t posX, posY, posZ, posW;
    
    // Normal: oct-encoded in 2x snorm8 = 2 bytes
    // Tangent: oct-encoded in 2x snorm8 = 2 bytes  
    // Tangent sign: 1 byte, padding: 1 byte
    uint packedNormalTangent;  // 4 bytes total
    
    // UV0: 2x float16 = 4 bytes
    uint16_t uv0X, uv0Y;
    
    // UV1 / Lightmap UV: 2x float16 = 4 bytes
    uint16_t uv1X, uv1Y;
    
    // Bone indices: 4x uint8 = 4 bytes
    uint8_t boneIndices[4];
    
    // Bone weights: 4x uint8 (normalized) = 4 bytes
    uint8_t boneWeights[4];
};

// Uncompressed vertex format (48 bytes, for comparison)
struct VertexFull {
    vec3 position;
    vec3 normal;
    vec4 tangent;
    vec2 uv0;
    vec2 uv1;
    uvec4 boneIndices;
    vec4 boneWeights;
};

// Bone/Joint for skeletal animation
struct BoneTransform {
    mat4 boneMatrix;
    mat4 prevBoneMatrix;     // For motion vectors
};

// ═══════════════════════════════════════════════════════════════════════════
// BINDINGS
// ═══════════════════════════════════════════════════════════════════════════

// Instance data (shared with compute shader)
layout(std430, binding = 0) readonly buffer InstanceBuffer {
    InstanceData instances[];
};

// Previous frame transforms
layout(std430, binding = 1) readonly buffer PreviousTransformBuffer {
    PreviousTransform prevTransforms[];
};

// Compacted visibility list (from compute shader)
layout(std430, binding = 2) readonly buffer VisibilityBuffer {
    VisibleInstance visibleInstances[];
};

// Mesh info for each mesh type
layout(std430, binding = 3) readonly buffer MeshInfoBuffer {
    MeshInfo meshInfos[];
};

// Vertex data (compressed or full)
#if ENABLE_VERTEX_COMPRESSION
layout(std430, binding = 4) readonly buffer VertexBuffer {
    VertexCompressed vertices[];
};
#else
layout(std430, binding = 4) readonly buffer VertexBuffer {
    VertexFull vertices[];
};
#endif

// Skeleton data for skinned meshes
#if ENABLE_SKINNING
layout(std430, binding = 5) readonly buffer BoneBuffer {
    BoneTransform bones[];
};

// Per-instance skeleton offset
layout(std430, binding = 6) readonly buffer SkeletonOffsetBuffer {
    uint skeletonOffsets[]; // Index into bone buffer
};
#endif

// Camera uniforms
layout(std140, binding = 7) uniform CameraUBO {
    mat4 view;
    mat4 proj;
    mat4 viewProj;
    mat4 prevViewProj;
    mat4 invView;
    mat4 invProj;
    
    vec4 cameraPosition;
    vec4 cameraForward;
    
    vec2 viewportSize;
    vec2 invViewportSize;
    
    float nearPlane;
    float farPlane;
    float time;
    float deltaTime;
    
    uint frameIndex;
    uint _pad0, _pad1, _pad2;
} camera;

// Per-draw push constants (optional, for multi-draw optimization)
layout(push_constant) uniform PushConstants {
    uint baseVisibilityIndex;  // Offset into visibility buffer
    uint meshTypeOverride;     // For non-indirect draws
    uint flags;
    uint _pad;
} push;

// ═══════════════════════════════════════════════════════════════════════════
// UTILITY FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

// Decode octahedron-encoded normal (2 bytes -> vec3)
vec3 octDecode(vec2 f) {
    f = f * 2.0 - 1.0;
    
    // https://twitter.com/Stubbesaurus/status/937994790553227264
    vec3 n = vec3(f.x, f.y, 1.0 - abs(f.x) - abs(f.y));
    float t = max(-n.z, 0.0);
    n.xy += mix(vec2(t), vec2(-t), greaterThanEqual(n.xy, vec2(0.0)));
    return normalize(n);
}

// Unpack compressed normal and tangent
void unpackNormalTangent(uint packed, out vec3 normal, out vec4 tangent) {
    // Extract bytes
    float nx = float(int8_t(packed & 0xFF)) / 127.0;
    float ny = float(int8_t((packed >> 8) & 0xFF)) / 127.0;
    float tx = float(int8_t((packed >> 16) & 0xFF)) / 127.0;
    float ty = float(int8_t((packed >> 24) & 0xFF)) / 127.0;
    
    // Decode from octahedron
    normal = octDecode(vec2(nx, ny) * 0.5 + 0.5);
    vec3 tang = octDecode(vec2(tx, ty) * 0.5 + 0.5);
    
    // Tangent sign is stored in the W position data
    tangent = vec4(tang, 1.0); // Sign will be set separately
}

// Convert half-float to float
float halfToFloat(uint16_t h) {
    return unpackHalf2x16(uint(h)).x;
}

// Fetch and decode compressed vertex
void fetchCompressedVertex(uint vertexIndex, out vec3 position, out vec3 normal, 
                           out vec4 tangent, out vec2 uv0, out vec2 uv1,
                           out uvec4 boneIdx, out vec4 boneWgt) {
    VertexCompressed v = vertices[vertexIndex];
    
    // Position from half-floats
    position = vec3(
        halfToFloat(v.posX),
        halfToFloat(v.posY),
        halfToFloat(v.posZ)
    );
    
    // Tangent sign stored in posW
    float tangentSign = halfToFloat(v.posW) >= 0.0 ? 1.0 : -1.0;
    
    // Normal and tangent from octahedron encoding
    unpackNormalTangent(v.packedNormalTangent, normal, tangent);
    tangent.w = tangentSign;
    
    // UVs from half-floats
    uv0 = vec2(halfToFloat(v.uv0X), halfToFloat(v.uv0Y));
    uv1 = vec2(halfToFloat(v.uv1X), halfToFloat(v.uv1Y));
    
    // Bone data
    boneIdx = uvec4(v.boneIndices[0], v.boneIndices[1], 
                    v.boneIndices[2], v.boneIndices[3]);
    boneWgt = vec4(v.boneWeights[0], v.boneWeights[1],
                   v.boneWeights[2], v.boneWeights[3]) / 255.0;
}

// Fetch full vertex
void fetchFullVertex(uint vertexIndex, out vec3 position, out vec3 normal,
                     out vec4 tangent, out vec2 uv0, out vec2 uv1,
                     out uvec4 boneIdx, out vec4 boneWgt) {
    VertexFull v = vertices[vertexIndex];
    
    position = v.position;
    normal = v.normal;
    tangent = v.tangent;
    uv0 = v.uv0;
    uv1 = v.uv1;
    boneIdx = v.boneIndices;
    boneWgt = v.boneWeights;
}

// Calculate proper normal matrix (inverse transpose of upper 3x3)
mat3 calcNormalMatrix(mat4 modelMatrix) {
    // For uniform scale, we can just use the upper 3x3
    // For non-uniform scale, we need inverse transpose
    mat3 m = mat3(modelMatrix);
    
    // Fast inverse for orthonormal + uniform scale
    // If non-uniform scale is common, use the full inverse transpose
    #if 1
        // Assumes mostly uniform scale - cheaper
        float invScale = 1.0 / length(m[0]);
        return m * invScale;
    #else
        // Full inverse transpose - handles all cases
        return transpose(inverse(m));
    #endif
}

// ═══════════════════════════════════════════════════════════════════════════
// SKINNING
// ═══════════════════════════════════════════════════════════════════════════

#if ENABLE_SKINNING
void applySkinning(uint skeletonOffset, uvec4 boneIdx, vec4 boneWgt,
                   inout vec3 position, inout vec3 normal, inout vec3 tangent,
                   out vec3 prevPosition) {
    mat4 skinMatrix = mat4(0.0);
    mat4 prevSkinMatrix = mat4(0.0);
    
    // Accumulate bone transforms
    for (int i = 0; i < 4; i++) {
        if (boneWgt[i] > 0.0) {
            uint boneIndex = skeletonOffset + boneIdx[i];
            skinMatrix += bones[boneIndex].boneMatrix * boneWgt[i];
            prevSkinMatrix += bones[boneIndex].prevBoneMatrix * boneWgt[i];
        }
    }
    
    // Apply skinning
    position = (skinMatrix * vec4(position, 1.0)).xyz;
    prevPosition = (prevSkinMatrix * vec4(position, 1.0)).xyz;
    
    // Transform normals (assuming orthonormal bone matrices)
    mat3 skinMatrix3 = mat3(skinMatrix);
    normal = normalize(skinMatrix3 * normal);
    tangent = normalize(skinMatrix3 * tangent);
}
#endif

// ═══════════════════════════════════════════════════════════════════════════
// PROCEDURAL ANIMATION
// ═══════════════════════════════════════════════════════════════════════════

// Wind animation for vegetation
vec3 applyWind(vec3 worldPos, vec3 localPos, float windStrength, float windFreq) {
    float windTime = camera.time * windFreq;
    
    // Height-based influence (higher = more movement)
    float heightFactor = saturate(localPos.y / 2.0);
    
    // Noise-based wind
    float windX = sin(windTime + worldPos.x * 0.5) * cos(windTime * 0.7 + worldPos.z * 0.3);
    float windZ = cos(windTime * 0.8 + worldPos.z * 0.5) * sin(windTime * 0.6 + worldPos.x * 0.4);
    
    vec3 windOffset = vec3(windX, 0.0, windZ) * windStrength * heightFactor * heightFactor;
    
    return worldPos + windOffset;
}

// ═══════════════════════════════════════════════════════════════════════════
// DEBUG HELPERS
// ═══════════════════════════════════════════════════════════════════════════

#if DEBUG_MODE
vec3 getLODColor(uint lod) {
    const vec3 lodColors[5] = vec3[5](
        vec3(0.0, 1.0, 0.0),   // LOD 0 - Green (highest detail)
        vec3(0.5, 1.0, 0.0),   // LOD 1 - Yellow-green
        vec3(1.0, 1.0, 0.0),   // LOD 2 - Yellow
        vec3(1.0, 0.5, 0.0),   // LOD 3 - Orange
        vec3(1.0, 0.0, 0.0)    // LOD 4 - Red (lowest detail)
    );
    return lodColors[min(lod, 4u)];
}

vec3 getInstanceColor(uint instanceID) {
    // Generate pseudo-random color from instance ID
    float r = fract(sin(float(instanceID) * 12.9898) * 43758.5453);
    float g = fract(sin(float(instanceID) * 78.233) * 43758.5453);
    float b = fract(sin(float(instanceID) * 45.164) * 43758.5453);
    return vec3(r, g, b);
}
#endif

// ═══════════════════════════════════════════════════════════════════════════
// MAIN
// ═══════════════════════════════════════════════════════════════════════════

void main() {
    // ─────────────────────────────────────────────────────────────────────
    // STEP 1: Resolve indirection
    // ─────────────────────────────────────────────────────────────────────
    
    // gl_DrawID gives us which draw call (for multi-draw indirect)
    // gl_InstanceIndex is the instance within this draw
    // gl_BaseInstance was set by the indirect command
    
    uint visibilityIndex = push.baseVisibilityIndex + gl_InstanceIndex;
    VisibleInstance visInst = visibleInstances[visibilityIndex];
    
    uint entityID = visInst.instanceID;
    uint meshType = visInst.meshTypeAndLOD & 0x00FFFFFFu;
    uint lodLevel = visInst.meshTypeAndLOD >> 24;
    
    // ─────────────────────────────────────────────────────────────────────
    // STEP 2: Fetch instance and mesh data
    // ─────────────────────────────────────────────────────────────────────
    
    InstanceData instance = instances[entityID];
    MeshInfo mesh = meshInfos[meshType];
    
    mat4 modelMatrix = instance.modelMatrix;
    mat3 normalMatrix = calcNormalMatrix(modelMatrix);
    
    #if ENABLE_MOTION_VECTORS
        mat4 prevModelMatrix = prevTransforms[entityID].prevModelMatrix;
    #endif
    
    // ─────────────────────────────────────────────────────────────────────
    // STEP 3: Fetch and decode vertex data
    // ─────────────────────────────────────────────────────────────────────
    
    uint globalVertexIndex = mesh.vertexOffset + gl_VertexIndex;
    
    vec3 localPosition;
    vec3 localNormal;
    vec4 localTangent;
    vec2 uv0, uv1;
    uvec4 boneIndices;
    vec4 boneWeights;
    
    #if ENABLE_VERTEX_COMPRESSION
        fetchCompressedVertex(globalVertexIndex, localPosition, localNormal,
                              localTangent, uv0, uv1, boneIndices, boneWeights);
    #else
        fetchFullVertex(globalVertexIndex, localPosition, localNormal,
                        localTangent, uv0, uv1, boneIndices, boneWeights);
    #endif
    
    // Apply UV scale from mesh info
    uv0 *= mesh.uvScale;
    
    // ─────────────────────────────────────────────────────────────────────
    // STEP 4: Skeletal animation (optional)
    // ─────────────────────────────────────────────────────────────────────
    
    vec3 skinnedPosition = localPosition;
    vec3 skinnedNormal = localNormal;
    vec3 skinnedTangent = localTangent.xyz;
    vec3 prevSkinnedPosition = localPosition;
    
    #if ENABLE_SKINNING
        // Check if this instance uses skinning
        if ((instance.flags & 0x100u) != 0u) { // INSTANCE_FLAG_SKINNED
            uint skeletonOffset = skeletonOffsets[entityID];
            applySkinning(skeletonOffset, boneIndices, boneWeights,
                          skinnedPosition, skinnedNormal, skinnedTangent,
                          prevSkinnedPosition);
        }
    #endif
    
    // ─────────────────────────────────────────────────────────────────────
    // STEP 5: World-space transformation
    // ─────────────────────────────────────────────────────────────────────
    
    vec4 worldPosition4 = modelMatrix * vec4(skinnedPosition, 1.0);
    vec3 worldPosition = worldPosition4.xyz;
    
    vec3 worldNormal = normalize(normalMatrix * skinnedNormal);
    vec3 worldTangent = normalize(normalMatrix * skinnedTangent);
    
    // Re-orthogonalize tangent (Gram-Schmidt)
    worldTangent = normalize(worldTangent - dot(worldTangent, worldNormal) * worldNormal);
    
    // ─────────────────────────────────────────────────────────────────────
    // STEP 6: Procedural animation (vegetation, flags, etc.)
    // ─────────────────────────────────────────────────────────────────────
    
    // Check for vegetation/wind flag
    if ((instance.flags & 0x200u) != 0u) { // INSTANCE_FLAG_WIND_AFFECTED
        float windStrength = 0.3;
        float windFreq = 2.0;
        worldPosition = applyWind(worldPosition, localPosition, windStrength, windFreq);
    }
    
    // ─────────────────────────────────────────────────────────────────────
    // STEP 7: Clip-space transformation
    // ─────────────────────────────────────────────────────────────────────
    
    vec4 clipPosition = camera.viewProj * vec4(worldPosition, 1.0);
    gl_Position = clipPosition;
    
    // ─────────────────────────────────────────────────────────────────────
    // STEP 8: Motion vectors (for TAA / motion blur)
    // ─────────────────────────────────────────────────────────────────────
    
    #if ENABLE_MOTION_VECTORS
        vec4 prevWorldPos4 = prevModelMatrix * vec4(prevSkinnedPosition, 1.0);
        vec4 prevClipPos = camera.prevViewProj * prevWorldPos4;
        
        vs_out.currentClipPos = clipPosition;
        vs_out.previousClipPos = prevClipPos;
    #endif
    
    // ─────────────────────────────────────────────────────────────────────
    // STEP 9: Output interpolants
    // ─────────────────────────────────────────────────────────────────────
    
    vs_out.worldPosition = worldPosition;
    vs_out.worldNormal = worldNormal;
    vs_out.worldTangent = vec4(worldTangent, localTangent.w); // Preserve bitangent sign
    vs_out.texCoord0 = uv0;
    vs_out.texCoord1 = uv1;
    vs_out.materialIndex = mesh.materialIndex;
    vs_out.instanceID = entityID;
    
    #if DEBUG_MODE
        vs_out.lodLevel = lodLevel;
        vs_out.debugColor = getLODColor(lodLevel);
    #endif
    
    // ─────────────────────────────────────────────────────────────────────
    // Shadow pass optimization: minimize outputs
    // ─────────────────────────────────────────────────────────────────────
    
    #if SHADOW_PASS
        // Only position matters for shadow maps
        // Could also write linear depth for VSM/ESM
    #endif
}
