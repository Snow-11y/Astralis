#version 460
#extension GL_EXT_mesh_shader : require
#extension GL_EXT_shader_explicit_arithmetic_types : require
#extension GL_EXT_shader_16bit_storage : require
#extension GL_EXT_shader_8bit_storage : require
#extension GL_KHR_shader_subgroup_ballot : require

// ═══════════════════════════════════════════════════════════════════════════
// CONFIGURATION
// ═══════════════════════════════════════════════════════════════════════════

// Max vertices and primitives per meshlet (hardware limits apply)
#define MAX_VERTICES 64
#define MAX_TRIANGLES 124  // 126 for some hardware, check VkPhysicalDeviceMeshShaderPropertiesEXT

layout(local_size_x = 32) in;
layout(triangles, max_vertices = MAX_VERTICES, max_primitives = MAX_TRIANGLES) out;

// ═══════════════════════════════════════════════════════════════════════════
// STRUCTURES (Must match Task Shader)
// ═══════════════════════════════════════════════════════════════════════════

struct MeshletDesc {
    vec3 boundingSphereCenter;
    float boundingSphereRadius;
    vec3 coneAxis;
    float coneCutoff;
    vec3 bboxMin;
    vec3 bboxMax;
    uint vertexOffset;
    uint triangleOffset;
    uint8_t vertexCount;
    uint8_t triangleCount;
    uint8_t lodLevel;
    uint8_t flags;
    uint parentClusterIndex;
    uint materialIndex;
};

struct MeshletTask {
    uint meshletIndex;
    uint instanceIndex;
    uint lodLevel;
    uint materialIndex;
};

struct TaskPayload {
    MeshletTask tasks[32];
    uint taskCount;
    mat4 viewProj;
    vec3 cameraPosition;
    uint frameIndex;
};

taskPayloadSharedEXT TaskPayload payload;

// Vertex data (compressed)
struct VertexCompressed {
    uint16_t posX, posY, posZ, posW;  // Position + tangent sign
    uint packedNormalTangent;          // Octahedron encoded
    uint16_t uv0X, uv0Y;
    uint16_t uv1X, uv1Y;
    uint8_t boneIndices[4];
    uint8_t boneWeights[4];
};

// Instance data
struct InstanceData {
    mat4 modelMatrix;
    mat4 prevModelMatrix;
    vec4 boundingSphere;
    uint meshLODGroupIndex;
    uint flags;
    uint customData;
    float lodBias;
};

// ═══════════════════════════════════════════════════════════════════════════
// BINDINGS
// ═══════════════════════════════════════════════════════════════════════════

layout(std430, binding = 0) readonly buffer MeshletBuffer {
    MeshletDesc meshlets[];
};

layout(std430, binding = 2) readonly buffer InstanceBuffer {
    InstanceData instances[];
};

// Global vertex buffer
layout(std430, binding = 8) readonly buffer VertexBuffer {
    VertexCompressed vertices[];
};

// Meshlet vertex indices (local to global mapping)
layout(std430, binding = 9) readonly buffer MeshletVertexBuffer {
    uint meshletVertices[];  // 32-bit indices
};

// Meshlet triangle indices (local vertex indices, 3 bytes per triangle)
layout(std430, binding = 10) readonly buffer MeshletTriangleBuffer {
    uint8_t meshletTriangles[];  // Packed triangles
};

// ═══════════════════════════════════════════════════════════════════════════
// OUTPUTS
// ═══════════════════════════════════════════════════════════════════════════

layout(location = 0) out VertexOutput {
    vec3 worldPosition;
    vec3 worldNormal;
    vec4 worldTangent;
    vec2 texCoord0;
    vec2 texCoord1;
    vec4 currentClipPos;
    vec4 prevClipPos;
    flat uint materialIndex;
    flat uint instanceID;
    flat uint meshletID;  // For debug visualization
} vs_out[];

// Per-primitive outputs
layout(location = 10) perprimitiveEXT out PerPrimitiveOutput {
    flat uint primitiveID;
} prim_out[];

// ═══════════════════════════════════════════════════════════════════════════
// SHARED MEMORY
// ═══════════════════════════════════════════════════════════════════════════

shared vec3 s_positions[MAX_VERTICES];
shared vec3 s_normals[MAX_VERTICES];
shared vec4 s_tangents[MAX_VERTICES];
shared vec2 s_uv0[MAX_VERTICES];
shared vec2 s_uv1[MAX_VERTICES];

// ═══════════════════════════════════════════════════════════════════════════
// UTILITY FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

vec3 octDecode(vec2 f) {
    f = f * 2.0 - 1.0;
    vec3 n = vec3(f.x, f.y, 1.0 - abs(f.x) - abs(f.y));
    float t = max(-n.z, 0.0);
    n.xy += mix(vec2(t), vec2(-t), greaterThanEqual(n.xy, vec2(0.0)));
    return normalize(n);
}

float halfToFloat(uint16_t h) {
    return unpackHalf2x16(uint(h)).x;
}

