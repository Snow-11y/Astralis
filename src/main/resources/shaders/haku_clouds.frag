#version 460 core

in VS_OUT {
    vec3 rayDir;
    vec2 uv;
} fs;

out vec4 fragColor;

layout(std140, binding = 0) uniform Camera {
    mat4 invProjection;
    mat4 invView;
    vec4 posTime;       // xyz=pos, w=time
    vec4 params;        // near, far, fov, aspect
    vec4 frame;         // frameIndex, 0, 0, 0
};

layout(std140, binding = 1) uniform Clouds {
    vec4 layer;         // min, max, coverage, density
    vec4 shape;         // scale, detail, erosion, curl
    vec4 light;         // absorption, scattering, phaseG, ambient
    vec4 wind;          // xyz=offset, w=speed
    vec4 sunDir;        // xyz=dir, w=intensity
    vec4 sunColor;      // rgb, 0
    vec4 quality;       // steps, lightSteps, 0, 0
};

layout(binding = 0) uniform sampler3D noiseMap;
layout(binding = 1) uniform sampler3D detailMap;
layout(binding = 2) uniform sampler2D blueNoise;
layout(binding = 3) uniform sampler2D weather;

#define CLOUD_MIN layer.x
#define CLOUD_MAX layer.y
#define COVERAGE layer.z
#define DENSITY layer.w
#define ABSORPTION light.x
#define SCATTERING light.y
#define PHASE_G light.z
#define AMBIENT light.w
#define MAX_STEPS int(quality.x)
#define LIGHT_STEPS int(quality.y)

const float PI = 3.14159265359;

float remap(float v, float lo, float hi, float nlo, float nhi) {
    return nlo + (v - lo) / (hi - lo) * (nhi - nlo);
}

float saturate(float x) { return clamp(x, 0.0, 1.0); }

float heightFraction(vec3 p) {
    return saturate((p.y - CLOUD_MIN) / (CLOUD_MAX - CLOUD_MIN));
}

float heightGradient(float h) {
    return smoothstep(0.0, 0.15, h) * smoothstep(0.9, 0.6, h);
}

float sampleDensity(vec3 p, float h, float lod) {
    vec3 sp = p + wind.xyz;

    vec4 base = textureLod(noiseMap, sp * shape.x, lod);
    float fbm = base.g * 0.625 + base.b * 0.25 + base.a * 0.125;
    float baseShape = remap(base.r, fbm - 1.0, 1.0, 0.0, 1.0);
    baseShape *= heightGradient(h);

    vec2 weatherUV = sp.xz * 0.00002;
    float localCov = texture(weather, weatherUV).r * COVERAGE;
    float baseDensity = saturate(remap(baseShape, 1.0 - localCov, 1.0, 0.0, 1.0));

    if (baseDensity <= 0.0) return 0.0;

    vec3 detail = textureLod(detailMap, sp * shape.y, lod).rgb;
    float detailFbm = detail.r * 0.625 + detail.g * 0.25 + detail.b * 0.125;
    float finalDensity = saturate(remap(baseDensity, detailFbm * shape.z, 1.0, 0.0, 1.0));

    return finalDensity * DENSITY;
}

float henyeyGreenstein(float cosT, float g) {
    float g2 = g * g;
    return (1.0 - g2) / (4.0 * PI * pow(1.0 + g2 - 2.0 * g * cosT, 1.5));
}

float lightMarch(vec3 p, float density) {
    vec3 lightDir = normalize(sunDir.xyz);
    float stepSize = (CLOUD_MAX - CLOUD_MIN) / float(LIGHT_STEPS);
    vec3 step = lightDir * stepSize;

    float lightDensity = 0.0;
    vec3 lp = p;

    for (int i = 0; i < LIGHT_STEPS; i++) {
        lp += step;
        float h = heightFraction(lp);
        if (h < 0.0 || h > 1.0) break;
        lightDensity += sampleDensity(lp, h, 2.0) * stepSize;
    }

    float beer = exp(-lightDensity * ABSORPTION);
    float powder = 1.0 - exp(-lightDensity * ABSORPTION * 2.0);

    return beer * mix(1.0, powder, saturate(density * 2.0));
}

void main() {
    vec3 ro = posTime.xyz;
    vec3 rd = normalize(fs.rayDir);

    if (rd.y < 0.0 && ro.y < CLOUD_MIN) {
        fragColor = vec4(0.0);
        return;
    }

    float tMin = (CLOUD_MIN - ro.y) / rd.y;
    float tMax = (CLOUD_MAX - ro.y) / rd.y;
    if (tMin > tMax) { float tmp = tMin; tMin = tMax; tMax = tmp; }

    tMin = max(tMin, params.x);
    tMax = min(tMax, params.y);

    if (tMin > tMax || tMax < 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    vec2 jitter = texture(blueNoise, fs.uv + vec2(frame.x * 0.0137, frame.x * 0.0073)).rg;
    float rayLen = tMax - tMin;
    float stepSize = rayLen / float(MAX_STEPS);
    float t = tMin + jitter.x * stepSize;

    vec4 result = vec4(0.0);
    float transmittance = 1.0;

    float cosTheta = dot(rd, normalize(sunDir.xyz));
    float phase = henyeyGreenstein(cosTheta, PHASE_G) * 0.5 + 
                  henyeyGreenstein(cosTheta, -PHASE_G * 0.5) * 0.5;

    for (int i = 0; i < MAX_STEPS && transmittance > 0.01; i++) {
        vec3 p = ro + rd * t;
        float h = heightFraction(p);
        float lod = clamp(t * 0.0008, 0.0, 3.0);

        float density = sampleDensity(p, h, lod);

        if (density > 0.0) {
            float lightEnergy = lightMarch(p, density);

            vec3 scattered = sunColor.rgb * sunDir.w * lightEnergy * phase * SCATTERING;
            scattered += sunColor.rgb * AMBIENT;

            float extinction = exp(-density * stepSize * ABSORPTION);

            vec3 integScatter = scattered * (1.0 - extinction);
            result.rgb += transmittance * integScatter;
            result.a += (1.0 - extinction) * transmittance;

            transmittance *= extinction;
        }

        t += stepSize;
    }

    result.a = 1.0 - transmittance;
    fragColor = result;
}
