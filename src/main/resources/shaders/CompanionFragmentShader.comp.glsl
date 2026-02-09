#version 460
#extension GL_EXT_nonuniform_qualifier : require

layout(location = 0) in VertexOutput {
    vec3 worldPosition;
    vec3 worldNormal;
    vec4 worldTangent;
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
} fs_in;

// Outputs
layout(location = 0) out vec4 outColor;
layout(location = 1) out vec4 outNormal;       // RGB = normal, A = roughness
layout(location = 2) out vec2 outMotionVector;

// Material data
struct MaterialData {
    vec4 baseColorFactor;
    vec4 emissiveFactor;
    float metallicFactor;
    float roughnessFactor;
    float normalScale;
    float occlusionStrength;
    
    uint albedoTexture;
    uint normalTexture;
    uint metallicRoughnessTexture;
    uint occlusionTexture;
    uint emissiveTexture;
    uint _pad0, _pad1, _pad2;
};

layout(std430, binding = 8) readonly buffer MaterialBuffer {
    MaterialData materials[];
};

// Bindless textures
layout(binding = 9) uniform sampler2D textures[];

void main() {
    MaterialData mat = materials[fs_in.materialIndex];
    
    // Sample textures (bindless)
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
    
    // PBR parameters
    vec4 metallicRoughness = texture(textures[nonuniformEXT(mat.metallicRoughnessTexture)], fs_in.texCoord0);
    float metallic = metallicRoughness.b * mat.metallicFactor;
    float roughness = metallicRoughness.g * mat.roughnessFactor;
    
    // Output
    outColor = albedo;
    outNormal = vec4(worldNormal * 0.5 + 0.5, roughness);
    
    // Motion vectors
    #if ENABLE_MOTION_VECTORS
        vec2 currentNDC = fs_in.currentClipPos.xy / fs_in.currentClipPos.w;
        vec2 prevNDC = fs_in.previousClipPos.xy / fs_in.previousClipPos.w;
        outMotionVector = (currentNDC - prevNDC) * 0.5; // In UV space
    #endif
    
    #if DEBUG_MODE
        outColor.rgb = fs_in.debugColor;
    #endif
}
