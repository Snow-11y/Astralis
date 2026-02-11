package stellar.snow.astralis.integration.Haku;

import org.joml.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import org.lwjgl.util.spvc.*;
import org.lwjgl.util.spvc.Spvc.*;

import java.io.*;
import java.lang.Math;
import java.lang.foreign.*;
import java.lang.invoke.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import static org.lwjgl.opengl.GL46C.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.spvc.Spvc.*;

/**
 * ════════════════════════════════════════════════════════════════════════════════════════════════════════
 *  ██╗  ██╗ █████╗ ██╗  ██╗██╗   ██╗    ███████╗███╗   ██╗ ██████╗ ██╗███╗   ██╗███████╗
 *  ██║  ██║██╔══██╗██║ ██╔╝██║   ██║    ██╔════╝████╗  ██║██╔════╝ ██║████╗  ██║██╔════╝
 *  ███████║███████║█████╔╝ ██║   ██║    █████╗  ██╔██╗ ██║██║  ███╗██║██╔██╗ ██║█████╗  
 *  ██╔══██║██╔══██║██╔═██╗ ██║   ██║    ██╔══╝  ██║╚██╗██║██║   ██║██║██║╚██╗██║██╔══╝  
 *  ██║  ██║██║  ██║██║  ██╗╚██████╔╝    ███████╗██║ ╚████║╚██████╔╝██║██║ ╚████║███████╗
 *  ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝     ╚══════╝╚═╝  ╚═══╝ ╚═════╝ ╚═╝╚═╝  ╚═══╝╚══════╝
 * ════════════════════════════════════════════════════════════════════════════════════════════════════════
 * 
 * Ultra high-performance rendering engine built from scratch.
 * 
 * Architecture:
 *   • Zero heap allocation in render loop (JOML stack + persistent mapped buffers)
 *   • Lock-free state via VarHandle
 *   • SPIR-V shader compilation with reflection via SPIRV-Cross
 *   • Quaternion camera with configurable smooth zoom
 *   • GPU raymarched volumetric clouds
 *   • Configurable fog systems (distance, water, lava)
 *   • Optimized leaves face culling for mesh generation
 *   • Zero-copy mesh building with direct buffer access
 * 
 * Requirements: Java 25+, LWJGL 3.3.6, JOML 1.10+
 * 
 * @author Haku Team
 * @version 1.0.0
 */
public final class Haku implements AutoCloseable {

    // ════════════════════════════════════════════════════════════════════════════════════════════════
    // CONSTANTS
    // ════════════════════════════════════════════════════════════════════════════════════════════════

    public static final int UBO_CAMERA = 0;
    public static final int UBO_CLOUDS = 1;
    public static final int UBO_FOG = 2;

    private static final int CAMERA_UBO_SIZE = 256;
    private static final int CLOUD_UBO_SIZE = 256;
    private static final int FOG_UBO_SIZE = 128;

    // ════════════════════════════════════════════════════════════════════════════════════════════════
    // VARHANDLE - Lock-Free State Management
    // ════════════════════════════════════════════════════════════════════════════════════════════════

    private static final VarHandle FRAME_HANDLE;
    private static final VarHandle DIRTY_HANDLE;

