#version 460
#extension GL_EXT_nonuniform_qualifier : require

layout(location = 0) in VertexOutput {
    vec3 worldPosition;
    vec3 worldNormal;
    vec4 worldTangent;
    vec2 texCoord0;
    vec2 texCoord1;
    vec4 currentClipPos;
    vec4 prevClipPos;
    flat uint materialIndex;
    flat uint instanceID;
    flat uint meshletID;
} fs_in;

layout(location = 10) perprimitiveEXT in PerPrimitiveOutput {
    flat uint primitiveID;
} prim_in;

// GBuffer outputs
layout(location = 0) out vec4 outAlbedo;
layout(location = 1) out vec4 outNormal;
layout(location = 2) out vec4 outMaterial;    // Metallic, Roughness, AO, flags
layout(location = 3) out vec2 outMotionVector;
layout(location = 4) out uint outEntityID;    // For picking/selection

// Material data
struct Material {
    vec4 baseColorFactor;
    float metallicFactor;
    float roughnessFactor;
    float normalScale;
    float occlusionStrength;
    uint albedoTexture;
    uint normalTexture;
    uint ormTexture;         // Occlusion, Roughness, Metallic
    uint emissiveTexture;
};

layout(std430, binding = 11) readonly buffer MaterialBuffer {
    Material materials[];
};

layout(binding = 12) uniform sampler2D textures[];

// Debug visualization
#if DEBUG_MODE
vec3 meshletDebugColor(uint meshletID) {
    float r = fract(sin(float(meshletID) * 12.9898) * 43758.5453);
    float g = fract(sin(float(meshletID) * 78.233) * 43758.5453);
    float b = fract(sin(float(meshletID) * 45.164) * 43758.5453);
    return vec3(r, g, b);
}
#endif

void main() {
    Material mat = materials[fs_in.materialIndex];
    
    // Sample textures
    vec4 albedo = texture(textures[nonuniformEXT(mat.albedoTexture)], fs_in.texCoord0);
    albedo *= mat.baseColorFactor;
    
    // Normal mapping
    vec3 N = normalize(fs_in.worldNormal);
    vec3 T = normalize(fs_in.worldTangent.xyz);
    vec3 B = cross(N, T) * fs_in.worldTangent.w;
    mat3 TBN = mat3(T, B, N);
    
    vec3 normalMap = texture(textures[nonuniformEXT(mat.normalTexture)], fs_in.texCoord0).rgb;
    normalMap = normalMap * 2.0 - 1.0;
    normalMap.xy *= mat.normalScale;
    vec3 worldNormal = normalize(TBN * normalMap);
    
    // ORM texture
    vec3 orm = texture(textures[nonuniformEXT(mat.ormTexture)], fs_in.texCoord0).rgb;
    float ao = orm.r * mat.occlusionStrength;
    float roughness = orm.g * mat.roughnessFactor;
    float metallic = orm.b * mat.metallicFactor;
    
    // Motion vectors
    vec2 currentNDC = fs_in.currentClipPos.xy / fs_in.currentClipPos.w;
    vec2 prevNDC = fs_in.prevClipPos.xy / fs_in.prevClipPos.w;
    vec2 motionVector = (currentNDC - prevNDC) * 0.5;
    
    // Output
    outAlbedo = albedo;
    outNormal = vec4(worldNormal * 0.5 + 0.5, roughness);
    outMaterial = vec4(metallic, roughness, ao, 0.0);
    outMotionVector = motionVector;
    outEntityID = fs_in.instanceID;
    
    #if DEBUG_MODE
        outAlbedo.rgb = meshletDebugColor(fs_in.meshletID);
    #endif
}
