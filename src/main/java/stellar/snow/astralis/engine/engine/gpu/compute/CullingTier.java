package stellar.snow.astralis.engine.gpu.compute;

import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.lang.foreign.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public enum CullingTier {
    FULL(0, 32 * 32),
    MINIMAL(32 * 32, 64 * 64),
    MODERATE(64 * 64, 128 * 128),
    AGGRESSIVE(128 * 128, Double.MAX_VALUE);

    private final double minDistSq;
    private final double maxDistSq;

    CullingTier(double minDistSq, double maxDistSq) {
        this.minDistSq = minDistSq;
        this.maxDistSq = maxDistSq;
    }

    public static CullingTier fromDistanceSquared(double distSq) {
        for (CullingTier tier : values()) {
            if (distSq >= tier.minDistSq && distSq < tier.maxDistSq) {
                return tier;
            }
        }
        return AGGRESSIVE;
    }

    public boolean shouldTickAI() {
        return this == FULL;
    }

    public boolean shouldTickPathfinding() {
        return this == FULL || this == MINIMAL;
    }

    public int getPhysicsInterval() {
        return switch (this) {
            case FULL, MINIMAL -> 1;
            case MODERATE -> 2;
            case AGGRESSIVE -> 10;
        };
    }

    // ========================================================================
    // CULLABLE CATEGORIES
    // ========================================================================

    /**
     * Every distinct object type that participates in distance/frustum/occlusion culling.
     * Each category carries its own defaults so systems can query
     * {@code tier.policy(CullableCategory.PARTICLE)} in a single call.
     */
    public enum CullableCategory {
        PLAYER,
        ENTITY,
        PARTICLE,
        REDSTONE,
        LEAF,
        BLOCK_ENTITY,
        ITEM_ENTITY,
        PROJECTILE,
        AMBIENT_SOUND,
        LIGHT_SOURCE,
        FLUID,
        WEATHER
    }

    // ========================================================================
    // CULLING POLICY RECORD
    // ========================================================================

    /**
     * Immutable policy snapshot describing exactly what to do for a
     * {@link CullableCategory} at a given {@link CullingTier}.
     */
    public record CullingPolicy(
            CullableCategory category,
            CullingTier tier,
            boolean shouldRender,
            boolean shouldTick,
            boolean shouldAnimate,
            boolean shouldCastShadow,
            boolean shouldReceiveLight,
            boolean shouldSpawnParticles,
            int tickInterval,
            int modelLOD,              // 0 = full, 1 = reduced, 2 = billboard, 3 = invisible
            float spawnRateMultiplier, // 0.0–1.0
            float shadowDistanceMul,   // 0.0–1.0
            int maxInstancesPerSource,
            int skinResolution         // power-of-two pixel dim: 64, 32, 16 …
    ) {
        /** Convenience: entity is completely culled. */
        public boolean isFullyCulled() {
            return !shouldRender && !shouldTick;
        }
    }

    // ========================================================================
    // PLAYER CULLING
    // ========================================================================

    /** Whether to render the full player mesh (skeleton + skin). */
    public boolean shouldRenderPlayerModel() {
        return this != AGGRESSIVE;
    }

    /** Whether to render capes / elytras / cosmetic attachments. */
    public boolean shouldRenderPlayerCape() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether to render floating nametag above head. */
    public boolean shouldRenderPlayerNameTag() {
        return this == FULL || this == MINIMAL;
    }

    /** Skeleton animation LOD: 0 = full bone set, 1 = upper/lower halves, 2 = static pose. */
    public int getPlayerAnimationLOD() {
        return switch (this) {
            case FULL -> 0;
            case MINIMAL -> 1;
            case MODERATE -> 2;
            case AGGRESSIVE -> 2;
        };
    }

    /** Skin texture resolution in pixels (width). Down-sampled at distance. */
    public int getPlayerSkinResolution() {
        return switch (this) {
            case FULL -> 64;
            case MINIMAL -> 32;
            case MODERATE -> 16;
            case AGGRESSIVE -> 8;
        };
    }

    /** How often (in ticks) to update player rotation/position on client for remote players. */
    public int getPlayerUpdateInterval() {
        return switch (this) {
            case FULL -> 1;
            case MINIMAL -> 2;
            case MODERATE -> 5;
            case AGGRESSIVE -> 20;
        };
    }

    /** Whether to render held items / armor pieces on remote players. */
    public boolean shouldRenderPlayerEquipment() {
        return this == FULL || this == MINIMAL;
    }

    // ========================================================================
    // ENTITY CULLING
    // ========================================================================

    /** Whether entity mesh should be drawn at all. */
    public boolean shouldRenderEntity() {
        return this != AGGRESSIVE;
    }

    /** Whether to tick entity-local animation state machine. */
    public boolean shouldTickAnimation() {
        return this == FULL || this == MINIMAL;
    }

    /**
     * Model LOD level.
     * 0 = full mesh, 1 = simplified (halved tris), 2 = billboard sprite, 3 = skip.
     */
    public int getEntityModelLOD() {
        return switch (this) {
            case FULL -> 0;
            case MINIMAL -> 1;
            case MODERATE -> 2;
            case AGGRESSIVE -> 3;
        };
    }

    /** Server-side entity tick interval for position/AI sync. */
    public int getEntityUpdateInterval() {
        return switch (this) {
            case FULL -> 1;
            case MINIMAL -> 2;
            case MODERATE -> 4;
            case AGGRESSIVE -> 20;
        };
    }

    /** Shadow render distance multiplier (0 = no shadow). */
    public float getEntityShadowMultiplier() {
        return switch (this) {
            case FULL -> 1.0f;
            case MINIMAL -> 0.5f;
            case MODERATE -> 0.0f;
            case AGGRESSIVE -> 0.0f;
        };
    }

    /** Whether entity should be included in collision broadphase. */
    public boolean shouldCollide() {
        return this == FULL || this == MINIMAL;
    }

    /** Max bone count to evaluate for skeletal animation. */
    public int getMaxAnimatedBones() {
        return switch (this) {
            case FULL -> 128;
            case MINIMAL -> 32;
            case MODERATE -> 8;
            case AGGRESSIVE -> 0;
        };
    }

    // ========================================================================
    // PARTICLE CULLING
    // ========================================================================

    /**
     * Multiplier applied to particle spawn rate. 0 = no particles, 1 = full rate.
     * This is the single most impactful client-side cull; particle overdraw is brutal.
     */
    public float getParticleSpawnRateMultiplier() {
        return switch (this) {
            case FULL -> 1.0f;
            case MINIMAL -> 0.5f;
            case MODERATE -> 0.15f;
            case AGGRESSIVE -> 0.0f;
        };
    }

    /** Hard cap on active particles per emitter source at this tier. */
    public int getMaxParticlesPerSource() {
        return switch (this) {
            case FULL -> 256;
            case MINIMAL -> 64;
            case MODERATE -> 16;
            case AGGRESSIVE -> 0;
        };
    }

    /** Whether particles should simulate physics (gravity, collision). */
    public boolean shouldSimulateParticlePhysics() {
        return this == FULL;
    }

    /** Whether ambient particles (dust, spores, underwater) should spawn. */
    public boolean shouldSpawnAmbientParticles() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether particle lighting should be evaluated (vs flat shaded). */
    public boolean shouldLightParticles() {
        return this == FULL;
    }

    /** Whether to use soft (alpha-blended) or hard (cutout) particle rendering. */
    public boolean shouldUseSoftParticles() {
        return this == FULL;
    }

    // ========================================================================
    // REDSTONE CULLING
    // ========================================================================

    /** How often redstone network state is re-evaluated (in game ticks). */
    public int getRedstoneUpdateInterval() {
        return switch (this) {
            case FULL -> 1;
            case MINIMAL -> 1;
            case MODERATE -> 2;
            case AGGRESSIVE -> 10;
        };
    }

    /** Whether to render the redstone dust wire overlay on blocks. */
    public boolean shouldRenderRedstoneDust() {
        return this != AGGRESSIVE;
    }

    /** Whether redstone torches animate their flicker. */
    public boolean shouldAnimateRedstoneTorches() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether to spawn the red redstone signal particles. */
    public boolean shouldShowRedstoneParticles() {
        return this == FULL;
    }

    /** Whether comparator / repeater face textures animate. */
    public boolean shouldAnimateRedstoneComponents() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether note block particles and sound should emit. */
    public boolean shouldEmitNoteBlockEffects() {
        return this == FULL || this == MINIMAL;
    }

    /**
     * Whether to propagate redstone signal changes at all.
     * At AGGRESSIVE range chunks may defer updates until the player approaches.
     */
    public boolean shouldPropagateRedstoneSignal() {
        return this != AGGRESSIVE;
    }

    // ========================================================================
    // LEAF / FOLIAGE CULLING
    // ========================================================================

    /** Whether leaf blocks should render with transparency (vs opaque fast-leaves). */
    public boolean shouldRenderTransparentLeaves() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether leaves sway with wind animation (vertex shader displacement). */
    public boolean shouldAnimateLeaves() {
        return this == FULL;
    }

    /** Whether to spawn falling leaf / petal particles from leaf blocks. */
    public boolean shouldSpawnLeafParticles() {
        return this == FULL;
    }

    /** Leaf decay check interval in ticks. Higher = less CPU. */
    public int getLeafDecayCheckInterval() {
        return switch (this) {
            case FULL -> 20;
            case MINIMAL -> 40;
            case MODERATE -> 100;
            case AGGRESSIVE -> 400;
        };
    }

    /** Whether leaves should perform distance-based decay checks at all. */
    public boolean shouldCheckLeafDecay() {
        return true; // Always check, but at different intervals
    }

    /** Mip level bias for leaf textures (higher = blurrier, cheaper). */
    public int getLeafMipBias() {
        return switch (this) {
            case FULL -> 0;
            case MINIMAL -> 1;
            case MODERATE -> 2;
            case AGGRESSIVE -> 3;
        };
    }

    /**
     * Whether foliage (tall grass, flowers, ferns) should render.
     * These are often the densest transparent geometry in a scene.
     */
    public boolean shouldRenderFoliage() {
        return this != AGGRESSIVE;
    }

    /** Whether foliage should use wind sway vertex animation. */
    public boolean shouldAnimateFoliage() {
        return this == FULL || this == MINIMAL;
    }

    // ========================================================================
    // BLOCK ENTITY CULLING
    // ========================================================================

    /** Whether block entities (chests, signs, banners, heads) should render special models. */
    public boolean shouldRenderBlockEntity() {
        return this != AGGRESSIVE;
    }

    /** Block entity tick interval (furnace smelting, hopper transfer, etc). */
    public int getBlockEntityTickInterval() {
        return switch (this) {
            case FULL -> 1;
            case MINIMAL -> 1;
            case MODERATE -> 4;
            case AGGRESSIVE -> 20;
        };
    }

    /** Whether animated block entities (enchanting table book, conduit) should animate. */
    public boolean shouldAnimateBlockEntity() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether beacon beam should render. */
    public boolean shouldRenderBeaconBeam() {
        return this != AGGRESSIVE; // Beacons are landmarks, render at distance
    }

    /** Whether end crystal beam/rotation should animate. */
    public boolean shouldAnimateEndCrystal() {
        return this == FULL || this == MINIMAL;
    }

    // ========================================================================
    // ITEM ENTITY CULLING
    // ========================================================================

    /** Whether dropped item entities should render their 3D model. */
    public boolean shouldRenderItemEntity() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether dropped items should bob/rotate. */
    public boolean shouldAnimateItemEntity() {
        return this == FULL;
    }

    /** Item entity merge check interval. */
    public int getItemMergeCheckInterval() {
        return switch (this) {
            case FULL -> 1;
            case MINIMAL -> 5;
            case MODERATE -> 20;
            case AGGRESSIVE -> 80;
        };
    }

    // ========================================================================
    // PROJECTILE CULLING
    // ========================================================================

    /** Whether projectiles (arrows, tridents, fireballs) should render. */
    public boolean shouldRenderProjectile() {
        return this != AGGRESSIVE;
    }

    /** Projectile physics tick interval. */
    public int getProjectilePhysicsInterval() {
        return switch (this) {
            case FULL -> 1;
            case MINIMAL -> 1;
            case MODERATE -> 2;
            case AGGRESSIVE -> 5;
        };
    }

    /** Whether projectile trail particles should emit. */
    public boolean shouldRenderProjectileTrail() {
        return this == FULL;
    }

    // ========================================================================
    // WEATHER & AMBIENT CULLING
    // ========================================================================

    /** Whether rain/snow particles should render in this tier's chunks. */
    public boolean shouldRenderWeatherParticles() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether lightning flash should illuminate blocks at this distance. */
    public boolean shouldRenderLightningFlash() {
        return this != AGGRESSIVE;
    }

    /** Whether ambient sounds (cave ambience, water flow) should play. */
    public boolean shouldPlayAmbientSounds() {
        return this == FULL || this == MINIMAL;
    }

    /** Volume multiplier for ambient audio at this tier. */
    public float getAmbientVolumeMultiplier() {
        return switch (this) {
            case FULL -> 1.0f;
            case MINIMAL -> 0.6f;
            case MODERATE -> 0.0f;
            case AGGRESSIVE -> 0.0f;
        };
    }

    // ========================================================================
    // FLUID CULLING
    // ========================================================================

    /** Whether water/lava surface animation should run. */
    public boolean shouldAnimateFluid() {
        return this == FULL || this == MINIMAL;
    }

    /** Fluid flow tick interval. */
    public int getFluidTickInterval() {
        return switch (this) {
            case FULL -> 1;
            case MINIMAL -> 1;
            case MODERATE -> 5;
            case AGGRESSIVE -> 20;
        };
    }

    /** Whether to render underwater fog / water tint overlay for submerged blocks. */
    public boolean shouldRenderFluidFog() {
        return this == FULL;
    }

    /** Whether bubble column particles should spawn. */
    public boolean shouldSpawnBubbleParticles() {
        return this == FULL;
    }

    // ========================================================================
    // LIGHT SOURCE CULLING
    // ========================================================================

    /** Whether dynamic light sources (held torches, entity fire) should update. */
    public boolean shouldUpdateDynamicLighting() {
        return this == FULL || this == MINIMAL;
    }

    /** Light propagation update interval in ticks. */
    public int getLightUpdateInterval() {
        return switch (this) {
            case FULL -> 1;
            case MINIMAL -> 2;
            case MODERATE -> 10;
            case AGGRESSIVE -> 40;
        };
    }

    /** Whether emissive block textures should glow. */
    public boolean shouldRenderEmissiveTextures() {
        return this != AGGRESSIVE;
    }

    // ========================================================================
    // UNIFIED POLICY LOOKUP
    // ========================================================================

    /**
     * Build a complete {@link CullingPolicy} for any category at this tier.
     * This is the primary query point — systems call this once per entity/chunk
     * and branch on the returned policy.
     */
    public CullingPolicy policy(CullableCategory category) {
        return switch (category) {
            case PLAYER -> new CullingPolicy(
                    category, this,
                    shouldRenderPlayerModel(),
                    true, // always tick players
                    shouldTickAnimation(),
                    getEntityShadowMultiplier() > 0,
                    true,
                    shouldSpawnAmbientParticles(),
                    getPlayerUpdateInterval(),
                    getPlayerAnimationLOD(),
                    1.0f,
                    getEntityShadowMultiplier(),
                    1,
                    getPlayerSkinResolution()
            );
            case ENTITY -> new CullingPolicy(
                    category, this,
                    shouldRenderEntity(),
                    this != AGGRESSIVE,
                    shouldTickAnimation(),
                    getEntityShadowMultiplier() > 0,
                    shouldRenderEntity(),
                    shouldSpawnAmbientParticles(),
                    getEntityUpdateInterval(),
                    getEntityModelLOD(),
                    1.0f,
                    getEntityShadowMultiplier(),
                    Integer.MAX_VALUE,
                    64
            );
            case PARTICLE -> new CullingPolicy(
                    category, this,
                    getParticleSpawnRateMultiplier() > 0,
                    getParticleSpawnRateMultiplier() > 0,
                    shouldSimulateParticlePhysics(),
                    false,
                    shouldLightParticles(),
                    true,
                    1,
                    0,
                    getParticleSpawnRateMultiplier(),
                    0.0f,
                    getMaxParticlesPerSource(),
                    0
            );
            case REDSTONE -> new CullingPolicy(
                    category, this,
                    shouldRenderRedstoneDust(),
                    shouldPropagateRedstoneSignal(),
                    shouldAnimateRedstoneTorches(),
                    false,
                    true,
                    shouldShowRedstoneParticles(),
                    getRedstoneUpdateInterval(),
                    0,
                    1.0f,
                    0.0f,
                    Integer.MAX_VALUE,
                    0
            );
            case LEAF -> new CullingPolicy(
                    category, this,
                    true, // leaves always render (opaque or transparent)
                    shouldCheckLeafDecay(),
                    shouldAnimateLeaves(),
                    true,
                    true,
                    shouldSpawnLeafParticles(),
                    getLeafDecayCheckInterval(),
                    getLeafMipBias(),
                    shouldSpawnLeafParticles() ? 1.0f : 0.0f,
                    1.0f,
                    Integer.MAX_VALUE,
                    0
            );
            case BLOCK_ENTITY -> new CullingPolicy(
                    category, this,
                    shouldRenderBlockEntity(),
                    true,
                    shouldAnimateBlockEntity(),
                    getEntityShadowMultiplier() > 0,
                    true,
                    false,
                    getBlockEntityTickInterval(),
                    getEntityModelLOD(),
                    1.0f,
                    getEntityShadowMultiplier(),
                    Integer.MAX_VALUE,
                    0
            );
            case ITEM_ENTITY -> new CullingPolicy(
                    category, this,
                    shouldRenderItemEntity(),
                    true,
                    shouldAnimateItemEntity(),
                    false,
                    true,
                    false,
                    getItemMergeCheckInterval(),
                    shouldRenderItemEntity() ? 0 : 3,
                    1.0f,
                    0.0f,
                    Integer.MAX_VALUE,
                    0
            );
            case PROJECTILE -> new CullingPolicy(
                    category, this,
                    shouldRenderProjectile(),
                    true,
                    shouldRenderProjectile(),
                    false,
                    true,
                    shouldRenderProjectileTrail(),
                    getProjectilePhysicsInterval(),
                    shouldRenderProjectile() ? 0 : 3,
                    shouldRenderProjectileTrail() ? 1.0f : 0.0f,
                    0.0f,
                    Integer.MAX_VALUE,
                    0
            );
            case AMBIENT_SOUND -> new CullingPolicy(
                    category, this,
                    false,
                    shouldPlayAmbientSounds(),
                    false,
                    false,
                    false,
                    false,
                    1,
                    0,
                    getAmbientVolumeMultiplier(),
                    0.0f,
                    Integer.MAX_VALUE,
                    0
            );
            case LIGHT_SOURCE -> new CullingPolicy(
                    category, this,
                    shouldRenderEmissiveTextures(),
                    shouldUpdateDynamicLighting(),
                    false,
                    false,
                    true,
                    false,
                    getLightUpdateInterval(),
                    0,
                    1.0f,
                    0.0f,
                    Integer.MAX_VALUE,
                    0
            );
            case FLUID -> new CullingPolicy(
                    category, this,
                    true,
                    true,
                    shouldAnimateFluid(),
                    false,
                    true,
                    shouldSpawnBubbleParticles(),
                    getFluidTickInterval(),
                    0,
                    shouldSpawnBubbleParticles() ? 1.0f : 0.0f,
                    0.0f,
                    Integer.MAX_VALUE,
                    0
            );
            case WEATHER -> new CullingPolicy(
                    category, this,
                    shouldRenderWeatherParticles(),
                    shouldRenderWeatherParticles(),
                    shouldRenderWeatherParticles(),
                    false,
                    false,
                    shouldRenderWeatherParticles(),
                    1,
                    0,
                    shouldRenderWeatherParticles() ? 1.0f : 0.0f,
                    0.0f,
                    getMaxParticlesPerSource(),
                    0
            );
        };
    }

    // ========================================================================
    // GPU FRUSTUM CULLING (LWJGL 3.3.6 / GL 4.3 Compute)
    // ========================================================================

    /**
     * Six frustum planes packed as float[24] (nx,ny,nz,d × 6).
     * Extracted from the combined view-projection matrix each frame.
     */
    public record FrustumPlanes(float[] data) {
        public FrustumPlanes {
            if (data.length != 24) throw new IllegalArgumentException("Expected 24 floats, got " + data.length);
        }

        /**
         * Extract frustum planes from a column-major 4×4 view-projection matrix.
         * Standard Griggs/Hartmann method.
         */
        public static FrustumPlanes fromViewProjection(float[] vp) {
            float[] planes = new float[24];
            // Left:   row3 + row0
            planes[0]  = vp[3]  + vp[0];
            planes[1]  = vp[7]  + vp[4];
            planes[2]  = vp[11] + vp[8];
            planes[3]  = vp[15] + vp[12];
            normalizePlane(planes, 0);
            // Right:  row3 - row0
            planes[4]  = vp[3]  - vp[0];
            planes[5]  = vp[7]  - vp[4];
            planes[6]  = vp[11] - vp[8];
            planes[7]  = vp[15] - vp[12];
            normalizePlane(planes, 4);
            // Bottom: row3 + row1
            planes[8]  = vp[3]  + vp[1];
            planes[9]  = vp[7]  + vp[5];
            planes[10] = vp[11] + vp[9];
            planes[11] = vp[15] + vp[13];
            normalizePlane(planes, 8);
            // Top:    row3 - row1
            planes[12] = vp[3]  - vp[1];
            planes[13] = vp[7]  - vp[5];
            planes[14] = vp[11] - vp[9];
            planes[15] = vp[15] - vp[13];
            normalizePlane(planes, 12);
            // Near:   row3 + row2
            planes[16] = vp[3]  + vp[2];
            planes[17] = vp[7]  + vp[6];
            planes[18] = vp[11] + vp[10];
            planes[19] = vp[15] + vp[14];
            normalizePlane(planes, 16);
            // Far:    row3 - row2
            planes[20] = vp[3]  - vp[2];
            planes[21] = vp[7]  - vp[6];
            planes[22] = vp[11] - vp[10];
            planes[23] = vp[15] - vp[14];
            normalizePlane(planes, 20);

            return new FrustumPlanes(planes);
        }

        private static void normalizePlane(float[] planes, int offset) {
            float len = (float) Math.sqrt(
                    planes[offset] * planes[offset] +
                    planes[offset + 1] * planes[offset + 1] +
                    planes[offset + 2] * planes[offset + 2]
            );
            if (len > 1e-8f) {
                planes[offset]     /= len;
                planes[offset + 1] /= len;
                planes[offset + 2] /= len;
                planes[offset + 3] /= len;
            }
        }

        /**
         * CPU-side AABB-vs-frustum test. Returns true if the box is at least partially inside.
         */
        public boolean testAABB(float minX, float minY, float minZ,
                                float maxX, float maxY, float maxZ) {
            for (int i = 0; i < 6; i++) {
                int off = i * 4;
                float nx = data[off], ny = data[off + 1], nz = data[off + 2], d = data[off + 3];

                // P-vertex: the AABB corner most aligned with the plane normal
                float px = nx >= 0 ? maxX : minX;
                float py = ny >= 0 ? maxY : minY;
                float pz = nz >= 0 ? maxZ : minZ;

                if (nx * px + ny * py + nz * pz + d < 0) {
                    return false; // Entirely outside this plane
                }
            }
            return true;
        }

        /**
         * Sphere-vs-frustum test.
         */
        public boolean testSphere(float cx, float cy, float cz, float radius) {
            for (int i = 0; i < 6; i++) {
                int off = i * 4;
                float dist = data[off] * cx + data[off + 1] * cy + data[off + 2] * cz + data[off + 3];
                if (dist < -radius) return false;
            }
            return true;
        }
    }

    /**
     * Result of GPU or CPU occlusion/frustum culling pass.
     */
    public record CullResult(
            int totalCandidates,
            int visibleCount,
            int frustumCulled,
            int occlusionCulled,
            int distanceCulled,
            int tierCulled,
            long computeTimeNanos
    ) {
        public int totalCulled() {
            return frustumCulled + occlusionCulled + distanceCulled + tierCulled;
        }

        public float cullingEfficiency() {
            return totalCandidates > 0 ? (float) totalCulled() / totalCandidates : 0f;
        }
    }

    // ========================================================================
    // GPU COMPUTE CULLING PIPELINE
    // ========================================================================

    /**
     * Manages a GL 4.3 compute shader pipeline for GPU-driven frustum +
     * distance + tier culling.  Processes thousands of AABBs per dispatch,
     * writes a visibility bitfield SSBO that the draw-indirect pass consumes.
     *
     * <p>Lifecycle: create once → {@link #uploadAABBs} per frame →
     * {@link #dispatch} → {@link #readResults}.</p>
     *
     * <p>Requires OpenGL 4.3+ (compute shaders, SSBOs, atomic counters).</p>
     */
    public static final class GpuCullPipeline implements AutoCloseable {

        /** GLSL compute shader source embedded as a constant. */
        private static final String COMPUTE_SHADER_SOURCE = """
            #version 430 core
            layout(local_size_x = 256) in;
            
            struct AABB {
                vec4 minBound; // xyz = min, w = tier (0-3)
                vec4 maxBound; // xyz = max, w = category
            };
            
            struct FrustumData {
                vec4 planes[6];
                vec4 cameraPos; // xyz = cam pos, w = unused
            };
            
            layout(std430, binding = 0) readonly buffer AABBBuffer {
                AABB aabbs[];
            };
            
            layout(std430, binding = 1) readonly buffer FrustumBuffer {
                FrustumData frustum;
            };
            
            layout(std430, binding = 2) buffer VisibilityBuffer {
                uint visibility[]; // 1 bit per entity, packed into uint32s
            };
            
            layout(std430, binding = 3) buffer CounterBuffer {
                uint visibleCount;
                uint frustumCulled;
                uint distanceCulled;
                uint tierCulled;
            };
            
            // Distance thresholds per tier (squared): FULL, MINIMAL, MODERATE, AGGRESSIVE
            uniform vec4 tierThresholdsSq;
            
            bool testFrustum(vec3 bmin, vec3 bmax) {
                for (int i = 0; i < 6; i++) {
                    vec4 plane = frustum.planes[i];
                    vec3 pVertex = mix(bmin, bmax, step(vec3(0.0), plane.xyz));
                    if (dot(plane.xyz, pVertex) + plane.w < 0.0) return false;
                }
                return true;
            }
            
            void main() {
                uint idx = gl_GlobalInvocationID.x;
                if (idx >= aabbs.length()) return;
                
                AABB aabb = aabbs[idx];
                vec3 bmin = aabb.minBound.xyz;
                vec3 bmax = aabb.maxBound.xyz;
                int tier = int(aabb.minBound.w);
                
                // Distance culling
                vec3 center = (bmin + bmax) * 0.5;
                float distSq = dot(center - frustum.cameraPos.xyz, center - frustum.cameraPos.xyz);
                
                float maxDist;
                if      (tier == 0) maxDist = tierThresholdsSq.x;
                else if (tier == 1) maxDist = tierThresholdsSq.y;
                else if (tier == 2) maxDist = tierThresholdsSq.z;
                else                maxDist = tierThresholdsSq.w;
                
                if (distSq > maxDist) {
                    atomicAdd(distanceCulled, 1u);
                    return;
                }
                
                // Frustum culling
                if (!testFrustum(bmin, bmax)) {
                    atomicAdd(frustumCulled, 1u);
                    return;
                }
                
                // Visible — set bit
                uint wordIdx = idx / 32u;
                uint bitIdx  = idx % 32u;
                atomicOr(visibility[wordIdx], 1u << bitIdx);
                atomicAdd(visibleCount, 1u);
            }
            """;

        private int computeProgram;
        private int aabbSSBO;
        private int frustumSSBO;
        private int visibilitySSBO;
        private int counterSSBO;
        private int capacity;
        private boolean alive;
        private final AtomicLong totalDispatches = new AtomicLong();

        /**
         * Compile and link the compute shader, allocate SSBOs.
         *
         * @param maxEntities maximum entities per dispatch
         */
        public GpuCullPipeline(int maxEntities) {
            this.capacity = maxEntities;
            this.alive = true;

            // Compile compute shader
            int shader = GL20.glCreateShader(GL43.GL_COMPUTE_SHADER);
            GL20.glShaderSource(shader, COMPUTE_SHADER_SOURCE);
            GL20.glCompileShader(shader);

            if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetShaderInfoLog(shader, 4096);
                GL20.glDeleteShader(shader);
                throw new RuntimeException("Cull compute shader compilation failed:\n" + log);
            }

            computeProgram = GL20.glCreateProgram();
            GL20.glAttachShader(computeProgram, shader);
            GL20.glLinkProgram(computeProgram);

            if (GL20.glGetProgrami(computeProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetProgramInfoLog(computeProgram, 4096);
                GL20.glDeleteProgram(computeProgram);
                GL20.glDeleteShader(shader);
                throw new RuntimeException("Cull compute program link failed:\n" + log);
            }

            GL20.glDeleteShader(shader);

            // Allocate SSBOs
            aabbSSBO       = GL15.glGenBuffers();
            frustumSSBO    = GL15.glGenBuffers();
            visibilitySSBO = GL15.glGenBuffers();
            counterSSBO    = GL15.glGenBuffers();

            // AABB buffer: 2 × vec4 per entity = 32 bytes
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, aabbSSBO);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) maxEntities * 32, GL15.GL_DYNAMIC_DRAW);

            // Frustum: 6 planes × vec4 + cameraPos vec4 = 28 floats = 112 bytes
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, frustumSSBO);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, 112, GL15.GL_DYNAMIC_DRAW);

            // Visibility: 1 bit per entity → ceil(maxEntities/32) × 4 bytes
            int visWords = (maxEntities + 31) / 32;
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, visibilitySSBO);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) visWords * 4, GL15.GL_DYNAMIC_DRAW);

            // Counter: 4 × uint = 16 bytes
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, counterSSBO);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, 16, GL15.GL_DYNAMIC_DRAW);

            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        }

        /**
         * Upload AABB data for this frame.
         *
         * @param aabbData interleaved (minX,minY,minZ,tier, maxX,maxY,maxZ,category) × count
         * @param count    number of entities
         */
        public void uploadAABBs(FloatBuffer aabbData, int count) {
            ensureAlive();
            if (count > capacity) {
                resize(count);
            }
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, aabbSSBO);
            GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, aabbData);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        }

        /**
         * Upload frustum planes and camera position.
         */
        public void uploadFrustum(FrustumPlanes planes, float camX, float camY, float camZ) {
            ensureAlive();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buf = stack.mallocFloat(28);
                buf.put(planes.data());
                buf.put(camX).put(camY).put(camZ).put(0f);
                buf.flip();

                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, frustumSSBO);
                GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, buf);
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            }
        }

        /**
         * Dispatch the compute shader. Call after uploading AABBs and frustum.
         *
         * @param entityCount number of entities to cull
         * @param tierMaxDistSq tier distance thresholds (squared): FULL, MINIMAL, MODERATE, AGGRESSIVE
         */
        public void dispatch(int entityCount, float[] tierMaxDistSq) {
            ensureAlive();

            // Clear visibility buffer
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, visibilitySSBO);
            GL43.glClearBufferData(GL43.GL_SHADER_STORAGE_BUFFER, GL30.GL_R32UI,
                    GL11.GL_RED_INTEGER, GL11.GL_UNSIGNED_INT, new int[]{0});

            // Clear counters
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, counterSSBO);
            GL43.glClearBufferData(GL43.GL_SHADER_STORAGE_BUFFER, GL30.GL_R32UI,
                    GL11.GL_RED_INTEGER, GL11.GL_UNSIGNED_INT, new int[]{0});

            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

            // Bind SSBOs
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, aabbSSBO);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 1, frustumSSBO);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 2, visibilitySSBO);
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 3, counterSSBO);

            GL20.glUseProgram(computeProgram);

            // Upload tier thresholds
            int loc = GL20.glGetUniformLocation(computeProgram, "tierThresholdsSq");
            GL20.glUniform4f(loc, tierMaxDistSq[0], tierMaxDistSq[1], tierMaxDistSq[2], tierMaxDistSq[3]);

            // Dispatch: 256 threads per workgroup
            int workgroups = (entityCount + 255) / 256;
            GL43.glDispatchCompute(workgroups, 1, 1);

            // Memory barrier — visibility buffer must complete before draw-indirect reads it
            GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);

            GL20.glUseProgram(0);
            totalDispatches.incrementAndGet();
        }

        /**
         * Read back the visibility bitfield (CPU readback — use sparingly).
         */
        public BitSet readVisibility(int entityCount) {
            ensureAlive();
            int wordCount = (entityCount + 31) / 32;

            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, visibilitySSBO);
            IntBuffer buf = GL15.glMapBuffer(GL43.GL_SHADER_STORAGE_BUFFER, GL15.GL_READ_ONLY).asIntBuffer();

            BitSet bits = new BitSet(entityCount);
            for (int w = 0; w < wordCount && w < buf.remaining(); w++) {
                int word = buf.get(w);
                for (int b = 0; b < 32; b++) {
                    int idx = w * 32 + b;
                    if (idx >= entityCount) break;
                    if ((word & (1 << b)) != 0) {
                        bits.set(idx);
                    }
                }
            }

            GL15.glUnmapBuffer(GL43.GL_SHADER_STORAGE_BUFFER);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            return bits;
        }

        /**
         * Read back culling counters.
         */
        public CullResult readResults(int totalCandidates, long computeTimeNanos) {
            ensureAlive();

            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, counterSSBO);
            IntBuffer buf = GL15.glMapBuffer(GL43.GL_SHADER_STORAGE_BUFFER, GL15.GL_READ_ONLY).asIntBuffer();

            int visible = buf.get(0);
            int frustum = buf.get(1);
            int distance = buf.get(2);
            int tier = buf.get(3);

            GL15.glUnmapBuffer(GL43.GL_SHADER_STORAGE_BUFFER);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

            return new CullResult(totalCandidates, visible, frustum, 0, distance, tier, computeTimeNanos);
        }

        /**
         * Get the visibility SSBO handle for binding to draw-indirect pipeline.
         */
        public int visibilitySSBO() { return visibilitySSBO; }

        /**
         * Get the AABB SSBO handle.
         */
        public int aabbSSBO() { return aabbSSBO; }

        /** Total dispatches since creation. */
        public long totalDispatches() { return totalDispatches.get(); }

        private void resize(int newCapacity) {
            this.capacity = newCapacity;

            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, aabbSSBO);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) newCapacity * 32, GL15.GL_DYNAMIC_DRAW);

            int visWords = (newCapacity + 31) / 32;
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, visibilitySSBO);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) visWords * 4, GL15.GL_DYNAMIC_DRAW);

            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        }

        private void ensureAlive() {
            if (!alive) throw new IllegalStateException("GpuCullPipeline has been closed");
        }

        @Override
        public void close() {
            if (!alive) return;
            alive = false;
            GL15.glDeleteBuffers(new int[]{aabbSSBO, frustumSSBO, visibilitySSBO, counterSSBO});
            GL20.glDeleteProgram(computeProgram);
        }
    }

    // ========================================================================
    // GL OCCLUSION QUERY POOL
    // ========================================================================

    /**
     * Pool of GL occlusion queries for hardware-accelerated visibility testing.
     * Used for large occluders (terrain chunks, big structures) where GPU HiZ
     * gives better results than CPU frustum alone.
     */
    public static final class OcclusionQueryPool implements AutoCloseable {

        private final int[] queryIds;
        private final boolean[] inFlight;
        private final int poolSize;
        private int nextSlot;
        private boolean alive = true;

        public OcclusionQueryPool(int poolSize) {
            this.poolSize = poolSize;
            this.queryIds = new int[poolSize];
            this.inFlight = new boolean[poolSize];

            GL15.glGenQueries(queryIds);
            this.nextSlot = 0;
        }

        /**
         * Begin an occlusion query. Returns the slot index.
         * Draw the bounding proxy geometry between begin/end.
         */
        public int begin() {
            ensureAlive();
            int slot = nextSlot;
            nextSlot = (nextSlot + 1) % poolSize;

            // If this slot is still in-flight, wait for it
            if (inFlight[slot]) {
                waitForResult(slot);
            }

            GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED, queryIds[slot]);
            inFlight[slot] = true;
            return slot;
        }

        /**
         * End the current occlusion query.
         */
        public void end() {
            ensureAlive();
            GL15.glEndQuery(GL15.GL_SAMPLES_PASSED);
        }

        /**
         * Check if query result is available (non-blocking).
         */
        public boolean isResultAvailable(int slot) {
            ensureAlive();
            return GL15.glGetQueryObjecti(queryIds[slot], GL15.GL_QUERY_RESULT_AVAILABLE) == GL11.GL_TRUE;
        }

        /**
         * Get the sample count (blocking if not yet available).
         * Returns 0 if fully occluded.
         */
        public int getResult(int slot) {
            ensureAlive();
            inFlight[slot] = false;
            return GL15.glGetQueryObjecti(queryIds[slot], GL15.GL_QUERY_RESULT);
        }

        /**
         * Check visibility: returns true if any samples passed.
         */
        public boolean isVisible(int slot) {
            return getResult(slot) > 0;
        }

        /**
         * Non-blocking visibility check. Returns empty if result not yet available.
         */
        public OptionalInt tryGetResult(int slot) {
            ensureAlive();
            if (!isResultAvailable(slot)) return OptionalInt.empty();
            return OptionalInt.of(getResult(slot));
        }

        private void waitForResult(int slot) {
            // Spin-wait (should rarely happen with proper double-buffering)
            while (!isResultAvailable(slot)) {
                Thread.onSpinWait();
            }
            getResult(slot); // Consume
        }

        private void ensureAlive() {
            if (!alive) throw new IllegalStateException("OcclusionQueryPool closed");
        }

        @Override
        public void close() {
            if (!alive) return;
            alive = false;
            GL15.glDeleteQueries(queryIds);
        }
    }

    // ========================================================================
    // CPU-SIDE BATCH CULLER
    // ========================================================================

    /**
     * High-throughput CPU frustum + distance + tier culler for frames where
     * GPU readback latency is unacceptable (e.g., first frame, teleport).
     * Processes AABBs in tight loop optimized for JIT auto-vectorization.
     */
    public static final class CpuBatchCuller {

        private CpuBatchCuller() {}

        /**
         * Cull a flat array of AABBs against frustum and distance.
         *
         * @param aabbs      packed (minX,minY,minZ,maxX,maxY,maxZ) × count
         * @param count      number of AABBs
         * @param frustum    frustum planes
         * @param camX       camera X
         * @param camY       camera Y
         * @param camZ       camera Z
         * @param maxDistSq  maximum view distance squared (per tier — uses single threshold here)
         * @param outVisible output bitset, set for visible entities
         * @return CullResult statistics
         */
        public static CullResult cull(float[] aabbs, int count,
                                      FrustumPlanes frustum,
                                      float camX, float camY, float camZ,
                                      double maxDistSq,
                                      BitSet outVisible) {
            long start = System.nanoTime();
            int visible = 0, frustumCulled = 0, distanceCulled = 0;

            float[] fp = frustum.data();

            for (int i = 0; i < count; i++) {
                int off = i * 6;
                float minX = aabbs[off],     minY = aabbs[off + 1], minZ = aabbs[off + 2];
                float maxX = aabbs[off + 3], maxY = aabbs[off + 4], maxZ = aabbs[off + 5];

                // Distance check (center of AABB)
                float cx = (minX + maxX) * 0.5f;
                float cy = (minY + maxY) * 0.5f;
                float cz = (minZ + maxZ) * 0.5f;
                float dx = cx - camX, dy = cy - camY, dz = cz - camZ;
                float distSq = dx * dx + dy * dy + dz * dz;

                if (distSq > maxDistSq) {
                    distanceCulled++;
                    continue;
                }

                // Frustum check (6 planes)
                boolean inside = true;
                for (int p = 0; p < 6 && inside; p++) {
                    int po = p * 4;
                    float nx = fp[po], ny = fp[po + 1], nz = fp[po + 2], d = fp[po + 3];

                    float pvx = nx >= 0 ? maxX : minX;
                    float pvy = ny >= 0 ? maxY : minY;
                    float pvz = nz >= 0 ? maxZ : minZ;

                    if (nx * pvx + ny * pvy + nz * pvz + d < 0) {
                        inside = false;
                    }
                }

                if (!inside) {
                    frustumCulled++;
                    continue;
                }

                outVisible.set(i);
                visible++;
            }

            long elapsed = System.nanoTime() - start;
            return new CullResult(count, visible, frustumCulled, 0, distanceCulled, 0, elapsed);
        }

        /**
         * Cull with per-entity tier awareness.
         *
         * @param aabbs  packed (minX,minY,minZ,maxX,maxY,maxZ) × count
         * @param tiers  CullingTier ordinal per entity
         * @param categories CullableCategory ordinal per entity
         */
        public static CullResult cullTiered(float[] aabbs, int[] tiers, int[] categories,
                                            int count,
                                            FrustumPlanes frustum,
                                            float camX, float camY, float camZ,
                                            BitSet outVisible) {
            long start = System.nanoTime();
            int visible = 0, frustumCulled = 0, distanceCulled = 0, tierCulled = 0;

            CullingTier[] allTiers = CullingTier.values();
            float[] fp = frustum.data();

            for (int i = 0; i < count; i++) {
                int off = i * 6;
                float minX = aabbs[off],     minY = aabbs[off + 1], minZ = aabbs[off + 2];
                float maxX = aabbs[off + 3], maxY = aabbs[off + 4], maxZ = aabbs[off + 5];

                float cx = (minX + maxX) * 0.5f;
                float cy = (minY + maxY) * 0.5f;
                float cz = (minZ + maxZ) * 0.5f;
                float dx = cx - camX, dy = cy - camY, dz = cz - camZ;
                float distSq = dx * dx + dy * dy + dz * dz;

                // Determine tier from distance
                CullingTier tier = CullingTier.fromDistanceSquared(distSq);
                CullableCategory cat = CullableCategory.values()[categories[i]];

                // Tier-based render check
                CullingPolicy pol = tier.policy(cat);
                if (!pol.shouldRender()) {
                    tierCulled++;
                    continue;
                }

                // Frustum
                boolean inside = true;
                for (int p = 0; p < 6 && inside; p++) {
                    int po = p * 4;
                    float nx = fp[po], ny = fp[po + 1], nz = fp[po + 2], d = fp[po + 3];

                    float pvx = nx >= 0 ? maxX : minX;
                    float pvy = ny >= 0 ? maxY : minY;
                    float pvz = nz >= 0 ? maxZ : minZ;

                    if (nx * pvx + ny * pvy + nz * pvz + d < 0) {
                        inside = false;
                    }
                }

                if (!inside) {
                    frustumCulled++;
                    continue;
                }

                outVisible.set(i);
                visible++;
            }

            long elapsed = System.nanoTime() - start;
            return new CullResult(count, visible, frustumCulled, 0, distanceCulled, tierCulled, elapsed);
        }
    }

    // ========================================================================
    // CULLING STATISTICS TRACKER
    // ========================================================================

    /**
     * Per-frame statistics accumulator. Thread-safe — systems on different
     * threads can report concurrently, then snapshot at frame boundary.
     */
    public static final class CullingStats {

        private final LongAdder totalCandidates  = new LongAdder();
        private final LongAdder totalVisible     = new LongAdder();
        private final LongAdder totalFrustum     = new LongAdder();
        private final LongAdder totalOcclusion   = new LongAdder();
        private final LongAdder totalDistance     = new LongAdder();
        private final LongAdder totalTier        = new LongAdder();
        private final LongAdder totalComputeNs   = new LongAdder();
        private final EnumMap<CullableCategory, LongAdder> perCategory =
                new EnumMap<>(CullableCategory.class);

        public CullingStats() {
            for (CullableCategory cat : CullableCategory.values()) {
                perCategory.put(cat, new LongAdder());
            }
        }

        /** Report a CullResult into this accumulator. */
        public void report(CullResult result) {
            totalCandidates.add(result.totalCandidates());
            totalVisible.add(result.visibleCount());
            totalFrustum.add(result.frustumCulled());
            totalOcclusion.add(result.occlusionCulled());
            totalDistance.add(result.distanceCulled());
            totalTier.add(result.tierCulled());
            totalComputeNs.add(result.computeTimeNanos());
        }

        /** Report a category-specific cull count. */
        public void reportCategoryCulled(CullableCategory cat, int culled) {
            perCategory.get(cat).add(culled);
        }

        /** Snapshot and reset. Call once per frame. */
        public CullingStatsSnapshot snapshotAndReset() {
            CullingStatsSnapshot snap = new CullingStatsSnapshot(
                    totalCandidates.sumThenReset(),
                    totalVisible.sumThenReset(),
                    totalFrustum.sumThenReset(),
                    totalOcclusion.sumThenReset(),
                    totalDistance.sumThenReset(),
                    totalTier.sumThenReset(),
                    totalComputeNs.sumThenReset(),
                    snapshotCategories()
            );
            for (LongAdder adder : perCategory.values()) adder.reset();
            return snap;
        }

        private EnumMap<CullableCategory, Long> snapshotCategories() {
            EnumMap<CullableCategory, Long> map = new EnumMap<>(CullableCategory.class);
            perCategory.forEach((k, v) -> map.put(k, v.sum()));
            return map;
        }
    }

    public record CullingStatsSnapshot(
            long totalCandidates,
            long totalVisible,
            long frustumCulled,
            long occlusionCulled,
            long distanceCulled,
            long tierCulled,
            long computeTimeNanos,
            EnumMap<CullableCategory, Long> perCategoryCulled
    ) {
        public long totalCulled() {
            return frustumCulled + occlusionCulled + distanceCulled + tierCulled;
        }

        public double efficiencyPercent() {
            return totalCandidates > 0 ? (double) totalCulled() * 100.0 / totalCandidates : 0.0;
        }

        public double computeTimeMs() {
            return computeTimeNanos / 1_000_000.0;
        }
    }

    // ========================================================================
    // TIER THRESHOLD CONFIGURATION
    // ========================================================================

    /**
     * Runtime-adjustable distance thresholds. Allows dynamic quality scaling
     * based on frame budget (e.g., if GPU-bound, shrink FULL radius).
     */
    public static final class TierThresholds {

        private volatile float fullMaxBlocks      = 32f;
        private volatile float minimalMaxBlocks    = 64f;
        private volatile float moderateMaxBlocks   = 128f;
        private volatile float aggressiveMaxBlocks = Float.MAX_VALUE;

        /** All thresholds as squared block distances, ready for GPU uniform upload. */
        public float[] asSquaredArray() {
            float f = fullMaxBlocks * fullMaxBlocks;
            float m = minimalMaxBlocks * minimalMaxBlocks;
            float o = moderateMaxBlocks * moderateMaxBlocks;
            float a = aggressiveMaxBlocks == Float.MAX_VALUE ? Float.MAX_VALUE
                    : aggressiveMaxBlocks * aggressiveMaxBlocks;
            return new float[]{f, m, o, a};
        }

        /** Determine tier from actual block distance (not squared). */
        public CullingTier tierFromDistance(float blockDistance) {
            if (blockDistance <= fullMaxBlocks)      return FULL;
            if (blockDistance <= minimalMaxBlocks)    return MINIMAL;
            if (blockDistance <= moderateMaxBlocks)   return MODERATE;
            return AGGRESSIVE;
        }

        /** Scale all thresholds by a multiplier (for dynamic quality). */
        public void scale(float multiplier) {
            fullMaxBlocks      *= multiplier;
            minimalMaxBlocks   *= multiplier;
            moderateMaxBlocks  *= multiplier;
            // AGGRESSIVE always extends to infinity
        }

        /** Reset to defaults. */
        public void reset() {
            fullMaxBlocks      = 32f;
            minimalMaxBlocks   = 64f;
            moderateMaxBlocks  = 128f;
            aggressiveMaxBlocks = Float.MAX_VALUE;
        }

        public void setFullMaxBlocks(float v)      { fullMaxBlocks = v; }
        public void setMinimalMaxBlocks(float v)    { minimalMaxBlocks = v; }
        public void setModerateMaxBlocks(float v)   { moderateMaxBlocks = v; }

        public float getFullMaxBlocks()      { return fullMaxBlocks; }
        public float getMinimalMaxBlocks()   { return minimalMaxBlocks; }
        public float getModerateMaxBlocks()  { return moderateMaxBlocks; }
    }

    // ========================================================================
    // CONVENIENCE: QUICK POLICY CHECK
    // ========================================================================

    /**
     * One-shot: given a squared distance and a category, get the full policy.
     * This is the fast path most systems will use.
     *
     * <pre>
     * CullingPolicy pol = CullingTier.policyFor(distSq, CullableCategory.ENTITY);
     * if (!pol.shouldRender()) return;
     * if (pol.modelLOD() >= 2) renderBillboard();
     * </pre>
     */
    public static CullingPolicy policyFor(double distanceSquared, CullableCategory category) {
        return fromDistanceSquared(distanceSquared).policy(category);
    }

    /**
     * Batch policy lookup — fills an output array with CullingPolicy per entity.
     * Avoids per-entity enum lookup overhead in tight loops.
     */
    public static void batchPolicies(double[] distancesSq, CullableCategory category,
                                     CullingPolicy[] outPolicies, int count) {
        for (int i = 0; i < count; i++) {
            outPolicies[i] = fromDistanceSquared(distancesSq[i]).policy(category);
        }
    }

    // ========================================================================
    // HUD / UI CULLING
    // ========================================================================

    /**
     * HUD element categories. Each has independent visibility, update rate,
     * and animation policy based on context (combat, idle, menu open, etc.).
     */
    public enum HudElement {
        HOTBAR,
        HEALTH_BAR,
        HUNGER_BAR,
        ARMOR_BAR,
        EXPERIENCE_BAR,
        CROSSHAIR,
        BOSS_BAR,
        SCOREBOARD,
        TAB_LIST,
        CHAT,
        ACTION_BAR,
        SUBTITLE_OVERLAY,
        VIGNETTE,
        TOOLTIP,
        DEBUG_OVERLAY,
        MAP_PREVIEW,
        COMPASS,
        STATUS_EFFECTS,
        HOTBAR_ITEM_NAMES,
        ATTACK_INDICATOR
    }

    /**
     * HUD activity context — determines which elements to cull.
     * Systems set this once per frame based on player state.
     */
    public enum HudContext {
        /** Player is fighting — prioritize combat-relevant HUD. */
        COMBAT,
        /** Normal exploration — standard HUD. */
        EXPLORATION,
        /** Standing still, nothing happening — aggressive HUD cull. */
        IDLE,
        /** Menu/inventory/chat open — minimal world HUD. */
        MENU_OPEN,
        /** Cinematic mode (F1-like) — almost everything hidden. */
        CINEMATIC,
        /** Riding a vehicle — show vehicle-relevant HUD. */
        RIDING,
        /** Swimming/flying — show breath/elytra HUD. */
        TRAVERSAL
    }

    /**
     * Complete HUD culling policy for a specific element in a specific context.
     */
    public record HudCullingPolicy(
            HudElement element,
            HudContext context,
            boolean visible,
            int updateIntervalMs,
            float opacity,
            boolean animateTransitions,
            boolean renderShadow,
            int fadeOutDelayMs,
            boolean forceShow
    ) {
        /** Element is completely hidden. */
        public boolean isHidden() {
            return !visible && !forceShow;
        }
    }

    /**
     * Get HUD culling policy for an element in a given context.
     */
    public static HudCullingPolicy hudPolicy(HudElement element, HudContext context) {
        return switch (element) {
            case HOTBAR -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, true,  16,  1.0f, true,  true,  0,     true);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  33,  1.0f, true,  true,  0,     false);
                case IDLE       -> new HudCullingPolicy(element, context, true,  100, 0.7f, true,  false, 5000,  false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, true,  33,  0.9f, true,  true,  0,     false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, true,  33,  1.0f, true,  true,  0,     false);
            };
            case HEALTH_BAR -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, true,  16,  1.0f, true,  true,  0,     true);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  50,  1.0f, true,  true,  0,     false);
                case IDLE       -> new HudCullingPolicy(element, context, true,  200, 0.5f, true,  false, 8000,  false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, true,  50,  0.8f, true,  true,  5000,  false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, true,  33,  1.0f, true,  true,  0,     false);
            };
            case HUNGER_BAR -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, true,  50,  1.0f, true,  true,  0,     false);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  100, 1.0f, true,  true,  0,     false);
                case IDLE       -> new HudCullingPolicy(element, context, true,  500, 0.4f, false, false, 10000, false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, true,  200, 0.6f, false, false, 5000,  false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, true,  100, 0.8f, true,  true,  0,     false);
            };
            case ARMOR_BAR -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, true,  33,  1.0f, true,  true,  0,     true);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  100, 0.9f, true,  true,  0,     false);
                case IDLE       -> new HudCullingPolicy(element, context, true,  500, 0.4f, false, false, 8000,  false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, true,  200, 0.7f, false, false, 5000,  false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, true,  100, 0.9f, true,  true,  0,     false);
            };
            case EXPERIENCE_BAR -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, true,  100, 0.6f, false, false, 3000,  false);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  200, 0.8f, true,  true,  0,     false);
                case IDLE       -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 3000,  false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 3000,  false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 3000,  false);
            };
            case CROSSHAIR -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, true,  16,  1.0f, true,  true,  0,     true);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  16,  1.0f, true,  true,  0,     false);
                case IDLE       -> new HudCullingPolicy(element, context, true,  33,  0.6f, true,  false, 10000, false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, true,  16,  0.8f, true,  true,  0,     false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, true,  16,  1.0f, true,  true,  0,     false);
            };
            case BOSS_BAR -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, true,  16,  1.0f, true,  true,  0,     true);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  50,  1.0f, true,  true,  0,     false);
                case IDLE       -> new HudCullingPolicy(element, context, true,  100, 0.8f, true,  true,  0,     false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, true,  200, 0.5f, false, false, 0,     false);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, true,  50,  1.0f, true,  true,  0,     false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, true,  50,  1.0f, true,  true,  0,     false);
            };
            case SCOREBOARD -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, true,  200, 0.5f, false, false, 3000,  false);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  200, 0.8f, true,  true,  0,     false);
                case IDLE       -> new HudCullingPolicy(element, context, true,  500, 0.6f, false, false, 10000, false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, true,  500, 0.5f, false, false, 5000,  false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, true,  500, 0.5f, false, false, 5000,  false);
            };
            case TAB_LIST -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case EXPLORATION-> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case IDLE       -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
            };
            case CHAT -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, true,  50,  0.6f, true,  true,  5000,  false);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  50,  1.0f, true,  true,  10000, false);
                case IDLE       -> new HudCullingPolicy(element, context, true,  100, 1.0f, true,  true,  15000, false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, true,  50,  1.0f, true,  true,  0,     true);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, true,  100, 0.8f, true,  true,  10000, false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, true,  100, 0.8f, true,  true,  10000, false);
            };
            case ACTION_BAR -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, true,  16,  1.0f, true,  true,  3000,  false);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  33,  1.0f, true,  true,  5000,  false);
                case IDLE       -> new HudCullingPolicy(element, context, true,  100, 0.8f, true,  false, 3000,  false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, true,  50,  1.0f, true,  true,  3000,  false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, true,  50,  1.0f, true,  true,  3000,  false);
            };
            case SUBTITLE_OVERLAY -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, true,  33,  1.0f, true,  true,  5000,  false);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  50,  1.0f, true,  true,  8000,  false);
                case IDLE       -> new HudCullingPolicy(element, context, true,  100, 0.8f, true,  false, 10000, false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, true,  100, 0.8f, true,  true,  5000,  false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, true,  50,  1.0f, true,  true,  5000,  false);
            };
            case VIGNETTE -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, true,  50,  1.0f, true,  false, 0,     false);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  100, 0.8f, true,  false, 0,     false);
                case IDLE       -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case CINEMATIC  -> new HudCullingPolicy(element, context, true,  100, 0.5f, true,  false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, true,  100, 0.6f, true,  false, 0,     false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, true,  100, 0.7f, true,  false, 0,     false);
            };
            case TOOLTIP -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  16,  1.0f, true,  true,  0,     false);
                case IDLE       -> new HudCullingPolicy(element, context, true,  16,  1.0f, true,  true,  0,     false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, true,  16,  1.0f, true,  true,  0,     true);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
            };
            case DEBUG_OVERLAY -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, true,  100, 0.6f, false, true,  0,     false);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  50,  1.0f, false, true,  0,     false);
                case IDLE       -> new HudCullingPolicy(element, context, true,  50,  1.0f, false, true,  0,     false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, true,  100, 0.8f, false, true,  0,     false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, true,  100, 0.8f, false, true,  0,     false);
            };
            case MAP_PREVIEW -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  200, 1.0f, true,  true,  0,     false);
                case IDLE       -> new HudCullingPolicy(element, context, true,  500, 0.8f, false, true,  0,     false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, true,  200, 1.0f, true,  true,  0,     false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, true,  200, 1.0f, true,  true,  0,     false);
            };
            case COMPASS -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  33,  1.0f, true,  true,  0,     false);
                case IDLE       -> new HudCullingPolicy(element, context, true,  100, 0.7f, false, false, 8000,  false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, true,  33,  1.0f, true,  true,  0,     false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, true,  33,  1.0f, true,  true,  0,     false);
            };
            case STATUS_EFFECTS -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, true,  50,  1.0f, true,  true,  0,     true);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  100, 1.0f, true,  true,  0,     false);
                case IDLE       -> new HudCullingPolicy(element, context, true,  200, 0.7f, true,  false, 10000, false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, true,  200, 0.5f, false, false, 0,     false);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, true,  100, 0.9f, true,  true,  0,     false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, true,  100, 0.9f, true,  true,  0,     false);
            };
            case HOTBAR_ITEM_NAMES -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  33,  1.0f, true,  true,  3000,  false);
                case IDLE       -> new HudCullingPolicy(element, context, true,  50,  0.8f, true,  false, 2000,  false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, true,  50,  0.8f, true,  true,  3000,  false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
            };
            case ATTACK_INDICATOR -> switch (context) {
                case COMBAT     -> new HudCullingPolicy(element, context, true,  16,  1.0f, true,  true,  0,     true);
                case EXPLORATION-> new HudCullingPolicy(element, context, true,  33,  0.8f, true,  true,  5000,  false);
                case IDLE       -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case MENU_OPEN  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case CINEMATIC  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case RIDING     -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
                case TRAVERSAL  -> new HudCullingPolicy(element, context, false, 500, 0.0f, false, false, 0,     false);
            };
        };
    }

    /**
     * Batch query: get all HUD policies for a context, keyed by element.
     */
    public static Map<HudElement, HudCullingPolicy> allHudPolicies(HudContext context) {
        Map<HudElement, HudCullingPolicy> map = new EnumMap<>(HudElement.class);
        for (HudElement el : HudElement.values()) {
            map.put(el, hudPolicy(el, context));
        }
        return map;
    }

    /**
     * Quick check: should this HUD element render at all in the given context?
     */
    public static boolean isHudVisible(HudElement element, HudContext context) {
        return hudPolicy(element, context).visible();
    }

    // ========================================================================
    // COSMETIC / VISUAL DETAIL CULLING
    // ========================================================================

    /** Whether enchantment glint overlay should render on items / armor. */
    public boolean shouldRenderEnchantmentGlint() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether damage tint / red flash should render on entities. */
    public boolean shouldRenderDamageTint() {
        return this != AGGRESSIVE;
    }

    /** Whether floating damage numbers / death animations should show. */
    public boolean shouldRenderDamageIndicators() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether entity name tags (mob custom names) should render. */
    public boolean shouldRenderMobNameTag() {
        return this == FULL;
    }

    /** Whether to render held item swing animation on entities. */
    public boolean shouldRenderSwingAnimation() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether footstep particles should spawn under entities. */
    public boolean shouldSpawnFootstepParticles() {
        return this == FULL;
    }

    /** Whether to render entity fire overlay when burning. */
    public boolean shouldRenderEntityFire() {
        return this != AGGRESSIVE;
    }

    /** Whether to render arrow-stuck-in-entity models. */
    public boolean shouldRenderStuckArrows() {
        return this == FULL;
    }

    // ========================================================================
    // SKY / ATMOSPHERE CULLING
    // ========================================================================

    /** Whether to render clouds. */
    public boolean shouldRenderClouds() {
        return true; // Clouds are always in view — LOD instead
    }

    /** Cloud render quality: 0 = fancy 3D, 1 = flat layer, 2 = disabled. */
    public int getCloudQuality() {
        return switch (this) {
            case FULL -> 0;
            case MINIMAL -> 0;
            case MODERATE -> 1;
            case AGGRESSIVE -> 2;
        };
    }

    /** Whether to render individual stars. */
    public boolean shouldRenderStars() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether sun/moon should render with corona / glow effects. */
    public boolean shouldRenderCelestialGlow() {
        return this == FULL;
    }

    /** Whether sky color gradient should blend per-biome. */
    public boolean shouldBlendBiomeSky() {
        return this == FULL || this == MINIMAL;
    }

    // ========================================================================
    // TERRAIN DETAIL CULLING
    // ========================================================================

    /** Whether biome color blending should apply to grass/water/foliage. */
    public boolean shouldBlendBiomeColors() {
        return this == FULL || this == MINIMAL;
    }

    /** Random block tick interval multiplier (higher = fewer ticks). */
    public int getRandomTickMultiplier() {
        return switch (this) {
            case FULL -> 1;
            case MINIMAL -> 1;
            case MODERATE -> 3;
            case AGGRESSIVE -> 10;
        };
    }

    /** Whether to render block-breaking crack overlay. */
    public boolean shouldRenderBlockBreakProgress() {
        return this != AGGRESSIVE;
    }

    /** Whether block highlight outline should render. */
    public boolean shouldRenderBlockOutline() {
        return this == FULL;
    }

    /** Whether snow layers should accumulate visually. */
    public boolean shouldRenderSnowLayers() {
        return this != AGGRESSIVE;
    }

    /** Whether crop growth stages should animate transitions. */
    public boolean shouldAnimateCropGrowth() {
        return this == FULL;
    }

    // ========================================================================
    // SOUND CULLING (EXPANDED)
    // ========================================================================

    /** Maximum simultaneous sound channels for entities at this tier. */
    public int getMaxSoundChannels() {
        return switch (this) {
            case FULL -> 16;
            case MINIMAL -> 8;
            case MODERATE -> 2;
            case AGGRESSIVE -> 0;
        };
    }

    /** Whether entity idle sounds (cow moo, zombie groan) should play. */
    public boolean shouldPlayEntityIdleSounds() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether block interaction sounds (door, lever, button) should play. */
    public boolean shouldPlayBlockSounds() {
        return this != AGGRESSIVE;
    }

    /** Whether footstep sounds from entities should play. */
    public boolean shouldPlayFootstepSounds() {
        return this == FULL || this == MINIMAL;
    }

    /** Sound volume falloff multiplier. */
    public float getSoundVolumeFalloff() {
        return switch (this) {
            case FULL -> 1.0f;
            case MINIMAL -> 0.7f;
            case MODERATE -> 0.3f;
            case AGGRESSIVE -> 0.0f;
        };
    }

    // ========================================================================
    // SPAWNER / MOB CAP CULLING
    // ========================================================================

    /** Mob spawner activation check interval. */
    public int getSpawnerCheckInterval() {
        return switch (this) {
            case FULL -> 1;
            case MINIMAL -> 2;
            case MODERATE -> 10;
            case AGGRESSIVE -> 40;
        };
    }

    /** Whether mob spawners should actually spawn mobs. */
    public boolean shouldSpawnerActivate() {
        return this != AGGRESSIVE;
    }

    /** Whether to render the spinning mob inside spawner cages. */
    public boolean shouldRenderSpawnerMob() {
        return this == FULL;
    }

    /** Whether to render spawner fire particles. */
    public boolean shouldRenderSpawnerParticles() {
        return this == FULL;
    }

    // ========================================================================
    // PAINTING / ITEM FRAME / ARMOR STAND CULLING
    // ========================================================================

    /** Whether paintings should render. */
    public boolean shouldRenderPainting() {
        return this != AGGRESSIVE;
    }

    /** Painting texture resolution level: 0 = full, 1 = half, 2 = quarter. */
    public int getPaintingResolutionLevel() {
        return switch (this) {
            case FULL -> 0;
            case MINIMAL -> 0;
            case MODERATE -> 1;
            case AGGRESSIVE -> 2;
        };
    }

    /** Whether item frames should render the contained item. */
    public boolean shouldRenderItemFrameContents() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether item frame map contents should render (expensive). */
    public boolean shouldRenderItemFrameMap() {
        return this == FULL;
    }

    /** Whether glow item frames should emit glow. */
    public boolean shouldRenderGlowItemFrame() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether armor stands should render full geometry. */
    public boolean shouldRenderArmorStand() {
        return this != AGGRESSIVE;
    }

    /** Whether armor stand equipment should render. */
    public boolean shouldRenderArmorStandEquipment() {
        return this == FULL || this == MINIMAL;
    }

    /** Armor stand pose update interval. */
    public int getArmorStandUpdateInterval() {
        return switch (this) {
            case FULL -> 1;
            case MINIMAL -> 2;
            case MODERATE -> 10;
            case AGGRESSIVE -> 40;
        };
    }

    // ========================================================================
    // VEHICLE CULLING (MINECART / BOAT)
    // ========================================================================

    /** Whether minecart/boat mesh should render. */
    public boolean shouldRenderVehicle() {
        return this != AGGRESSIVE;
    }

    /** Whether vehicle passenger should render. */
    public boolean shouldRenderVehiclePassenger() {
        return this == FULL || this == MINIMAL;
    }

    /** Vehicle physics tick interval. */
    public int getVehiclePhysicsInterval() {
        return switch (this) {
            case FULL -> 1;
            case MINIMAL -> 1;
            case MODERATE -> 2;
            case AGGRESSIVE -> 10;
        };
    }

    /** Whether minecart sound should play. */
    public boolean shouldPlayVehicleSound() {
        return this == FULL || this == MINIMAL;
    }

    // ========================================================================
    // VILLAGER / NPC CULLING
    // ========================================================================

    /** Whether villager AI gossip system should tick. */
    public boolean shouldTickVillagerGossip() {
        return this == FULL;
    }

    /** Whether villager trade restock should process. */
    public boolean shouldTickVillagerTrades() {
        return this == FULL || this == MINIMAL;
    }

    /** Villager schedule/activity update interval. */
    public int getVillagerScheduleInterval() {
        return switch (this) {
            case FULL -> 1;
            case MINIMAL -> 5;
            case MODERATE -> 20;
            case AGGRESSIVE -> 100;
        };
    }

    /** Whether to render villager profession badge / hat. */
    public boolean shouldRenderVillagerProfession() {
        return this == FULL || this == MINIMAL;
    }

    // ========================================================================
    // SIGN / TEXT RENDERING CULLING
    // ========================================================================

    /** Whether sign text should render. */
    public boolean shouldRenderSignText() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether sign glow effect (glow ink sac) should render. */
    public boolean shouldRenderSignGlow() {
        return this == FULL;
    }

    /** Sign text render resolution: 0 = full SDF, 1 = bitmap, 2 = not rendered. */
    public int getSignTextQuality() {
        return switch (this) {
            case FULL -> 0;
            case MINIMAL -> 1;
            case MODERATE -> 2;
            case AGGRESSIVE -> 2;
        };
    }

    // ========================================================================
    // BANNER CULLING
    // ========================================================================

    /** Whether banner patterns should render (vs solid color). */
    public boolean shouldRenderBannerPattern() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether banner cloth physics animation should run. */
    public boolean shouldAnimateBanner() {
        return this == FULL;
    }

    /** Banner pattern layer limit (fewer = cheaper). */
    public int getMaxBannerLayers() {
        return switch (this) {
            case FULL -> 6;
            case MINIMAL -> 3;
            case MODERATE -> 1;
            case AGGRESSIVE -> 0;
        };
    }

    // ========================================================================
    // MAP RENDERING CULLING
    // ========================================================================

    /** Whether held maps should render their contents. */
    public boolean shouldRenderMapContents() {
        return this == FULL;
    }

    /** Map texture resolution level. */
    public int getMapResolutionLevel() {
        return switch (this) {
            case FULL -> 0;
            case MINIMAL -> 1;
            case MODERATE -> 2;
            case AGGRESSIVE -> 3;
        };
    }

    /** Whether map markers / icons should render. */
    public boolean shouldRenderMapMarkers() {
        return this == FULL;
    }

    // ========================================================================
    // CHEST / CONTAINER ANIMATION CULLING
    // ========================================================================

    /** Whether chest lid animation should play. */
    public boolean shouldAnimateChestLid() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether shulker box open/close animation should play. */
    public boolean shouldAnimateShulkerBox() {
        return this == FULL || this == MINIMAL;
    }

    /** Whether ender chest particles should spawn. */
    public boolean shouldSpawnEnderChestParticles() {
        return this == FULL;
    }

    /** Chest search/sort interval for hopper chains. */
    public int getContainerSearchInterval() {
        return switch (this) {
            case FULL -> 1;
            case MINIMAL -> 1;
            case MODERATE -> 4;
            case AGGRESSIVE -> 20;
        };
    }
}