void decodeVertex(VertexCompressed v, out vec3 pos, out vec3 normal, 
                  out vec4 tangent, out vec2 uv0, out vec2 uv1) {
    pos = vec3(halfToFloat(v.posX), halfToFloat(v.posY), halfToFloat(v.posZ));
    
    float tangentSign = halfToFloat(v.posW) >= 0.0 ? 1.0 : -1.0;
    
    float nx = float(int8_t(v.packedNormalTangent & 0xFF)) / 127.0;
    float ny = float(int8_t((v.packedNormalTangent >> 8) & 0xFF)) / 127.0;
    float tx = float(int8_t((v.packedNormalTangent >> 16) & 0xFF)) / 127.0;
    float ty = float(int8_t((v.packedNormalTangent >> 24) & 0xFF)) / 127.0;
    
    normal = octDecode(vec2(nx, ny) * 0.5 + 0.5);
    tangent = vec4(octDecode(vec2(tx, ty) * 0.5 + 0.5), tangentSign);
    
    uv0 = vec2(halfToFloat(v.uv0X), halfToFloat(v.uv0Y));
    uv1 = vec2(halfToFloat(v.uv1X), halfToFloat(v.uv1Y));
}

mat3 calcNormalMatrix(mat4 m) {
    // For uniform scale
    return mat3(m);
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN MESH SHADER
// ═══════════════════════════════════════════════════════════════════════════

void main() {
    uint lID = gl_LocalInvocationID.x;
    uint wgID = gl_WorkGroupID.x;
    
    // ─────────────────────────────────────────────────────────────────────
    // STEP 1: Fetch task data
    // ─────────────────────────────────────────────────────────────────────
    
    if (wgID >= payload.taskCount) {
        // No work for this workgroup
        SetMeshOutputsEXT(0, 0);
        return;
    }
    
    MeshletTask task = payload.tasks[wgID];
    MeshletDesc meshlet = meshlets[task.meshletIndex];
    InstanceData instance = instances[task.instanceIndex];
    
    uint vertexCount = uint(meshlet.vertexCount);
    uint triangleCount = uint(meshlet.triangleCount);
    
    mat4 modelMatrix = instance.modelMatrix;
    mat4 mvp = payload.viewProj * modelMatrix;
    mat3 normalMatrix = calcNormalMatrix(modelMatrix);
    
    // ─────────────────────────────────────────────────────────────────────
    // STEP 2: Cooperative vertex loading and processing
    // ─────────────────────────────────────────────────────────────────────
    
    // Each thread loads multiple vertices if needed
    for (uint i = lID; i < vertexCount; i += 32) {
        // Fetch global vertex index for this meshlet vertex
        uint globalVertexIdx = meshletVertices[meshlet.vertexOffset + i];
        
        // Fetch and decode vertex
        VertexCompressed compressedVertex = vertices[globalVertexIdx];
        vec3 localPos, localNormal;
        vec4 localTangent;
        vec2 uv0, uv1;
        decodeVertex(compressedVertex, localPos, localNormal, localTangent, uv0, uv1);
        
        // Transform to world space
        s_positions[i] = (modelMatrix * vec4(localPos, 1.0)).xyz;
        s_normals[i] = normalize(normalMatrix * localNormal);
        s_tangents[i] = vec4(normalize(normalMatrix * localTangent.xyz), localTangent.w);
        s_uv0[i] = uv0;
        s_uv1[i] = uv1;
    }
    
    barrier();
    
    // ─────────────────────────────────────────────────────────────────────
    // STEP 3: Set mesh outputs
    // ─────────────────────────────────────────────────────────────────────
    
    SetMeshOutputsEXT(vertexCount, triangleCount);
    
    // ─────────────────────────────────────────────────────────────────────
    // STEP 4: Write vertices
    // ─────────────────────────────────────────────────────────────────────
    
    for (uint i = lID; i < vertexCount; i += 32) {
        vec3 worldPos = s_positions[i];
        vec4 clipPos = payload.viewProj * vec4(worldPos, 1.0);
        
        gl_MeshVerticesEXT[i].gl_Position = clipPos;
        
        vs_out[i].worldPosition = worldPos;
        vs_out[i].worldNormal = s_normals[i];
        vs_out[i].worldTangent = s_tangents[i];
        vs_out[i].texCoord0 = s_uv0[i];
        vs_out[i].texCoord1 = s_uv1[i];
        vs_out[i].currentClipPos = clipPos;
        vs_out[i].prevClipPos = instance.prevModelMatrix * vec4(s_positions[i], 1.0);
        vs_out[i].prevClipPos = payload.viewProj * vs_out[i].prevClipPos; // Simplified
        vs_out[i].materialIndex = task.materialIndex;
        vs_out[i].instanceID = task.instanceIndex;
        vs_out[i].meshletID = task.meshletIndex;
    }
    
    // ─────────────────────────────────────────────────────────────────────
    // STEP 5: Write triangles
    // ─────────────────────────────────────────────────────────────────────
    
    for (uint i = lID; i < triangleCount; i += 32) {
        uint triOffset = meshlet.triangleOffset + i * 3;
        
        // Fetch 3 vertex indices (local to meshlet)
        uint i0 = uint(meshletTriangles[triOffset + 0]);
        uint i1 = uint(meshletTriangles[triOffset + 1]);
        uint i2 = uint(meshletTriangles[triOffset + 2]);
        
        gl_PrimitiveTriangleIndicesEXT[i] = uvec3(i0, i1, i2);
        
        prim_out[i].primitiveID = i;
    }
}