    static {
        try {
            var lookup = MethodHandles.lookup();
            FRAME_HANDLE = lookup.findVarHandle(Haku.class, "frameCounter", long.class);
            DIRTY_HANDLE = lookup.findVarHandle(Haku.class, "dirtyFlags", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static final int DIRTY_PROJECTION = 1;
    public static final int DIRTY_VIEW = 1 << 1;
    public static final int DIRTY_CLOUDS = 1 << 2;
    public static final int DIRTY_FOG = 1 << 3;
    public static final int DIRTY_ALL = 0xFFFFFFFF;

    // ════════════════════════════════════════════════════════════════════════════════════════════════
    // INSTANCE STATE
    // ════════════════════════════════════════════════════════════════════════════════════════════════

    private final HakuConfig config;
    private final HakuCamera camera;
    private final HakuZoom zoom;
    private final HakuCloudRenderer clouds;
    private final HakuFogController fog;
    private final HakuLeavesCuller leavesCuller;
    private final HakuShaderPipeline shaders;
    private final HakuMeshBuilder meshBuilder;

    // Lock-free state
    @SuppressWarnings("unused") private volatile long frameCounter;
    @SuppressWarnings("unused") private volatile int dirtyFlags = DIRTY_ALL;

    // Timing
    private long lastNanos;
    private double deltaTime;
    private double totalTime;

    // GPU Resources - Persistent Mapped Buffers
    private int cameraUbo, cloudUbo, fogUbo;
    private long cameraMapped, cloudMapped, fogMapped;
    private int fullscreenVao;

    private boolean initialized;

    // ════════════════════════════════════════════════════════════════════════════════════════════════
    // CONFIGURATION (Immutable Record)
    // ════════════════════════════════════════════════════════════════════════════════════════════════

    public record HakuConfig(
        int width,
        int height,
        float fov,
        float zNear,
        float zFar,
        
        // Zoom
        float zoomFactor,
        float zoomSpeed,
        EasingFunction zoomEasing,
        
        // Clouds
        boolean cloudsEnabled,
        float cloudAltitudeMin,
        float cloudAltitudeMax,
        float cloudCoverage,
        float cloudDensity,
        int cloudSteps,
        int cloudLightSteps,
        
        // Fog
        boolean distanceFogEnabled,
        boolean waterFogEnabled,
        boolean lavaFogEnabled,
        float fogDensity,
        float fogStart,
        float fogEnd,
        
        // Leaves
        boolean leavesCullingEnabled,
        int leavesCullingDepth,
        
        // Mesh
        int meshBuilderCapacity
    ) {
        public HakuConfig {
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid dimensions");
            if (zNear <= 0 || zFar <= zNear) throw new IllegalArgumentException("Invalid clip planes");
            if (zoomFactor < 1.0f) throw new IllegalArgumentException("Zoom factor must be >= 1");
            if (cloudSteps < 8 || cloudSteps > 256) throw new IllegalArgumentException("Cloud steps: [8,256]");
        }

        public float aspect() { return (float) width / height; }

        public static HakuConfig defaults() {
            return new HakuConfig(
                1920, 1080, 70.0f, 0.05f, 1600.0f,
                4.0f, 10.0f, EasingFunction.CUBIC_OUT,
                true, 1500.0f, 4000.0f, 0.5f, 0.8f, 64, 6,
                true, true, false, 0.015f, 0.0f, 512.0f,
                true, 4,
                1 << 20
            );
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════════
    // EASING FUNCTIONS
    // ════════════════════════════════════════════════════════════════════════════════════════════════

    public enum EasingFunction {
        LINEAR {
            @Override public float apply(float t) { return t; }
        },
        QUADRATIC_OUT {
            @Override public float apply(float t) { return 1.0f - (1.0f - t) * (1.0f - t); }
        },
        CUBIC_OUT {
            @Override public float apply(float t) { float u = 1.0f - t; return 1.0f - u * u * u; }
        },
        EXPONENTIAL_OUT {
            @Override public float apply(float t) { return t >= 1.0f ? 1.0f : 1.0f - (float) Math.pow(2, -10 * t); }
        },
        BACK_OUT {
            @Override public float apply(float t) {
                float c = 1.70158f;
                float u = t - 1.0f;
                return 1.0f + (c + 1.0f) * u * u * u + c * u * u;
            }
        };

        public abstract float apply(float t);
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════════════════════════════════════

    public Haku(HakuConfig config) {
        this.config = Objects.requireNonNull(config);
        this.lastNanos = System.nanoTime();

        this.camera = new HakuCamera(config.fov(), config.aspect(), config.zNear(), config.zFar());
        this.zoom = new HakuZoom(config.zoomFactor(), config.zoomSpeed(), config.zoomEasing());
        this.shaders = new HakuShaderPipeline();
        this.clouds = new HakuCloudRenderer(config, shaders);
        this.fog = new HakuFogController(config);
        this.leavesCuller = new HakuLeavesCuller(config.leavesCullingDepth());
        this.meshBuilder = new HakuMeshBuilder(config.meshBuilderCapacity());
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════════
    // INITIALIZATION (Call after GL context ready)
    // ════════════════════════════════════════════════════════════════════════════════════════════════

    public Haku initialize() {
        if (initialized) return this;

        createUniformBuffers();
        createFullscreenVao();
        clouds.initialize();

        initialized = true;
        return this;
    }

    private void createUniformBuffers() {
        int flags = GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT;

        cameraUbo = glCreateBuffers();
        glNamedBufferStorage(cameraUbo, CAMERA_UBO_SIZE, flags);
        cameraMapped = nglMapNamedBufferRange(cameraUbo, 0, CAMERA_UBO_SIZE, flags);

        cloudUbo = glCreateBuffers();
        glNamedBufferStorage(cloudUbo, CLOUD_UBO_SIZE, flags);
        cloudMapped = nglMapNamedBufferRange(cloudUbo, 0, CLOUD_UBO_SIZE, flags);

        fogUbo = glCreateBuffers();
        glNamedBufferStorage(fogUbo, FOG_UBO_SIZE, flags);
        fogMapped = nglMapNamedBufferRange(fogUbo, 0, FOG_UBO_SIZE, flags);

        glBindBufferBase(GL_UNIFORM_BUFFER, UBO_CAMERA, cameraUbo);
        glBindBufferBase(GL_UNIFORM_BUFFER, UBO_CLOUDS, cloudUbo);
        glBindBufferBase(GL_UNIFORM_BUFFER, UBO_FOG, fogUbo);
    }

    private void createFullscreenVao() {
        fullscreenVao = glCreateVertexArrays();
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════════
    // FRAME UPDATE
    // ════════════════════════════════════════════════════════════════════════════════════════════════

    public void update() {
        long now = System.nanoTime();
        deltaTime = (now - lastNanos) * 1e-9;
        lastNanos = now;
        totalTime += deltaTime;

        FRAME_HANDLE.getAndAdd(this, 1L);

        // Update zoom -> affects camera projection
        if (zoom.update((float) deltaTime)) {
            camera.setFovMultiplier(zoom.current());
            markDirty(DIRTY_PROJECTION);
        }

        camera.update();
        if (camera.consumeViewDirty()) markDirty(DIRTY_VIEW);
        if (camera.consumeProjectionDirty()) markDirty(DIRTY_PROJECTION);

        if (config.cloudsEnabled()) {
            clouds.update((float) deltaTime, (float) totalTime);
        }

        syncUniforms();
    }

    private void syncUniforms() {
        int flags = (int) DIRTY_HANDLE.getAndSet(this, 0);
        if (flags == 0) return;

        if ((flags & (DIRTY_VIEW | DIRTY_PROJECTION)) != 0) {
            camera.writeUbo(cameraMapped, (float) totalTime, frameCounter);
        }
        if ((flags & DIRTY_CLOUDS) != 0) {
            clouds.writeUbo(cloudMapped);
        }
        if ((flags & DIRTY_FOG) != 0) {
            fog.writeUbo(fogMapped);
        }
    }

    public void markDirty(int flags) {
        DIRTY_HANDLE.getAndBitwiseOr(this, flags);
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════════
    // RENDERING
    // ════════════════════════════════════════════════════════════════════════════════════════════════

    public void renderClouds() {
        if (!config.cloudsEnabled()) return;

        glBindVertexArray(fullscreenVao);
        clouds.bind();
        glDrawArrays(GL_TRIANGLES, 0, 3);
        clouds.unbind();
    }

    public void drawFullscreenTriangle() {
        glBindVertexArray(fullscreenVao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════════
    // ACCESSORS
    // ════════════════════════════════════════════════════════════════════════════════════════════════

    public HakuCamera camera() { return camera; }
    public HakuZoom zoom() { return zoom; }
    public HakuCloudRenderer clouds() { return clouds; }
    public HakuFogController fog() { return fog; }
    public HakuLeavesCuller leavesCuller() { return leavesCuller; }
    public HakuShaderPipeline shaders() { return shaders; }
    public HakuMeshBuilder meshBuilder() { return meshBuilder; }
    public HakuConfig config() { return config; }

    public long frame() { return frameCounter; }
    public double deltaTime() { return deltaTime; }
    public double totalTime() { return totalTime; }

    // ════════════════════════════════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ════════════════════════════════════════════════════════════════════════════════════════════════

    @Override
    public void close() {
        if (!initialized) return;

        glUnmapNamedBuffer(cameraUbo);
        glUnmapNamedBuffer(cloudUbo);
        glUnmapNamedBuffer(fogUbo);

        glDeleteBuffers(new int[]{cameraUbo, cloudUbo, fogUbo});
        glDeleteVertexArrays(fullscreenVao);

        clouds.close();
        shaders.close();
        meshBuilder.close();

        initialized = false;
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════════
    // ████  HAKU CAMERA  ████
    // Quaternion-based camera with JOML stack allocation, frustum culling
    // ════════════════════════════════════════════════════════════════════════════════════════════════

    public static final class HakuCamera {

        // Matrices (persistent, reused)
        private final Matrix4f projection = new Matrix4f();
        private final Matrix4f view = new Matrix4f();
        private final Matrix4f invProjection = new Matrix4f();
        private final Matrix4f invView = new Matrix4f();
        private final Matrix4f viewProjection = new Matrix4f();

        // Transform
        private final Vector3f position = new Vector3f();
        private final Quaternionf orientation = new Quaternionf();

        // Frustum planes: left, right, bottom, top, near, far
        private final Vector4f[] frustumPlanes = new Vector4f[6];

        // Parameters
        private final float baseFov;
        private final float aspect;
        private final float zNear;
        private final float zFar;
        private float fovMultiplier = 1.0f;

        // Dirty tracking
        private boolean viewDirty = true;
        private boolean projectionDirty = true;

        public HakuCamera(float fov, float aspect, float zNear, float zFar) {
            this.baseFov = fov;
            this.aspect = aspect;
            this.zNear = zNear;
            this.zFar = zFar;

            for (int i = 0; i < 6; i++) frustumPlanes[i] = new Vector4f();

            rebuildProjection();
            rebuildView();
        }

        public void update() {
            if (projectionDirty) rebuildProjection();
            if (viewDirty) rebuildView();
        }

        private void rebuildProjection() {
            float fov = (float) Math.toRadians(baseFov / fovMultiplier);
            projection.setPerspective(fov, aspect, zNear, zFar);
            projection.invert(invProjection);
            rebuildViewProjection();
            projectionDirty = false;
        }

        private void rebuildView() {
            // View = inverse of camera transform
            // Camera transform: translate then rotate
            // View: inverse rotate then inverse translate
            view.identity();
            
            // Conjugate orientation for view matrix
            try (MemoryStack stack = stackPush()) {
                Quaternionf conj = new Quaternionf(orientation).conjugate();
                view.rotate(conj);
            }
            view.translate(-position.x, -position.y, -position.z);
            
            view.invert(invView);
            rebuildViewProjection();
            viewDirty = false;
        }

        private void rebuildViewProjection() {
            projection.mul(view, viewProjection);
            extractFrustumPlanes();
        }

        private void extractFrustumPlanes() {
            // Gribb/Hartmann method
            Matrix4f m = viewProjection;

            // Left: row3 + row0
            frustumPlanes[0].set(m.m03() + m.m00(), m.m13() + m.m10(), m.m23() + m.m20(), m.m33() + m.m30()).normalize3();
            // Right: row3 - row0
            frustumPlanes[1].set(m.m03() - m.m00(), m.m13() - m.m10(), m.m23() - m.m20(), m.m33() - m.m30()).normalize3();
            // Bottom: row3 + row1
            frustumPlanes[2].set(m.m03() + m.m01(), m.m13() + m.m11(), m.m23() + m.m21(), m.m33() + m.m31()).normalize3();
            // Top: row3 - row1
            frustumPlanes[3].set(m.m03() - m.m01(), m.m13() - m.m11(), m.m23() - m.m21(), m.m33() - m.m31()).normalize3();
            // Near: row3 + row2
            frustumPlanes[4].set(m.m03() + m.m02(), m.m13() + m.m12(), m.m23() + m.m22(), m.m33() + m.m32()).normalize3();
            // Far: row3 - row2
            frustumPlanes[5].set(m.m03() - m.m02(), m.m13() - m.m12(), m.m23() - m.m22(), m.m33() - m.m32()).normalize3();
        }

        /**
         * Frustum-sphere test. Returns true if sphere may be visible.
         */
        public boolean testSphere(float x, float y, float z, float radius) {
            for (int i = 0; i < 6; i++) {
                Vector4f p = frustumPlanes[i];
                if (p.x * x + p.y * y + p.z * z + p.w < -radius) return false;
            }
            return true;
        }

        /**
         * Frustum-AABB test. Returns true if AABB may be visible.
         */
        public boolean testAABB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            for (int i = 0; i < 6; i++) {
                Vector4f p = frustumPlanes[i];
                // P-vertex: furthest along normal
                float px = p.x > 0 ? maxX : minX;
                float py = p.y > 0 ? maxY : minY;
                float pz = p.z > 0 ? maxZ : minZ;
                if (p.x * px + p.y * py + p.z * pz + p.w < 0) return false;
            }
            return true;
        }

        // Position
        public void setPosition(float x, float y, float z) {
            position.set(x, y, z);
            viewDirty = true;
        }

        public void setPosition(Vector3fc pos) {
            position.set(pos);
            viewDirty = true;
        }

        public void translate(float dx, float dy, float dz) {
            position.add(dx, dy, dz);
            viewDirty = true;
        }

        // Orientation
        public void setOrientation(Quaternionfc q) {
            orientation.set(q);
            viewDirty = true;
        }

        public void setRotation(float pitch, float yaw, float roll) {
            orientation.identity()
                .rotateY((float) Math.toRadians(-yaw))
                .rotateX((float) Math.toRadians(pitch))
                .rotateZ((float) Math.toRadians(roll));
            viewDirty = true;
        }

        public void lookAt(float eyeX, float eyeY, float eyeZ, float targetX, float targetY, float targetZ) {
            position.set(eyeX, eyeY, eyeZ);
            
            // Direction vector
            float dx = targetX - eyeX;
            float dy = targetY - eyeY;
            float dz = targetZ - eyeZ;
            
            // Create rotation from direction
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > 1e-6f) {
                dx /= len; dy /= len; dz /= len;
                
                // Calculate yaw and pitch
                float yaw = (float) Math.atan2(dx, dz);
                float pitch = (float) Math.asin(-dy);
                
                orientation.identity()
                    .rotateY(yaw)
                    .rotateX(pitch);
            }
            viewDirty = true;
        }

        // FOV
        public void setFovMultiplier(float mult) {
            if (Math.abs(mult - fovMultiplier) > 1e-6f) {
                fovMultiplier = mult;
                projectionDirty = true;
            }
        }

        public float fov() { return baseFov / fovMultiplier; }

        // Dirty state
        public boolean consumeViewDirty() {
            boolean d = viewDirty;
            viewDirty = false;
            return d;
        }

        public boolean consumeProjectionDirty() {
            boolean d = projectionDirty;
            projectionDirty = false;
            return d;
        }

        // Getters
        public Vector3fc position() { return position; }
        public Quaternionfc orientation() { return orientation; }
        public Matrix4fc projection() { return projection; }
        public Matrix4fc view() { return view; }
        public Matrix4fc invProjection() { return invProjection; }
        public Matrix4fc invView() { return invView; }
        public Matrix4fc viewProjection() { return viewProjection; }
        public float zNear() { return zNear; }
        public float zFar() { return zFar; }

        /**
         * Write camera UBO (std140 layout):
         *   mat4 invProjection    [0-63]
         *   mat4 invView          [64-127]
         *   vec4 position_time    [128-143]
         *   vec4 params           [144-159]  near, far, fov, aspect
         *   vec4 frame            [160-175]  frameIndex, 0, 0, 0
         */
        public void writeUbo(long ptr, float time, long frame) {
            invProjection.getToAddress(ptr);
            invView.getToAddress(ptr + 64);

            memPutFloat(ptr + 128, position.x);
            memPutFloat(ptr + 132, position.y);
            memPutFloat(ptr + 136, position.z);
            memPutFloat(ptr + 140, time);

            memPutFloat(ptr + 144, zNear);
            memPutFloat(ptr + 148, zFar);
            memPutFloat(ptr + 152, fov());
            memPutFloat(ptr + 156, aspect);

            memPutFloat(ptr + 160, (float) frame);
            memPutFloat(ptr + 164, 0.0f);
            memPutFloat(ptr + 168, 0.0f);
            memPutFloat(ptr + 172, 0.0f);
        }

        /**
         * Get forward direction vector (no allocation with stack)
         */
        public Vector3f getForward(Vector3f dest) {
            return orientation.transform(dest.set(0, 0, -1));
        }

        /**
         * Get right direction vector
         */
        public Vector3f getRight(Vector3f dest) {
            return orientation.transform(dest.set(1, 0, 0));
        }

        /**
         * Get up direction vector
         */
        public Vector3f getUp(Vector3f dest) {
            return orientation.transform(dest.set(0, 1, 0));
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════════
    // ████  HAKU ZOOM  ████
    // Smooth zoom with configurable easing
    // ════════════════════════════════════════════════════════════════════════════════════════════════

    public static final class HakuZoom {

        private final float maxZoom;
        private final float speed;
        private final EasingFunction easing;

        private float current = 1.0f;
        private float target = 1.0f;
        private float progress = 1.0f;
        private float startValue = 1.0f;

        public HakuZoom(float maxZoom, float speed, EasingFunction easing) {
            this.maxZoom = maxZoom;
            this.speed = speed;
            this.easing = easing;
        }

        /**
         * @return true if zoom changed this frame
         */
        public boolean update(float dt) {
            if (Math.abs(current - target) < 1e-5f) {
                current = target;
                return false;
            }

            progress += dt * speed;
            if (progress >= 1.0f) {
                progress = 1.0f;
                current = target;
            } else {
                float t = easing.apply(progress);
                current = startValue + (target - startValue) * t;
            }

            return true;
        }

        public void setZooming(boolean zooming) {
            float newTarget = zooming ? maxZoom : 1.0f;
            if (Math.abs(newTarget - target) > 1e-5f) {
                startValue = current;
                target = newTarget;
                progress = 0.0f;
            }
        }

        public void toggleZoom() {
            setZooming(target < maxZoom * 0.5f);
        }

        public float current() { return current; }
        public float target() { return target; }
        public boolean isZooming() { return target > 1.5f; }
        public boolean isTransitioning() { return Math.abs(current - target) > 1e-5f; }
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════════
    // ████  HAKU CLOUD RENDERER  ████
    // GPU raymarched volumetric clouds
    // ════════════════════════════════════════════════════════════════════════════════════════════════

    public static final class HakuCloudRenderer implements AutoCloseable {

        private final HakuConfig config;
        private final HakuShaderPipeline shaders;

        private int program;
        private int noiseTexture3D;
        private int detailNoiseTexture;
        private int blueNoiseTexture;
        private int weatherTexture;

        // Animated state
        private final Vector3f windOffset = new Vector3f();
        private final Vector3f sunDir = new Vector3f(0.4f, 0.8f, 0.3f).normalize();
        private final Vector3f sunColor = new Vector3f(1.0f, 0.95f, 0.88f);

        private float coverage;
        private float density;
        private float absorption = 0.035f;
        private float scattering = 0.7f;
        private float phaseG = 0.65f;
        private float ambientStrength = 0.25f;

        public HakuCloudRenderer(HakuConfig config, HakuShaderPipeline shaders) {
            this.config = config;
            this.shaders = shaders;
            this.coverage = config.cloudCoverage();
            this.density = config.cloudDensity();
        }

        public void initialize() {
            // Load shaders (expects SPIR-V or GLSL in working directory or classpath)
            program = shaders.createProgram(
                "haku_clouds.vert",
                "haku_clouds.frag"
            );

            noiseTexture3D = generateWorleyPerlin3D(128);
            detailNoiseTexture = generateWorleyPerlin3D(32);
            blueNoiseTexture = generateBlueNoise2D(256);
            weatherTexture = generateWeatherMap(512);
        }

        private int generateWorleyPerlin3D(int size) {
            int tex = glCreateTextures(GL_TEXTURE_3D);
            glTextureStorage3D(tex, 1, GL_RGBA8, size, size, size);

            byte[] data = new byte[size * size * size * 4];
            Random rng = new Random(42);

            // Pre-generate cell points for Worley
            int cells = Math.max(4, size / 16);
            float[][][] cellPoints = new float[cells][cells][cells * 3];
            for (int cz = 0; cz < cells; cz++) {
                for (int cy = 0; cy < cells; cy++) {
                    for (int cx = 0; cx < cells; cx++) {
                        cellPoints[cz][cy][cx * 3] = (cx + rng.nextFloat()) / cells;
                        cellPoints[cz][cy][cx * 3 + 1] = (cy + rng.nextFloat()) / cells;
                        cellPoints[cz][cy][cx * 3 + 2] = (cz + rng.nextFloat()) / cells;
                    }
                }
            }

            for (int z = 0; z < size; z++) {
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        int idx = ((z * size + y) * size + x) * 4;
                        float fx = x / (float) size;
                        float fy = y / (float) size;
                        float fz = z / (float) size;

                        // R: Perlin-like value noise
                        float perlin = valueNoise3D(fx * 4, fy * 4, fz * 4, rng.nextLong());

                        // G,B,A: Worley at different frequencies
                        float w1 = worleyNoise3D(fx, fy, fz, cells, cellPoints);
                        float w2 = worleyNoise3D(fx * 2, fy * 2, fz * 2, cells, cellPoints);
                        float w3 = worleyNoise3D(fx * 4, fy * 4, fz * 4, cells, cellPoints);

                        data[idx]     = (byte) (Math.clamp(perlin, 0f, 1f) * 255);
                        data[idx + 1] = (byte) (Math.clamp(1f - w1, 0f, 1f) * 255);
                        data[idx + 2] = (byte) (Math.clamp(1f - w2, 0f, 1f) * 255);
                        data[idx + 3] = (byte) (Math.clamp(1f - w3, 0f, 1f) * 255);
                    }
                }
            }

            try (MemoryStack stack = stackPush()) {
                ByteBuffer buf = stack.malloc(data.length);
                buf.put(data).flip();
                glTextureSubImage3D(tex, 0, 0, 0, 0, size, size, size, GL_RGBA, GL_UNSIGNED_BYTE, buf);
            }

            glTextureParameteri(tex, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTextureParameteri(tex, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTextureParameteri(tex, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTextureParameteri(tex, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTextureParameteri(tex, GL_TEXTURE_WRAP_R, GL_REPEAT);

            return tex;
        }

        private float valueNoise3D(float x, float y, float z, long seed) {
            int xi = (int) Math.floor(x);
            int yi = (int) Math.floor(y);
            int zi = (int) Math.floor(z);
            float xf = x - xi;
            float yf = y - yi;
            float zf = z - zi;

            // Smoothstep
            float u = xf * xf * (3 - 2 * xf);
            float v = yf * yf * (3 - 2 * yf);
            float w = zf * zf * (3 - 2 * zf);

            // Hash corners
            float n000 = hash3(xi, yi, zi, seed);
            float n100 = hash3(xi + 1, yi, zi, seed);
            float n010 = hash3(xi, yi + 1, zi, seed);
            float n110 = hash3(xi + 1, yi + 1, zi, seed);
            float n001 = hash3(xi, yi, zi + 1, seed);
            float n101 = hash3(xi + 1, yi, zi + 1, seed);
            float n011 = hash3(xi, yi + 1, zi + 1, seed);
            float n111 = hash3(xi + 1, yi + 1, zi + 1, seed);

            // Trilinear interpolation
            float nx00 = n000 + u * (n100 - n000);
            float nx10 = n010 + u * (n110 - n010);
            float nx01 = n001 + u * (n101 - n001);
            float nx11 = n011 + u * (n111 - n011);
            float nxy0 = nx00 + v * (nx10 - nx00);
            float nxy1 = nx01 + v * (nx11 - nx01);

            return nxy0 + w * (nxy1 - nxy0);
        }

        private float hash3(int x, int y, int z, long seed) {
            long h = seed;
            h ^= x * 374761393L;
            h ^= y * 668265263L;
            h ^= z * 1274126177L;
            h = (h ^ (h >> 13)) * 1274126177L;
            return (h & 0xFFFFFL) / (float) 0xFFFFFL;
        }

        private float worleyNoise3D(float x, float y, float z, int cells, float[][][] points) {
            x = (x % 1.0f + 1.0f) % 1.0f;
            y = (y % 1.0f + 1.0f) % 1.0f;
            z = (z % 1.0f + 1.0f) % 1.0f;

            int cx = (int) (x * cells) % cells;
            int cy = (int) (y * cells) % cells;
            int cz = (int) (z * cells) % cells;

            float minDist = 1.0f;

            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int ncx = (cx + dx + cells) % cells;
                        int ncy = (cy + dy + cells) % cells;
                        int ncz = (cz + dz + cells) % cells;

                        float px = points[ncz][ncy][ncx * 3] + dx / (float) cells;
                        float py = points[ncz][ncy][ncx * 3 + 1] + dy / (float) cells;
                        float pz = points[ncz][ncy][ncx * 3 + 2] + dz / (float) cells;

                        float ddx = x - px;
                        float ddy = y - py;
                        float ddz = z - pz;
                        float dist = ddx * ddx + ddy * ddy + ddz * ddz;

                        minDist = Math.min(minDist, dist);
                    }
                }
            }

            return (float) Math.sqrt(minDist);
        }

        private int generateBlueNoise2D(int size) {
            int tex = glCreateTextures(GL_TEXTURE_2D);
            glTextureStorage2D(tex, 1, GL_RG8, size, size);

            // Simple approximation - proper blue noise would use void-and-cluster
            byte[] data = new byte[size * size * 2];
            Random rng = new Random(12345);

            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int idx = (y * size + x) * 2;
                    // R2 sequence for decent spatial distribution
                    float g = 1.32471795724f;
                    float a1 = 1.0f / g;
                    float a2 = 1.0f / (g * g);
                    int n = y * size + x;
                    float r = (0.5f + a1 * n) % 1.0f;
                    float g2 = (0.5f + a2 * n) % 1.0f;
                    
                    data[idx] = (byte) (r * 255);
                    data[idx + 1] = (byte) (g2 * 255);
                }
            }

            try (MemoryStack stack = stackPush()) {
                ByteBuffer buf = stack.malloc(data.length);
                buf.put(data).flip();
                glTextureSubImage2D(tex, 0, 0, 0, size, size, GL_RG, GL_UNSIGNED_BYTE, buf);
            }

            glTextureParameteri(tex, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTextureParameteri(tex, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTextureParameteri(tex, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTextureParameteri(tex, GL_TEXTURE_WRAP_T, GL_REPEAT);

            return tex;
        }

        private int generateWeatherMap(int size) {
            int tex = glCreateTextures(GL_TEXTURE_2D);
            glTextureStorage2D(tex, 1, GL_RGBA8, size, size);

            byte[] data = new byte[size * size * 4];
            long seed = 999;

            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int idx = (y * size + x) * 4;
                    float fx = x / (float) size;
                    float fy = y / (float) size;

                    // Coverage (multi-octave)
                    float cov = valueNoise3D(fx * 2, fy * 2, 0, seed) * 0.6f +
                               valueNoise3D(fx * 4, fy * 4, 0, seed + 1) * 0.3f +
                               valueNoise3D(fx * 8, fy * 8, 0, seed + 2) * 0.1f;

                    // Cloud type
                    float type = valueNoise3D(fx * 3 + 50, fy * 3 + 50, 0, seed);

                    // Precipitation
                    float precip = valueNoise3D(fx * 6 + 100, fy * 6 + 100, 0, seed);

                    data[idx]     = (byte) (Math.clamp(cov, 0f, 1f) * 255);
                    data[idx + 1] = (byte) (Math.clamp(type, 0f, 1f) * 255);
                    data[idx + 2] = (byte) (Math.clamp(precip, 0f, 1f) * 255);
                    data[idx + 3] = (byte) 255;
                }
            }

            try (MemoryStack stack = stackPush()) {
                ByteBuffer buf = stack.malloc(data.length);
                buf.put(data).flip();
                glTextureSubImage2D(tex, 0, 0, 0, size, size, GL_RGBA, GL_UNSIGNED_BYTE, buf);
            }

            glTextureParameteri(tex, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTextureParameteri(tex, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTextureParameteri(tex, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTextureParameteri(tex, GL_TEXTURE_WRAP_T, GL_REPEAT);

            return tex;
        }

        public void update(float dt, float time) {
            // Animate wind
            windOffset.x += 0.015f * dt;
            windOffset.z += 0.005f * dt;
        }

        public void bind() {
            glUseProgram(program);

            glBindTextureUnit(0, noiseTexture3D);
            glBindTextureUnit(1, detailNoiseTexture);
            glBindTextureUnit(2, blueNoiseTexture);
            glBindTextureUnit(3, weatherTexture);

            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glDepthMask(false);
        }

        public void unbind() {
            glDepthMask(true);
            glDisable(GL_BLEND);
            glUseProgram(0);
        }

        /**
         * Write cloud UBO (std140):
         *   vec4 layer      [0-15]   min, max, coverage, density
         *   vec4 shape      [16-31]  scale, detail, erosion, curl
         *   vec4 light      [32-47]  absorption, scattering, phaseG, ambient
         *   vec4 wind       [48-63]  xyz offset, speed
         *   vec4 sun_dir    [64-79]  xyz dir, intensity
         *   vec4 sun_color  [80-95]  rgb, 0
         *   vec4 quality    [96-111] steps, lightSteps, 0, 0
         */
        public void writeUbo(long ptr) {
            memPutFloat(ptr, config.cloudAltitudeMin());
            memPutFloat(ptr + 4, config.cloudAltitudeMax());
            memPutFloat(ptr + 8, coverage);
            memPutFloat(ptr + 12, density);

            memPutFloat(ptr + 16, 0.0002f);
            memPutFloat(ptr + 20, 0.3f);
            memPutFloat(ptr + 24, 0.5f);
            memPutFloat(ptr + 28, 0.1f);

            memPutFloat(ptr + 32, absorption);
            memPutFloat(ptr + 36, scattering);
            memPutFloat(ptr + 40, phaseG);
            memPutFloat(ptr + 44, ambientStrength);

            memPutFloat(ptr + 48, windOffset.x);
            memPutFloat(ptr + 52, windOffset.y);
            memPutFloat(ptr + 56, windOffset.z);
            memPutFloat(ptr + 60, 0.015f);

            memPutFloat(ptr + 64, sunDir.x);
            memPutFloat(ptr + 68, sunDir.y);
            memPutFloat(ptr + 72, sunDir.z);
            memPutFloat(ptr + 76, 2.2f);

            memPutFloat(ptr + 80, sunColor.x);
            memPutFloat(ptr + 84, sunColor.y);
            memPutFloat(ptr + 88, sunColor.z);
            memPutFloat(ptr + 92, 0);

            memPutFloat(ptr + 96, config.cloudSteps());
            memPutFloat(ptr + 100, config.cloudLightSteps());
            memPutFloat(ptr + 104, 0);
            memPutFloat(ptr + 108, 0);
        }

        // Setters
        public void setSunDirection(float x, float y, float z) {
            sunDir.set(x, y, z).normalize();
        }

        public void setSunColor(float r, float g, float b) {
            sunColor.set(r, g, b);
        }

        public void setCoverage(float c) { coverage = Math.clamp(c, 0f, 1f); }
        public void setDensity(float d) { density = Math.clamp(d, 0f, 2f); }

        @Override
        public void close() {
            if (program != 0) glDeleteProgram(program);
            glDeleteTextures(new int[]{noiseTexture3D, detailNoiseTexture, blueNoiseTexture, weatherTexture});
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════════
    // ████  HAKU FOG CONTROLLER  ████
    // Distance, water, and lava fog
    // ════════════════════════════════════════════════════════════════════════════════════════════════

    public static final class HakuFogController {

        private boolean distanceEnabled;
        private boolean waterEnabled;
        private boolean lavaEnabled;

        private float density;
        private float start;
        private float end;

        private final Vector3f distanceColor = new Vector3f(0.7f, 0.8f, 0.95f);
        private final Vector3f waterColor = new Vector3f(0.0f, 0.05f, 0.15f);
        private final Vector3f lavaColor = new Vector3f(0.6f, 0.1f, 0.0f);

        private float waterDensity = 0.06f;
        private float lavaDensity = 2.5f;

        public HakuFogController(HakuConfig config) {
            this.distanceEnabled = config.distanceFogEnabled();
            this.waterEnabled = config.waterFogEnabled();
            this.lavaEnabled = config.lavaFogEnabled();
            this.density = config.fogDensity();
            this.start = config.fogStart();
            this.end = config.fogEnd();
        }

        /**
         * Write fog UBO (std140):
         *   vec4 params       [0-15]   density, start, end, flags
         *   vec4 distColor    [16-31]  rgb, 0
         *   vec4 waterParams  [32-47]  rgb, density
         *   vec4 lavaParams   [48-63]  rgb, density
         */
        public void writeUbo(long ptr) {
            int flags = (distanceEnabled ? 1 : 0) | (waterEnabled ? 2 : 0) | (lavaEnabled ? 4 : 0);

            memPutFloat(ptr, density);
            memPutFloat(ptr + 4, start);
            memPutFloat(ptr + 8, end);
            memPutInt(ptr + 12, flags);

            memPutFloat(ptr + 16, distanceColor.x);
            memPutFloat(ptr + 20, distanceColor.y);
            memPutFloat(ptr + 24, distanceColor.z);
            memPutFloat(ptr + 28, 0);

            memPutFloat(ptr + 32, waterColor.x);
            memPutFloat(ptr + 36, waterColor.y);
            memPutFloat(ptr + 40, waterColor.z);
            memPutFloat(ptr + 44, waterDensity);

            memPutFloat(ptr + 48, lavaColor.x);
            memPutFloat(ptr + 52, lavaColor.y);
            memPutFloat(ptr + 56, lavaColor.z);
            memPutFloat(ptr + 60, lavaDensity);
        }

        // Setters
        public void setDistanceEnabled(boolean e) { distanceEnabled = e; }
        public void setWaterEnabled(boolean e) { waterEnabled = e; }
        public void setLavaEnabled(boolean e) { lavaEnabled = e; }
        public void setDensity(float d) { density = d; }
        public void setRange(float start, float end) { this.start = start; this.end = end; }
        public void setDistanceColor(float r, float g, float b) { distanceColor.set(r, g, b); }
        public void setWaterColor(float r, float g, float b) { waterColor.set(r, g, b); }
        public void setLavaColor(float r, float g, float b) { lavaColor.set(r, g, b); }

        // Getters
        public boolean isDistanceEnabled() { return distanceEnabled; }
        public boolean isWaterEnabled() { return waterEnabled; }
        public boolean isLavaEnabled() { return lavaEnabled; }
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════════
    // ████  HAKU LEAVES CULLER  ████
    // Optimized adjacency-based face culling for leaves
    // ════════════════════════════════════════════════════════════════════════════════════════════════

    public static final class HakuLeavesCuller {

        private final int depth;

        // Direction offsets: -X, +X, -Y, +Y, -Z, +Z
        private static final int[][] OFFSETS = {
            {-1, 0, 0}, {1, 0, 0},
            {0, -1, 0}, {0, 1, 0},
            {0, 0, -1}, {0, 0, 1}
        };

        public HakuLeavesCuller(int depth) {
            this.depth = Math.clamp(depth, 1, 8);
        }

        /**
         * Check if a specific face should be culled.
         *
         * @param access Block accessor function
         * @param x Block X
         * @param y Block Y
         * @param z Block Z
         * @param face 0=-X, 1=+X, 2=-Y, 3=+Y, 4=-Z, 5=+Z
         * @return true if face should be culled (hidden by other leaves)
         */
        public boolean shouldCull(BlockAccessor access, int x, int y, int z, int face) {
            int[] off = OFFSETS[face];
            int cx = x + off[0];
            int cy = y + off[1];
            int cz = z + off[2];

            for (int d = 0; d < depth; d++) {
                if (!access.isLeaves(cx, cy, cz)) {
                    return false;
                }
                cx += off[0];
                cy += off[1];
                cz += off[2];
            }
            return true;
        }

        /**
         * Get cull mask for all 6 faces.
         *
         * @return Bitmask where bit N set = face N should be culled
         */
        public int cullMask(BlockAccessor access, int x, int y, int z) {
            int mask = 0;
            for (int face = 0; face < 6; face++) {
                if (shouldCull(access, x, y, z, face)) {
                    mask |= 1 << face;
                }
            }
            return mask;
        }

        /**
         * Count visible faces (not culled).
         */
        public int visibleFaceCount(BlockAccessor access, int x, int y, int z) {
            return 6 - Integer.bitCount(cullMask(access, x, y, z));
        }

        @FunctionalInterface
        public interface BlockAccessor {
            boolean isLeaves(int x, int y, int z);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════════
    // ████  HAKU SHADER PIPELINE  ████
    // SPIR-V compilation using LWJGL's SPIRV-Cross bindings
    // ════════════════════════════════════════════════════════════════════════════════════════════════

    public static final class HakuShaderPipeline implements AutoCloseable {

        private final Map<String, Integer> cache = new ConcurrentHashMap<>();
        private long spvcContext;

        public HakuShaderPipeline() {
            try (MemoryStack stack = stackPush()) {
                PointerBuffer pContext = stack.mallocPointer(1);
                checkSpvc(spvc_context_create(pContext));
                spvcContext = pContext.get(0);
            }
        }

        public int createProgram(String vertexPath, String fragmentPath) {
            String key = vertexPath + "|" + fragmentPath;
            return cache.computeIfAbsent(key, k -> compileProgram(vertexPath, fragmentPath));
        }

        private int compileProgram(String vertexPath, String fragmentPath) {
            // Try SPIR-V first, fall back to GLSL
            byte[] vertSpirv = loadSpirV(vertexPath.replace(".vert", ".vert.spv"));
            byte[] fragSpirv = loadSpirV(fragmentPath.replace(".frag", ".frag.spv"));

            int program;
            if (vertSpirv != null && fragSpirv != null) {
                program = compileSpirvProgram(vertSpirv, fragSpirv);
            } else {
                String vertSrc = loadGlsl(vertexPath);
                String fragSrc = loadGlsl(fragmentPath);
                if (vertSrc == null || fragSrc == null) {
                    throw new RuntimeException("Failed to load shaders: " + vertexPath + ", " + fragmentPath);
                }
                program = compileGlslProgram(vertSrc, fragSrc);
            }

            return program;
        }

        private byte[] loadSpirV(String path) {
            try {
                Path p = Path.of(path);
                if (Files.exists(p)) {
                    return Files.readAllBytes(p);
                }
                // Try classpath
                try (InputStream is = getClass().getResourceAsStream("/" + path)) {
                    if (is != null) return is.readAllBytes();
                }
            } catch (IOException ignored) {}
            return null;
        }

        private String loadGlsl(String path) {
            try {
                Path p = Path.of(path);
                if (Files.exists(p)) {
                    return Files.readString(p, StandardCharsets.UTF_8);
                }
                try (InputStream is = getClass().getResourceAsStream("/" + path)) {
                    if (is != null) return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (IOException ignored) {}
            return null;
        }

        private int compileSpirvProgram(byte[] vertSpirv, byte[] fragSpirv) {
            try (MemoryStack stack = stackPush()) {
                int vertShader = glCreateShader(GL_VERTEX_SHADER);
                ByteBuffer vBuf = stack.malloc(vertSpirv.length);
                vBuf.put(vertSpirv).flip();
                glShaderBinary(new int[]{vertShader}, GL_SHADER_BINARY_FORMAT_SPIR_V, vBuf);
                glSpecializeShader(vertShader, "main", new int[0], new int[0]);
                checkShader(vertShader);

                int fragShader = glCreateShader(GL_FRAGMENT_SHADER);
                ByteBuffer fBuf = stack.malloc(fragSpirv.length);
                fBuf.put(fragSpirv).flip();
                glShaderBinary(new int[]{fragShader}, GL_SHADER_BINARY_FORMAT_SPIR_V, fBuf);
                glSpecializeShader(fragShader, "main", new int[0], new int[0]);
                checkShader(fragShader);

                int prog = glCreateProgram();
                glAttachShader(prog, vertShader);
                glAttachShader(prog, fragShader);
                glLinkProgram(prog);
                checkProgram(prog);

                glDeleteShader(vertShader);
                glDeleteShader(fragShader);

                return prog;
            }
        }

        private int compileGlslProgram(String vertSrc, String fragSrc) {
            int vertShader = glCreateShader(GL_VERTEX_SHADER);
            glShaderSource(vertShader, vertSrc);
            glCompileShader(vertShader);
            checkShader(vertShader);

            int fragShader = glCreateShader(GL_FRAGMENT_SHADER);
            glShaderSource(fragShader, fragSrc);
            glCompileShader(fragShader);
            checkShader(fragShader);

            int prog = glCreateProgram();
            glAttachShader(prog, vertShader);
            glAttachShader(prog, fragShader);
            glLinkProgram(prog);
            checkProgram(prog);

            glDeleteShader(vertShader);
            glDeleteShader(fragShader);

            return prog;
        }

        /**
         * Cross-compile SPIR-V to GLSL (useful for reflection or older drivers)
         */
        public String crossCompileToGlsl(byte[] spirv, int glslVersion) {
            try (MemoryStack stack = stackPush()) {
                PointerBuffer pCompiler = stack.mallocPointer(1);
                
                // Create parsed IR
                PointerBuffer pParsedIr = stack.mallocPointer(1);
                IntBuffer spirvWords = stack.mallocInt(spirv.length / 4);
                ByteBuffer spirvBuf = stack.malloc(spirv.length);
                spirvBuf.put(spirv).flip();
                spirvBuf.asIntBuffer().get(new int[spirv.length / 4]);
                
                // Re-read as int buffer
                spirvBuf.flip();
                for (int i = 0; i < spirv.length / 4; i++) {
                    spirvWords.put(spirvBuf.getInt());
                }
                spirvWords.flip();
                
                checkSpvc(spvc_context_parse_spirv(spvcContext, spirvWords, spirv.length / 4, pParsedIr));
                long parsedIr = pParsedIr.get(0);

                checkSpvc(spvc_context_create_compiler(spvcContext, SPVC_BACKEND_GLSL, parsedIr, SPVC_CAPTURE_MODE_TAKE_OWNERSHIP, pCompiler));
                long compiler = pCompiler.get(0);

                // Set options
                PointerBuffer pOptions = stack.mallocPointer(1);
                checkSpvc(spvc_compiler_create_compiler_options(compiler, pOptions));
                long options = pOptions.get(0);

                spvc_compiler_options_set_uint(options, SPVC_COMPILER_OPTION_GLSL_VERSION, glslVersion);
                spvc_compiler_options_set_bool(options, SPVC_COMPILER_OPTION_GLSL_ES, false);
                spvc_compiler_install_compiler_options(compiler, options);

                // Compile
                PointerBuffer pResult = stack.mallocPointer(1);
                checkSpvc(spvc_compiler_compile(compiler, pResult));

                return memUTF8(pResult.get(0));
            }
        }

        private void checkSpvc(int result) {
            if (result != SPVC_SUCCESS) {
                throw new RuntimeException("SPIR-V Cross error: " + result);
            }
        }

        private void checkShader(int shader) {
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
                String log = glGetShaderInfoLog(shader);
                glDeleteShader(shader);
                throw new RuntimeException("Shader compile error: " + log);
            }
        }

        private void checkProgram(int program) {
            if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
                String log = glGetProgramInfoLog(program);
                glDeleteProgram(program);
                throw new RuntimeException("Program link error: " + log);
            }
        }

        @Override
        public void close() {
            for (int prog : cache.values()) {
                glDeleteProgram(prog);
            }
            cache.clear();

            if (spvcContext != 0) {
                spvc_context_destroy(spvcContext);
                spvcContext = 0;
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════════
    // ████  HAKU MESH BUILDER  ████
    // Zero-allocation mesh building with direct buffer access
    // ════════════════════════════════════════════════════════════════════════════════════════════════

    public static final class HakuMeshBuilder implements AutoCloseable {

        // Vertex format: position (3f) + normal (3f) + uv (2f) + color (4ub packed)
        public static final int VERTEX_SIZE = 3 * 4 + 3 * 4 + 2 * 4 + 4; // 36 bytes

        private final ByteBuffer vertexBuffer;
        private final IntBuffer indexBuffer;

        private int vertexCount;
        private int indexCount;

        // Reusable vectors for calculations (avoid allocation)
        private final Vector3f tempNormal = new Vector3f();
        private final Vector3f tempV0 = new Vector3f();
        private final Vector3f tempV1 = new Vector3f();
        private final Vector3f tempV2 = new Vector3f();

        public HakuMeshBuilder(int capacity) {
            this.vertexBuffer = memAlloc(capacity);
            this.indexBuffer = memAllocInt(capacity / 4);
        }

        public HakuMeshBuilder clear() {
            vertexBuffer.clear();
            indexBuffer.clear();
            vertexCount = 0;
            indexCount = 0;
            return this;
        }

        /**
         * Add vertex with position, normal, UV, and packed color.
         */
        public HakuMeshBuilder vertex(
            float px, float py, float pz,
            float nx, float ny, float nz,
            float u, float v,
            int packedColor
        ) {
            vertexBuffer.putFloat(px).putFloat(py).putFloat(pz);
            vertexBuffer.putFloat(nx).putFloat(ny).putFloat(nz);
            vertexBuffer.putFloat(u).putFloat(v);
            vertexBuffer.putInt(packedColor);
            vertexCount++;
            return this;
        }

        /**
         * Add vertex with position, normal, UV, and RGBA color.
         */
        public HakuMeshBuilder vertex(
            float px, float py, float pz,
            float nx, float ny, float nz,
            float u, float v,
            float r, float g, float b, float a
        ) {
            int packed = packColor(r, g, b, a);
            return vertex(px, py, pz, nx, ny, nz, u, v, packed);
        }

        /**
         * Add triangle indices.
         */
        public HakuMeshBuilder triangle(int i0, int i1, int i2) {
            indexBuffer.put(i0).put(i1).put(i2);
            indexCount += 3;
            return this;
        }

        /**
         * Add quad as two triangles (CCW winding).
         */
        public HakuMeshBuilder quad(int i0, int i1, int i2, int i3) {
            triangle(i0, i1, i2);
            triangle(i0, i2, i3);
            return this;
        }

        /**
         * Add axis-aligned cube face.
         */
        public HakuMeshBuilder face(
            int axis, boolean positive,
            float x, float y, float z,
            float size,
            float u0, float v0, float u1, float v1,
            int color
        ) {
            int base = vertexCount;
            float nx = 0, ny = 0, nz = 0;
            float x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3;

            switch (axis) {
                case 0 -> { // X face
                    float fx = positive ? x + size : x;
                    nx = positive ? 1 : -1;
                    x0 = x1 = x2 = x3 = fx;
                    y0 = y; z0 = z;
                    y1 = y + size; z1 = z;
                    y2 = y + size; z2 = z + size;
                    y3 = y; z3 = z + size;
                }
                case 1 -> { // Y face
                    float fy = positive ? y + size : y;
                    ny = positive ? 1 : -1;
                    y0 = y1 = y2 = y3 = fy;
                    x0 = x; z0 = z;
                    x1 = x; z1 = z + size;
                    x2 = x + size; z2 = z + size;
                    x3 = x + size; z3 = z;
                }
                default -> { // Z face
                    float fz = positive ? z + size : z;
                    nz = positive ? 1 : -1;
                    z0 = z1 = z2 = z3 = fz;
                    x0 = x; y0 = y;
                    x1 = x + size; y1 = y;
                    x2 = x + size; y2 = y + size;
                    x3 = x; y3 = y + size;
                }
            }

            vertex(x0, y0, z0, nx, ny, nz, u0, v0, color);
            vertex(x1, y1, z1, nx, ny, nz, u1, v0, color);
            vertex(x2, y2, z2, nx, ny, nz, u1, v1, color);
            vertex(x3, y3, z3, nx, ny, nz, u0, v1, color);

            if (positive) {
                quad(base, base + 1, base + 2, base + 3);
            } else {
                quad(base, base + 3, base + 2, base + 1);
            }

            return this;
        }

        /**
         * Calculate and set normal from face vertices (flat shading).
         */
        public HakuMeshBuilder calculateFlatNormal(int vertexOffset, int count) {
            if (count < 3) return this;

            int pos = vertexOffset * VERTEX_SIZE;
            tempV0.set(
                vertexBuffer.getFloat(pos),
                vertexBuffer.getFloat(pos + 4),
                vertexBuffer.getFloat(pos + 8)
            );
            tempV1.set(
                vertexBuffer.getFloat(pos + VERTEX_SIZE),
                vertexBuffer.getFloat(pos + VERTEX_SIZE + 4),
                vertexBuffer.getFloat(pos + VERTEX_SIZE + 8)
            );
            tempV2.set(
                vertexBuffer.getFloat(pos + VERTEX_SIZE * 2),
                vertexBuffer.getFloat(pos + VERTEX_SIZE * 2 + 4),
                vertexBuffer.getFloat(pos + VERTEX_SIZE * 2 + 8)
            );

            tempV1.sub(tempV0);
            tempV2.sub(tempV0);
            tempV1.cross(tempV2, tempNormal).normalize();

            for (int i = 0; i < count; i++) {
                int npos = (vertexOffset + i) * VERTEX_SIZE + 12;
                vertexBuffer.putFloat(npos, tempNormal.x);
                vertexBuffer.putFloat(npos + 4, tempNormal.y);
                vertexBuffer.putFloat(npos + 8, tempNormal.z);
            }

            return this;
        }

        // Build into GPU buffers
        public int vertexCount() { return vertexCount; }
        public int indexCount() { return indexCount; }
        public int triangleCount() { return indexCount / 3; }

        public ByteBuffer vertexData() {
            return vertexBuffer.slice(0, vertexCount * VERTEX_SIZE);
        }

        public IntBuffer indexData() {
            return indexBuffer.slice(0, indexCount);
        }

        /**
         * Upload to GPU VAO with VBO/IBO.
         * Returns array: [vao, vbo, ibo]
         */
        public int[] upload() {
            int vao = glCreateVertexArrays();
            int vbo = glCreateBuffers();
            int ibo = glCreateBuffers();

            ByteBuffer vdata = vertexData();
            vdata.flip();
            glNamedBufferData(vbo, vdata, GL_STATIC_DRAW);

            IntBuffer idata = indexData();
            idata.flip();
            glNamedBufferData(ibo, idata, GL_STATIC_DRAW);

            // Bind VBO to VAO
            glVertexArrayVertexBuffer(vao, 0, vbo, 0, VERTEX_SIZE);
            glVertexArrayElementBuffer(vao, ibo);

            // Position (location 0)
            glEnableVertexArrayAttrib(vao, 0);
            glVertexArrayAttribFormat(vao, 0, 3, GL_FLOAT, false, 0);
            glVertexArrayAttribBinding(vao, 0, 0);

            // Normal (location 1)
            glEnableVertexArrayAttrib(vao, 1);
            glVertexArrayAttribFormat(vao, 1, 3, GL_FLOAT, false, 12);
            glVertexArrayAttribBinding(vao, 1, 0);

            // UV (location 2)
            glEnableVertexArrayAttrib(vao, 2);
            glVertexArrayAttribFormat(vao, 2, 2, GL_FLOAT, false, 24);
            glVertexArrayAttribBinding(vao, 2, 0);

            // Color (location 3) - packed as 4 unsigned bytes
            glEnableVertexArrayAttrib(vao, 3);
            glVertexArrayAttribFormat(vao, 3, 4, GL_UNSIGNED_BYTE, true, 32);
            glVertexArrayAttribBinding(vao, 3, 0);

            return new int[]{vao, vbo, ibo};
        }

        public static int packColor(float r, float g, float b, float a) {
            int ri = (int) (Math.clamp(r, 0f, 1f) * 255);
            int gi = (int) (Math.clamp(g, 0f, 1f) * 255);
            int bi = (int) (Math.clamp(b, 0f, 1f) * 255);
            int ai = (int) (Math.clamp(a, 0f, 1f) * 255);
            return (ai << 24) | (bi << 16) | (gi << 8) | ri;
        }

        public static int packColor(int r, int g, int b, int a) {
            return (a << 24) | (b << 16) | (g << 8) | r;
        }

        @Override
        public void close() {
            memFree(vertexBuffer);
            memFree(indexBuffer);
        }
    }
}
