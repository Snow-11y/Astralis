#version 460 core

out VS_OUT {
    vec3 rayDir;
    vec2 uv;
} vs;

layout(std140, binding = 0) uniform Camera {
    mat4 invProjection;
    mat4 invView;
    vec4 posTime;
    vec4 params;
    vec4 frame;
};

void main() {
    vec2 pos = vec2(
        (gl_VertexID == 1) ? 3.0 : -1.0,
        (gl_VertexID == 2) ? 3.0 : -1.0
    );

    gl_Position = vec4(pos, 0.0, 1.0);
    vs.uv = pos * 0.5 + 0.5;

    vec4 clip = vec4(pos, 1.0, 1.0);
    vec4 view = invProjection * clip;
    view.xyz /= view.w;

    vec4 world = invView * vec4(normalize(view.xyz), 0.0);
    vs.rayDir = normalize(world.xyz);
}
