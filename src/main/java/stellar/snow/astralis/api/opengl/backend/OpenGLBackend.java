package stellar.snow.astralis.api.opengl.backend;

import stellar.snow.astralis.Astralis;
import stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper;

import java.lang.foreign.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL41.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL44.*;
import static org.lwjgl.opengl.GL45.*;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.opengl.KHRDebug.*;
import static org.lwjgl.opengl.NVMeshShader.*;
import static org.lwjgl.opengl.ARBBindlessTexture.*;

/**
 * OpenGLBackend - High-performance OpenGL GPUBackend implementation with modern Java 21+ features.
 *
 * <p>Core Features:</p>
 * <ul>
 *   <li>Full OpenGL 4.6 support with extension fallbacks</li>
 *   <li>Direct State Access (DSA) where available</li>
 *   <li>Persistent mapped buffers with triple-buffering</li>
 *   <li>Bindless textures support</li>
 *   <li>Automatic VAO management</li>
 *   <li>FBO pooling and caching</li>
 *   <li>Shader program caching</li>
 *   <li>Query object pooling</li>
 *   <li>Debug callback integration</li>
 *   <li>GPU profiling with timer queries</li>
 *   <li>Comprehensive resource tracking</li>
 *   <li>Automatic capability detection</li>
 *   <li>Thread-safe state management</li>
 *   <li>Memory segment integration</li>
 * </ul>
 *
 * @author Enhanced GPU Framework
 * @version 2.0.0
 * @since Java 21
 */
public final class OpenGLBackend implements GPUBackend, AutoCloseable {

    // ========================================================================
    // SINGLETON
    // ========================================================================

    private static volatile OpenGLBackend INSTANCE;
    private static final Object INSTANCE_LOCK = new Object();

    public static OpenGLBackend get() {
        OpenGLBackend instance = INSTANCE;
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                instance = INSTANCE;
                if (instance == null) {
                    INSTANCE = instance = new OpenGLBackend();
                }
            }
        }
        return instance;
    }

    // ========================================================================
    // GL CONSTANTS (Organized)
    // ========================================================================

    /** Buffer targets */
    public static final class GLBufferTarget {
        public static final int ARRAY = GL_ARRAY_BUFFER;
        public static final int ELEMENT_ARRAY = GL_ELEMENT_ARRAY_BUFFER;
        public static final int UNIFORM = GL_UNIFORM_BUFFER;
        public static final int SHADER_STORAGE = GL_SHADER_STORAGE_BUFFER;
        public static final int DRAW_INDIRECT = GL_DRAW_INDIRECT_BUFFER;
        public static final int DISPATCH_INDIRECT = GL_DISPATCH_INDIRECT_BUFFER;
        public static final int COPY_READ = GL_COPY_READ_BUFFER;
        public static final int COPY_WRITE = GL_COPY_WRITE_BUFFER;
        public static final int PIXEL_PACK = GL_PIXEL_PACK_BUFFER;
        public static final int PIXEL_UNPACK = GL_PIXEL_UNPACK_BUFFER;
        public static final int QUERY = GL_QUERY_BUFFER;
        public static final int TEXTURE = GL_TEXTURE_BUFFER;
        public static final int TRANSFORM_FEEDBACK = GL_TRANSFORM_FEEDBACK_BUFFER;
        public static final int ATOMIC_COUNTER = GL_ATOMIC_COUNTER_BUFFER;
        public static final int PARAMETER = GL_PARAMETER_BUFFER;
        private GLBufferTarget() {}
    }

    /** Buffer usage hints */
    public static final class GLBufferUsage {
        public static final int STREAM_DRAW = GL_STREAM_DRAW;
        public static final int STREAM_READ = GL_STREAM_READ;
        public static final int STREAM_COPY = GL_STREAM_COPY;
        public static final int STATIC_DRAW = GL_STATIC_DRAW;
        public static final int STATIC_READ = GL_STATIC_READ;
        public static final int STATIC_COPY = GL_STATIC_COPY;
        public static final int DYNAMIC_DRAW = GL_DYNAMIC_DRAW;
        public static final int DYNAMIC_READ = GL_DYNAMIC_READ;
        public static final int DYNAMIC_COPY = GL_DYNAMIC_COPY;
        private GLBufferUsage() {}
    }

    /** Buffer storage flags */
    public static final class GLStorageFlags {
        public static final int MAP_READ = GL_MAP_READ_BIT;
        public static final int MAP_WRITE = GL_MAP_WRITE_BIT;
        public static final int MAP_PERSISTENT = GL_MAP_PERSISTENT_BIT;
        public static final int MAP_COHERENT = GL_MAP_COHERENT_BIT;
        public static final int DYNAMIC_STORAGE = GL_DYNAMIC_STORAGE_BIT;
        public static final int CLIENT_STORAGE = GL_CLIENT_STORAGE_BIT;
        private GLStorageFlags() {}
    }

    /** Map buffer access flags */
    public static final class GLMapFlags {
        public static final int READ = GL_MAP_READ_BIT;
        public static final int WRITE = GL_MAP_WRITE_BIT;
        public static final int INVALIDATE_RANGE = GL_MAP_INVALIDATE_RANGE_BIT;
        public static final int INVALIDATE_BUFFER = GL_MAP_INVALIDATE_BUFFER_BIT;
        public static final int FLUSH_EXPLICIT = GL_MAP_FLUSH_EXPLICIT_BIT;
        public static final int UNSYNCHRONIZED = GL_MAP_UNSYNCHRONIZED_BIT;
        public static final int PERSISTENT = GL_MAP_PERSISTENT_BIT;
        public static final int COHERENT = GL_MAP_COHERENT_BIT;
        private GLMapFlags() {}
    }

    /** Shader types */
    public static final class GLShaderType {
        public static final int VERTEX = GL_VERTEX_SHADER;
        public static final int FRAGMENT = GL_FRAGMENT_SHADER;
        public static final int GEOMETRY = GL_GEOMETRY_SHADER;
        public static final int TESS_CONTROL = GL_TESS_CONTROL_SHADER;
        public static final int TESS_EVALUATION = GL_TESS_EVALUATION_SHADER;
        public static final int COMPUTE = GL_COMPUTE_SHADER;
        private GLShaderType() {}
    }

    /** Memory barrier bits */
    public static final class GLBarrier {
        public static final int VERTEX_ATTRIB_ARRAY = GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT;
        public static final int ELEMENT_ARRAY = GL_ELEMENT_ARRAY_BARRIER_BIT;
        public static final int UNIFORM = GL_UNIFORM_BARRIER_BIT;
        public static final int TEXTURE_FETCH = GL_TEXTURE_FETCH_BARRIER_BIT;
        public static final int SHADER_IMAGE_ACCESS = GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
        public static final int COMMAND = GL_COMMAND_BARRIER_BIT;
        public static final int PIXEL_BUFFER = GL_PIXEL_BUFFER_BARRIER_BIT;
        public static final int TEXTURE_UPDATE = GL_TEXTURE_UPDATE_BARRIER_BIT;
        public static final int BUFFER_UPDATE = GL_BUFFER_UPDATE_BARRIER_BIT;
        public static final int FRAMEBUFFER = GL_FRAMEBUFFER_BARRIER_BIT;
        public static final int TRANSFORM_FEEDBACK = GL_TRANSFORM_FEEDBACK_BARRIER_BIT;
        public static final int ATOMIC_COUNTER = GL_ATOMIC_COUNTER_BARRIER_BIT;
        public static final int SHADER_STORAGE = GL_SHADER_STORAGE_BARRIER_BIT;
        public static final int CLIENT_MAPPED_BUFFER = GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT;
        public static final int QUERY_BUFFER = GL_QUERY_BUFFER_BARRIER_BIT;
        public static final int ALL = GL_ALL_BARRIER_BITS;
        private GLBarrier() {}
    }

    // ========================================================================
    // CONFIGURATION
    // ========================================================================

    /**
     * Backend configuration.
     */
    public record Config(
        boolean enableDebugOutput,
        boolean enableDebugSync,
        boolean enableProfiling,
        boolean enableBindlessTextures,
        boolean enableDSA,
        boolean enablePersistentMapping,
        boolean useSparseBuffers,
        int maxFramesInFlight,
        int queryPoolSize,
        int fboPoolSize,
        int vaoPoolSize,
        long stagingBufferSize
    ) {
        public static Config defaults() {
            return new Config(
                false,      // debug output
                false,      // debug sync
                true,       // profiling
                true,       // bindless textures
                true,       // DSA
                true,       // persistent mapping
                false,      // sparse buffers
                3,          // frames in flight
                128,        // query pool size
                16,         // FBO pool size
                32,         // VAO pool size
                32 * 1024 * 1024  // 32MB staging
            );
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private boolean enableDebugOutput = false;
            private boolean enableDebugSync = false;
            private boolean enableProfiling = true;
            private boolean enableBindlessTextures = true;
            private boolean enableDSA = true;
            private boolean enablePersistentMapping = true;
            private boolean useSparseBuffers = false;
            private int maxFramesInFlight = 3;
            private int queryPoolSize = 128;
            private int fboPoolSize = 16;
            private int vaoPoolSize = 32;
            private long stagingBufferSize = 32 * 1024 * 1024;

            public Builder enableDebugOutput(boolean val) { enableDebugOutput = val; return this; }
            public Builder enableDebugSync(boolean val) { enableDebugSync = val; return this; }
            public Builder enableProfiling(boolean val) { enableProfiling = val; return this; }
            public Builder enableBindlessTextures(boolean val) { enableBindlessTextures = val; return this; }
            public Builder enableDSA(boolean val) { enableDSA = val; return this; }
            public Builder enablePersistentMapping(boolean val) { enablePersistentMapping = val; return this; }
            public Builder useSparseBuffers(boolean val) { useSparseBuffers = val; return this; }
            public Builder maxFramesInFlight(int val) { maxFramesInFlight = val; return this; }
            public Builder queryPoolSize(int val) { queryPoolSize = val; return this; }
            public Builder fboPoolSize(int val) { fboPoolSize = val; return this; }
            public Builder vaoPoolSize(int val) { vaoPoolSize = val; return this; }
            public Builder stagingBufferSize(long val) { stagingBufferSize = val; return this; }

            public Config build() {
                return new Config(
                    enableDebugOutput, enableDebugSync, enableProfiling,
                    enableBindlessTextures, enableDSA, enablePersistentMapping,
                    useSparseBuffers, maxFramesInFlight, queryPoolSize,
                    fboPoolSize, vaoPoolSize, stagingBufferSize
                );
            }
        }
    }

    // ========================================================================
    // RECORDS & TYPES
    // ========================================================================

    /**
     * OpenGL version info.
     */
    public record GLVersion(
        int major,
        int minor,
        boolean isCore,
        boolean isES,
        String vendorString,
        String rendererString,
        String versionString,
        String shadingLanguageVersion
    ) {
        public int combined() {
            return major * 10 + minor;
        }

        public boolean atLeast(int maj, int min) {
            return major > maj || (major == maj && minor >= min);
        }

        public static GLVersion parse(String version, String vendor, String renderer, String glsl) {
            boolean isES = version.contains("ES");
            boolean isCore = version.contains("Core");

            int major = 3, minor = 3;
            try {
                String[] parts = version.split("[\\s.]");
                major = Integer.parseInt(parts[0]);
                minor = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}

            return new GLVersion(major, minor, isCore, isES, vendor, renderer, version, glsl);
        }
    }

    /**
     * Mapped buffer info for tracking.
     */
    public record MappedBufferInfo(
        long buffer,
        int target,
        MemorySegment mappedSegment,
        ByteBuffer mappedBuffer,
        long offset,
        long size,
        int accessFlags,
        boolean persistent,
        int frameCreated
    ) {}

    /**
     * Buffer resource with metadata.
     */
    public record BufferResource(
        int handle,
        long size,
        int usage,
        int storageFlags,
        MappedBufferInfo mapping,
        boolean immutable,
        String debugName
    ) implements AutoCloseable {
        @Override
        public void close() {
            OpenGLBackend.get().destroyBuffer(handle);
        }
    }

    /**
     * Texture resource with metadata.
     */
    public record TextureResource(
        int handle,
        int target,
        int width,
        int height,
        int depth,
        int internalFormat,
        int mipLevels,
        int arrayLayers,
        int samples,
        long bindlessHandle,
        boolean isBindless,
        String debugName
    ) implements AutoCloseable {
        @Override
        public void close() {
            OpenGLBackend.get().destroyTexture(handle);
        }
    }

    /**
     * Sampler resource.
     */
    public record SamplerResource(
        int handle,
        int minFilter,
        int magFilter,
        int wrapS,
        int wrapT,
        int wrapR,
        float anisotropy,
        String debugName
    ) implements AutoCloseable {
        @Override
        public void close() {
            glDeleteSamplers(handle);
        }
    }

    /**
     * Shader resource.
     */
    public record ShaderResource(
        int handle,
        int type,
        String source,
        boolean isSpirv,
        String debugName
    ) implements AutoCloseable {
        @Override
        public void close() {
            OpenGLBackend.get().destroyShader(handle);
        }
    }

    /**
     * Program resource.
     */
    public record ProgramResource(
        int handle,
        List<ShaderResource> shaders,
        Map<String, Integer> uniformLocations,
        Map<String, Integer> uniformBlockIndices,
        Map<String, Integer> ssboIndices,
        boolean separable,
        String debugName
    ) implements AutoCloseable {
        @Override
        public void close() {
            OpenGLBackend.get().destroyProgram(handle);
        }
    }

    /**
     * VAO resource.
     */
    public record VAOResource(
        int handle,
        long vertexBuffer,
        long indexBuffer,
        List<VertexAttribute> attributes,
        String debugName
    ) {
        public record VertexAttribute(
            int location,
            int size,
            int type,
            boolean normalized,
            int stride,
            long offset
        ) {}
    }

    /**
     * FBO resource.
     */
    public record FBOResource(
        int handle,
        int width,
        int height,
        List<Integer> colorAttachments,
        int depthAttachment,
        int stencilAttachment,
        int depthStencilAttachment,
        String debugName
    ) implements AutoCloseable {
        @Override
        public void close() {
            glDeleteFramebuffers(handle);
        }
    }

    /**
     * Sync object (fence).
     */
    public record SyncObject(
        long handle,
        int type,
        long creationFrame
    ) {}

    /**
     * Timer query result.
     */
    public record TimerQueryResult(
        String name,
        long startNanos,
        long endNanos,
        double elapsedMs
    ) {}

    /**
     * Debug message.
     */
    public record DebugMessage(
        int source,
        int type,
        int id,
        int severity,
        String message,
        Instant timestamp
    ) {
        public String severityString() {
            return switch (severity) {
                case GL_DEBUG_SEVERITY_HIGH -> "HIGH";
                case GL_DEBUG_SEVERITY_MEDIUM -> "MEDIUM";
                case GL_DEBUG_SEVERITY_LOW -> "LOW";
                case GL_DEBUG_SEVERITY_NOTIFICATION -> "NOTIFICATION";
                default -> "UNKNOWN";
            };
        }

        public String sourceString() {
            return switch (source) {
                case GL_DEBUG_SOURCE_API -> "API";
                case GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "WINDOW";
                case GL_DEBUG_SOURCE_SHADER_COMPILER -> "SHADER";
                case GL_DEBUG_SOURCE_THIRD_PARTY -> "THIRD_PARTY";
                case GL_DEBUG_SOURCE_APPLICATION -> "APPLICATION";
                case GL_DEBUG_SOURCE_OTHER -> "OTHER";
                default -> "UNKNOWN";
            };
        }

        public String typeString() {
            return switch (type) {
                case GL_DEBUG_TYPE_ERROR -> "ERROR";
                case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "DEPRECATED";
                case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "UNDEFINED";
                case GL_DEBUG_TYPE_PORTABILITY -> "PORTABILITY";
                case GL_DEBUG_TYPE_PERFORMANCE -> "PERFORMANCE";
                case GL_DEBUG_TYPE_MARKER -> "MARKER";
                case GL_DEBUG_TYPE_PUSH_GROUP -> "PUSH_GROUP";
                case GL_DEBUG_TYPE_POP_GROUP -> "POP_GROUP";
                case GL_DEBUG_TYPE_OTHER -> "OTHER";
                default -> "UNKNOWN";
            };
        }
    }

    // ========================================================================
    // CORE STATE
    // ========================================================================

    private volatile Config config = Config.defaults();
    private volatile boolean initialized = false;
    private volatile boolean closed = false;
    private final Instant creationTime = Instant.now();

    // Version and capabilities
    private volatile GLVersion glVersion;
    private volatile Capabilities capabilities;

    // Resource tracking
    private final ConcurrentHashMap<Integer, BufferResource> bufferResources = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, TextureResource> textureResources = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, SamplerResource> samplerResources = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, ShaderResource> shaderResources = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, ProgramResource> programResources = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, VAOResource> vaoResources = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, FBOResource> fboResources = new ConcurrentHashMap<>();

    // Mapped buffer tracking
    private final ConcurrentHashMap<Long, MappedBufferInfo> mappedBuffers = new ConcurrentHashMap<>();

    // Sync objects per frame
    private final List<SyncObject> frameSyncObjects = new CopyOnWriteArrayList<>();

    // Query pools
    private final Deque<Integer> timerQueryPool = new ConcurrentLinkedDeque<>();
    private final ConcurrentHashMap<Integer, String> activeTimerQueries = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<TimerQueryResult> timerResults = new ConcurrentLinkedQueue<>();

    // VAO pool
    private final Deque<Integer> vaoPool = new ConcurrentLinkedDeque<>();

    // FBO pool
    private final Deque<Integer> fboPool = new ConcurrentLinkedDeque<>();

    // Debug messages
    private final ConcurrentLinkedQueue<DebugMessage> debugMessages = new ConcurrentLinkedQueue<>();
    private static final int MAX_DEBUG_MESSAGES = 1000;

    // Current state tracking (for state filtering)
    private volatile int currentProgram = 0;
    private volatile int currentVAO = 0;
    private volatile int currentFBO = 0;
    private final int[] currentTextures = new int[32];
    private final int[] currentSamplers = new int[32];
    private final int[] currentUBOs = new int[16];
    private final int[] currentSSBOs = new int[16];

    // Frame management
    private final AtomicInteger currentFrame = new AtomicInteger(0);
    private final AtomicLong frameCount = new AtomicLong(0);

    // Staging buffer
    private volatile int stagingBuffer;
    private volatile MemorySegment stagingMemory;
    private final AtomicLong stagingOffset = new AtomicLong(0);

    // Statistics
    private final LongAdder drawCalls = new LongAdder();
    private final LongAdder dispatchCalls = new LongAdder();
    private final LongAdder bufferUploads = new LongAdder();
    private final LongAdder textureUploads = new LongAdder();
    private final LongAdder programBinds = new LongAdder();
    private final LongAdder textureBinds = new LongAdder();
    private final LongAdder vaoBinds = new LongAdder();
    private final LongAdder fboBinds = new LongAdder();
    private final LongAdder memoryBarriers = new LongAdder();
    private final LongAdder stateChanges = new LongAdder();
    private final AtomicLong totalBufferMemory = new AtomicLong(0);
    private final AtomicLong totalTextureMemory = new AtomicLong(0);

    // Concurrency
    private final ReentrantLock stateLock = new ReentrantLock();

    // ========================================================================
    // CAPABILITIES
    // ========================================================================

    /**
     * OpenGL capabilities.
     */
    public record Capabilities(
        boolean multiDrawIndirect,
        boolean indirectCount,
        boolean computeShaders,
        boolean meshShaders,
        boolean taskShaders,
        boolean bufferDeviceAddress,
        boolean persistentMapping,
        boolean immutableStorage,
        boolean directStateAccess,
        boolean bindlessTextures,
        boolean sparseTextures,
        boolean sparseBuffers,
        boolean spirvShaders,
        boolean programPipelines,
        boolean debugOutput,
        boolean timerQuery,
        boolean anisotropicFiltering,
        boolean textureFilterAnisotropic,
        boolean clipControl,
        boolean shaderDrawParameters,
        boolean baseInstance,
        boolean drawIndirectFirstInstance,
        boolean multiBinds,
        boolean textureStorage,
        boolean bufferStorage,
        boolean shaderStorageBufferObject,
        boolean computeVariableGroupSize,
        int maxTextureSize,
        int maxTextureUnits,
        int maxUniformBlockSize,
        int maxSSBOSize,
        int maxComputeWorkGroupSizeX,
        int maxComputeWorkGroupSizeY,
        int maxComputeWorkGroupSizeZ,
        int maxComputeWorkGroupInvocations,
        int maxDrawIndirectCount,
        int maxVertexAttribs,
        int maxColorAttachments,
        float maxAnisotropy,
        long maxBufferSize,
        Set<String> extensions
    ) {
        public static final Capabilities EMPTY = new Capabilities(
            false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0f, 0L, Set.of()
        );
    }

    private Capabilities detectCapabilities() {
        int v = glVersion.combined();
        Set<String> extensions = new HashSet<>();

        // Get extensions
        int numExtensions = glGetInteger(GL_NUM_EXTENSIONS);
        for (int i = 0; i < numExtensions; i++) {
            String ext = glGetStringi(GL_EXTENSIONS, i);
            if (ext != null) extensions.add(ext);
        }

        // Query limits
        int maxTexSize = glGetInteger(GL_MAX_TEXTURE_SIZE);
        int maxTexUnits = glGetInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
        int maxUBOSize = glGetInteger(GL_MAX_UNIFORM_BLOCK_SIZE);
        int maxSSBOSize = v >= 43 ? glGetInteger(GL_MAX_SHADER_STORAGE_BLOCK_SIZE) : 0;
        int maxWorkGroupX = v >= 43 ? glGetIntegeri(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 0) : 0;
        int maxWorkGroupY = v >= 43 ? glGetIntegeri(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 1) : 0;
        int maxWorkGroupZ = v >= 43 ? glGetIntegeri(GL_MAX_COMPUTE_WORK_GROUP_SIZE, 2) : 0;
        int maxWorkGroupInv = v >= 43 ? glGetInteger(GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS) : 0;
        int maxVertexAttribs = glGetInteger(GL_MAX_VERTEX_ATTRIBS);
        int maxColorAttachments = glGetInteger(GL_MAX_COLOR_ATTACHMENTS);

        float maxAniso = 1.0f;
        if (extensions.contains("GL_EXT_texture_filter_anisotropic") ||
            extensions.contains("GL_ARB_texture_filter_anisotropic")) {
            maxAniso = glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY);
        }

        return new Capabilities(
            v >= 43 || extensions.contains("GL_ARB_multi_draw_indirect"),
            v >= 46 || extensions.contains("GL_ARB_indirect_parameters"),
            v >= 43 || extensions.contains("GL_ARB_compute_shader"),
            extensions.contains("GL_NV_mesh_shader"),
            extensions.contains("GL_NV_mesh_shader"),
            extensions.contains("GL_NV_shader_buffer_load") || extensions.contains("GL_ARB_gpu_shader_int64"),
            v >= 44 || extensions.contains("GL_ARB_buffer_storage"),
            v >= 44 || extensions.contains("GL_ARB_buffer_storage"),
            v >= 45 || extensions.contains("GL_ARB_direct_state_access"),
            extensions.contains("GL_ARB_bindless_texture") || extensions.contains("GL_NV_bindless_texture"),
            extensions.contains("GL_ARB_sparse_texture"),
            extensions.contains("GL_ARB_sparse_buffer"),
            v >= 46 || extensions.contains("GL_ARB_gl_spirv"),
            v >= 41 || extensions.contains("GL_ARB_separate_shader_objects"),
            v >= 43 || extensions.contains("GL_KHR_debug"),
            v >= 33 || extensions.contains("GL_ARB_timer_query"),
            extensions.contains("GL_EXT_texture_filter_anisotropic"),
            extensions.contains("GL_ARB_texture_filter_anisotropic"),
            v >= 45 || extensions.contains("GL_ARB_clip_control"),
            v >= 46 || extensions.contains("GL_ARB_shader_draw_parameters"),
            v >= 42 || extensions.contains("GL_ARB_base_instance"),
            v >= 42 || extensions.contains("GL_ARB_base_instance"),
            v >= 44 || extensions.contains("GL_ARB_multi_bind"),
            v >= 42 || extensions.contains("GL_ARB_texture_storage"),
            v >= 44 || extensions.contains("GL_ARB_buffer_storage"),
            v >= 43 || extensions.contains("GL_ARB_shader_storage_buffer_object"),
            extensions.contains("GL_ARB_compute_variable_group_size"),
            maxTexSize,
            maxTexUnits,
            maxUBOSize,
            maxSSBOSize,
            maxWorkGroupX,
            maxWorkGroupY,
            maxWorkGroupZ,
            maxWorkGroupInv,
            Integer.MAX_VALUE,  // OpenGL has no explicit limit
            maxVertexAttribs,
            maxColorAttachments,
            maxAniso,
            Long.MAX_VALUE,  // OpenGL buffer size limit is implementation-defined
            Set.copyOf(extensions)
        );
    }

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    private OpenGLBackend() {
        // Private constructor for singleton
    }

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    /**
     * Initialize backend with default configuration.
     */
    public boolean initialize() {
        return initialize(Config.defaults());
    }

    /**
     * Initialize backend with custom configuration.
     */
    public boolean initialize(Config config) {
        if (initialized) {
            Astralis.LOGGER.warn("[OpenGL] Backend already initialized");
            return true;
        }

        this.config = config;

        try {
            // Initialize OpenGL call mapper
            OpenGLCallMapper.initialize();

            // Parse version info
            glVersion = GLVersion.parse(
                glGetString(GL_VERSION),
                glGetString(GL_VENDOR),
                glGetString(GL_RENDERER),
                glGetString(GL_SHADING_LANGUAGE_VERSION)
            );

            // Detect capabilities
            capabilities = detectCapabilities();

            // Setup debug output
            if (config.enableDebugOutput() && capabilities.debugOutput()) {
                setupDebugOutput();
            }

            // Initialize query pool
            if (config.enableProfiling() && capabilities.timerQuery()) {
                initializeQueryPool();
            }

            // Initialize VAO pool
            initializeVAOPool();

            // Initialize FBO pool
            initializeFBOPool();

            // Initialize staging buffer
            initializeStagingBuffer();

            // Enable clip control if available (better depth precision)
            if (capabilities.clipControl()) {
                glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE);
            }

            initialized = true;

            Astralis.LOGGER.info("[OpenGL] Backend initialized: {} ({})",
                glVersion.versionString(), glVersion.rendererString());
            logCapabilities();

            return true;

        } catch (Exception e) {
            Astralis.LOGGER.error("[OpenGL] Initialization failed", e);
            return false;
        }
    }

    private void setupDebugOutput() {
        glEnable(GL_DEBUG_OUTPUT);
        if (config.enableDebugSync()) {
            glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
        }

        glDebugMessageCallback((source, type, id, severity, length, message, userParam) -> {
            String msg = org.lwjgl.system.MemoryUtil.memUTF8(message, length);

            DebugMessage debugMsg = new DebugMessage(source, type, id, severity, msg, Instant.now());

            // Keep limited history
            if (debugMessages.size() >= MAX_DEBUG_MESSAGES) {
                debugMessages.poll();
            }
            debugMessages.offer(debugMsg);

            // Log based on severity
            switch (severity) {
                case GL_DEBUG_SEVERITY_HIGH ->
                    Astralis.LOGGER.error("[GL] {}: {}", debugMsg.typeString(), msg);
                case GL_DEBUG_SEVERITY_MEDIUM ->
                    Astralis.LOGGER.warn("[GL] {}: {}", debugMsg.typeString(), msg);
                case GL_DEBUG_SEVERITY_LOW ->
                    Astralis.LOGGER.info("[GL] {}: {}", debugMsg.typeString(), msg);
                case GL_DEBUG_SEVERITY_NOTIFICATION ->
                    Astralis.LOGGER.debug("[GL] {}: {}", debugMsg.typeString(), msg);
            }
        }, 0);

        // Disable low-severity notifications by default
        glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DEBUG_SEVERITY_NOTIFICATION,
            (IntBuffer) null, false);
    }

    private void initializeQueryPool() {
        for (int i = 0; i < config.queryPoolSize(); i++) {
            int query = glGenQueries();
            timerQueryPool.offer(query);
        }
    }

    private void initializeVAOPool() {
        for (int i = 0; i < config.vaoPoolSize(); i++) {
            int vao = glGenVertexArrays();
            vaoPool.offer(vao);
        }
    }

    private void initializeFBOPool() {
        for (int i = 0; i < config.fboPoolSize(); i++) {
            int fbo = glGenFramebuffers();
            fboPool.offer(fbo);
        }
    }

    private void initializeStagingBuffer() {
        if (capabilities.persistentMapping() && config.enablePersistentMapping()) {
            stagingBuffer = glGenBuffers();
            glBindBuffer(GL_COPY_READ_BUFFER, stagingBuffer);

            int flags = GLStorageFlags.MAP_WRITE | GLStorageFlags.MAP_PERSISTENT | GLStorageFlags.MAP_COHERENT;
            glBufferStorage(GL_COPY_READ_BUFFER, config.stagingBufferSize(), flags);

            ByteBuffer mapped = glMapBufferRange(GL_COPY_READ_BUFFER, 0, config.stagingBufferSize(),
                GLMapFlags.WRITE | GLMapFlags.PERSISTENT | GLMapFlags.COHERENT);

            if (mapped != null) {
                stagingMemory = MemorySegment.ofBuffer(mapped);
            }

            glBindBuffer(GL_COPY_READ_BUFFER, 0);
        }
    }

    private void logCapabilities() {
        Astralis.LOGGER.info("[OpenGL] Capabilities:");
        Astralis.LOGGER.info("  Multi-draw indirect: {}", capabilities.multiDrawIndirect());
        Astralis.LOGGER.info("  Indirect count: {}", capabilities.indirectCount());
        Astralis.LOGGER.info("  Compute shaders: {}", capabilities.computeShaders());
        Astralis.LOGGER.info("  Mesh shaders: {}", capabilities.meshShaders());
        Astralis.LOGGER.info("  Persistent mapping: {}", capabilities.persistentMapping());
        Astralis.LOGGER.info("  DSA: {}", capabilities.directStateAccess());
        Astralis.LOGGER.info("  Bindless textures: {}", capabilities.bindlessTextures());
        Astralis.LOGGER.info("  SPIR-V: {}", capabilities.spirvShaders());
        Astralis.LOGGER.info("  Max texture size: {}", capabilities.maxTextureSize());
        Astralis.LOGGER.info("  Max SSBO size: {} MB", capabilities.maxSSBOSize() / (1024 * 1024));
        Astralis.LOGGER.info("  Max compute work group: {}x{}x{}",
            capabilities.maxComputeWorkGroupSizeX(),
            capabilities.maxComputeWorkGroupSizeY(),
            capabilities.maxComputeWorkGroupSizeZ());
    }

    // ========================================================================
    // GPUBACKEND INTERFACE - BASIC
    // ========================================================================

    @Override
    public Type getType() {
        return Type.OPENGL;
    }

    @Override
    public String getVersionString() {
        return glVersion != null ? glVersion.versionString() : "Not initialized";
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Get OpenGL version info.
     */
    public GLVersion getGLVersion() {
        return glVersion;
    }

    /**
     * Get capabilities.
     */
    public Capabilities getCapabilities() {
        return capabilities;
    }

    /**
     * Check if DSA is available and enabled.
     */
    public boolean useDSA() {
        return capabilities != null && capabilities.directStateAccess() && config.enableDSA();
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("OpenGLBackend not initialized");
        }
        if (closed) {
            throw new IllegalStateException("OpenGLBackend is closed");
        }
    }

    // ========================================================================
    // BUFFER OPERATIONS
    // ========================================================================

    @Override
    public long createBuffer(long size, int usage, int memoryFlags) {
        checkInitialized();

        int handle;
        int glUsage = translateUsageFlags(usage, memoryFlags);
        boolean useImmutable = capabilities.immutableStorage() && config.enablePersistentMapping();
        boolean persistent = (memoryFlags & MemoryFlags.PERSISTENT) != 0;

        if (useDSA()) {
            // Direct State Access path
            handle = glCreateBuffers();

            if (useImmutable && persistent) {
                int storageFlags = GLStorageFlags.MAP_WRITE | GLStorageFlags.MAP_PERSISTENT | GLStorageFlags.MAP_COHERENT;
                if ((memoryFlags & MemoryFlags.HOST_VISIBLE) != 0) {
                    storageFlags |= GLStorageFlags.MAP_READ;
                }
                if ((usage & BufferUsage.TRANSFER_DST) != 0) {
                    storageFlags |= GLStorageFlags.DYNAMIC_STORAGE;
                }
                glNamedBufferStorage(handle, size, storageFlags);
            } else if (useImmutable) {
                int storageFlags = GLStorageFlags.DYNAMIC_STORAGE;
                glNamedBufferStorage(handle, size, storageFlags);
            } else {
                glNamedBufferData(handle, size, glUsage);
            }
        } else {
            // Traditional path
            handle = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, handle);

            if (useImmutable && persistent) {
                int storageFlags = GLStorageFlags.MAP_WRITE | GLStorageFlags.MAP_PERSISTENT | GLStorageFlags.MAP_COHERENT;
                if ((memoryFlags & MemoryFlags.HOST_VISIBLE) != 0) {
                    storageFlags |= GLStorageFlags.MAP_READ;
                }
                glBufferStorage(GL_ARRAY_BUFFER, size, storageFlags);
            } else if (useImmutable) {
                glBufferStorage(GL_ARRAY_BUFFER, size, GLStorageFlags.DYNAMIC_STORAGE);
            } else {
                glBufferData(GL_ARRAY_BUFFER, size, glUsage);
            }

            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        // Track resource
        BufferResource resource = new BufferResource(
            handle, size, usage, glUsage, null, useImmutable, null
        );
        bufferResources.put(handle, resource);
        totalBufferMemory.addAndGet(size);

        return handle;
    }

    /**
     * Create buffer with debug name.
     */
    public long createBuffer(long size, int usage, int memoryFlags, String debugName) {
        long buffer = createBuffer(size, usage, memoryFlags);
        if (buffer != 0 && debugName != null) {
            setObjectLabel(GL_BUFFER, (int) buffer, debugName);

            // Update tracked resource
            BufferResource old = bufferResources.get((int) buffer);
            if (old != null) {
                bufferResources.put((int) buffer, new BufferResource(
                    old.handle(), old.size(), old.usage(), old.storageFlags(),
                    old.mapping(), old.immutable(), debugName
                ));
            }
        }
        return buffer;
    }

    /**
     * Create buffer resource with full control.
     */
    public BufferResource createBufferResource(long size, int usage, int memoryFlags, String debugName) {
        checkInitialized();

        int handle;
        int glUsage = translateUsageFlags(usage, memoryFlags);
        boolean useImmutable = capabilities.immutableStorage() && config.enablePersistentMapping();
        boolean persistent = (memoryFlags & MemoryFlags.PERSISTENT) != 0;
        int storageFlags = 0;

        if (useDSA()) {
            handle = glCreateBuffers();

            if (useImmutable) {
                storageFlags = GLStorageFlags.DYNAMIC_STORAGE;
                if (persistent) {
                    storageFlags |= GLStorageFlags.MAP_WRITE | GLStorageFlags.MAP_PERSISTENT | GLStorageFlags.MAP_COHERENT;
                    if ((memoryFlags & MemoryFlags.HOST_VISIBLE) != 0) {
                        storageFlags |= GLStorageFlags.MAP_READ;
                    }
                }
                glNamedBufferStorage(handle, size, storageFlags);
            } else {
                glNamedBufferData(handle, size, glUsage);
            }
        } else {
            handle = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, handle);

            if (useImmutable) {
                storageFlags = GLStorageFlags.DYNAMIC_STORAGE;
                if (persistent) {
                    storageFlags |= GLStorageFlags.MAP_WRITE | GLStorageFlags.MAP_PERSISTENT | GLStorageFlags.MAP_COHERENT;
                }
                glBufferStorage(GL_ARRAY_BUFFER, size, storageFlags);
            } else {
                glBufferData(GL_ARRAY_BUFFER, size, glUsage);
            }

            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        // Set debug name
        if (debugName != null && capabilities.debugOutput()) {
            setObjectLabel(GL_BUFFER, handle, debugName);
        }

        // Auto-map if persistent
        MappedBufferInfo mapping = null;
        if (persistent && useImmutable) {
            mapping = mapBufferPersistentInternal(handle, 0, size);
        }

        BufferResource resource = new BufferResource(
            handle, size, usage, storageFlags, mapping, useImmutable, debugName
        );
        bufferResources.put(handle, resource);
        totalBufferMemory.addAndGet(size);

        return resource;
    }

    private int translateUsageFlags(int usage, int memoryFlags) {
        boolean dynamic = (memoryFlags & MemoryFlags.HOST_VISIBLE) != 0;
        boolean stream = (memoryFlags & MemoryFlags.TRANSIENT) != 0;
        boolean read = (usage & BufferUsage.TRANSFER_SRC) != 0;

        if (stream) {
            return read ? GL_STREAM_READ : GL_STREAM_DRAW;
        } else if (dynamic) {
            return read ? GL_DYNAMIC_READ : GL_DYNAMIC_DRAW;
        } else {
            return read ? GL_STATIC_READ : GL_STATIC_DRAW;
        }
    }

    @Override
    public void destroyBuffer(long buffer) {
        if (buffer == 0) return;

        int handle = (int) buffer;

        // Unmap if mapped
        MappedBufferInfo mapping = mappedBuffers.remove(buffer);
        if (mapping != null && !mapping.persistent()) {
            if (useDSA()) {
                glUnmapNamedBuffer(handle);
            } else {
                glBindBuffer(GL_ARRAY_BUFFER, handle);
                glUnmapBuffer(GL_ARRAY_BUFFER);
                glBindBuffer(GL_ARRAY_BUFFER, 0);
            }
        }

        // Update stats
        BufferResource resource = bufferResources.remove(handle);
        if (resource != null) {
            totalBufferMemory.addAndGet(-resource.size());
        }

        glDeleteBuffers(handle);
    }

    @Override
    public void bufferUpload(long buffer, long offset, ByteBuffer data) {
        checkInitialized();

        int handle = (int) buffer;
        BufferResource resource = bufferResources.get(handle);

        // If persistently mapped, use direct copy
        if (resource != null && resource.mapping() != null) {
            MemorySegment dst = resource.mapping().mappedSegment().asSlice(offset, data.remaining());
            MemorySegment src = MemorySegment.ofBuffer(data);
            dst.copyFrom(src);
            bufferUploads.increment();
            return;
        }

        // Check for existing mapping
        MappedBufferInfo mapping = mappedBuffers.get(buffer);
        if (mapping != null && mapping.persistent()) {
            MemorySegment dst = mapping.mappedSegment().asSlice(offset - mapping.offset(), data.remaining());
            MemorySegment src = MemorySegment.ofBuffer(data);
            dst.copyFrom(src);
            bufferUploads.increment();
            return;
        }

        // Use subdata
        if (useDSA()) {
            glNamedBufferSubData(handle, offset, data);
        } else {
            glBindBuffer(GL_ARRAY_BUFFER, handle);
            glBufferSubData(GL_ARRAY_BUFFER, offset, data);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        bufferUploads.increment();
    }

    /**
     * Upload buffer data using MemorySegment.
     */
    public void bufferUpload(long buffer, long offset, MemorySegment data) {
        bufferUpload(buffer, offset, data.asByteBuffer());
    }

    /**
     * Upload via staging buffer (for device-local buffers).
     */
    public void bufferUploadStaged(long buffer, long offset, ByteBuffer data) {
        if (stagingMemory == null) {
            bufferUpload(buffer, offset, data);
            return;
        }

        int size = data.remaining();
        long stagingOff = stagingOffset.getAndAdd(size);

        // Reset if wrapping
        if (stagingOff + size > config.stagingBufferSize()) {
            waitForFrame(currentFrame.get());
            stagingOffset.set(0);
            stagingOff = stagingOffset.getAndAdd(size);
        }

        // Copy to staging
        MemorySegment dst = stagingMemory.asSlice(stagingOff, size);
        MemorySegment src = MemorySegment.ofBuffer(data);
        dst.copyFrom(src);

        // Copy from staging to target
        if (useDSA()) {
            glCopyNamedBufferSubData(stagingBuffer, (int) buffer, stagingOff, offset, size);
        } else {
            glBindBuffer(GL_COPY_READ_BUFFER, stagingBuffer);
            glBindBuffer(GL_COPY_WRITE_BUFFER, (int) buffer);
            glCopyBufferSubData(GL_COPY_READ_BUFFER, GL_COPY_WRITE_BUFFER, stagingOff, offset, size);
            glBindBuffer(GL_COPY_READ_BUFFER, 0);
            glBindBuffer(GL_COPY_WRITE_BUFFER, 0);
        }

        bufferUploads.increment();
    }

    @Override
    public ByteBuffer mapBuffer(long buffer, long offset, long size) {
        checkInitialized();

        int handle = (int) buffer;

        // Check existing mapping
        MappedBufferInfo existing = mappedBuffers.get(buffer);
        if (existing != null) {
            return existing.mappedBuffer();
        }

        // Check persistent mapping in resource
        BufferResource resource = bufferResources.get(handle);
        if (resource != null && resource.mapping() != null) {
            return resource.mapping().mappedBuffer();
        }

        ByteBuffer mapped;
        int accessFlags = GLMapFlags.WRITE | GLMapFlags.INVALIDATE_RANGE;

        if (useDSA()) {
            mapped = glMapNamedBufferRange(handle, offset, size, accessFlags);
        } else {
            glBindBuffer(GL_ARRAY_BUFFER, handle);
            mapped = glMapBufferRange(GL_ARRAY_BUFFER, offset, size, accessFlags);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        if (mapped != null) {
            MappedBufferInfo info = new MappedBufferInfo(
                buffer, GL_ARRAY_BUFFER,
                MemorySegment.ofBuffer(mapped), mapped,
                offset, size, accessFlags, false, currentFrame.get()
            );
            mappedBuffers.put(buffer, info);
        }

        return mapped;
    }

    /**
     * Map buffer with specific access flags.
     */
    public ByteBuffer mapBuffer(long buffer, long offset, long size, int accessFlags) {
        checkInitialized();

        int handle = (int) buffer;
        ByteBuffer mapped;

        if (useDSA()) {
            mapped = glMapNamedBufferRange(handle, offset, size, accessFlags);
        } else {
            glBindBuffer(GL_ARRAY_BUFFER, handle);
            mapped = glMapBufferRange(GL_ARRAY_BUFFER, offset, size, accessFlags);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        if (mapped != null) {
            boolean persistent = (accessFlags & GLMapFlags.PERSISTENT) != 0;
            MappedBufferInfo info = new MappedBufferInfo(
                buffer, GL_ARRAY_BUFFER,
                MemorySegment.ofBuffer(mapped), mapped,
                offset, size, accessFlags, persistent, currentFrame.get()
            );
            mappedBuffers.put(buffer, info);
        }

        return mapped;
    }

    /**
     * Map buffer persistently.
     */
    public MemorySegment mapBufferPersistent(long buffer, long offset, long size) {
        MappedBufferInfo info = mapBufferPersistentInternal((int) buffer, offset, size);
        return info != null ? info.mappedSegment() : null;
    }

    private MappedBufferInfo mapBufferPersistentInternal(int handle, long offset, long size) {
        if (!capabilities.persistentMapping()) {
            return null;
        }

        int accessFlags = GLMapFlags.WRITE | GLMapFlags.PERSISTENT | GLMapFlags.COHERENT;

        ByteBuffer mapped;
        if (useDSA()) {
            mapped = glMapNamedBufferRange(handle, offset, size, accessFlags);
        } else {
            glBindBuffer(GL_ARRAY_BUFFER, handle);
            mapped = glMapBufferRange(GL_ARRAY_BUFFER, offset, size, accessFlags);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }

        if (mapped != null) {
            MappedBufferInfo info = new MappedBufferInfo(
                handle, GL_ARRAY_BUFFER,
                MemorySegment.ofBuffer(mapped), mapped,
                offset, size, accessFlags, true, currentFrame.get()
            );
            mappedBuffers.put((long) handle, info);
            return info;
        }

        return null;
    }

    @Override
    public void unmapBuffer(long buffer) {
        MappedBufferInfo info = mappedBuffers.get(buffer);
        if (info == null) return;

        // Don't unmap persistent buffers
        if (info.persistent()) {
            return;
        }

        int handle = (int) buffer;
        if (useDSA()) {
            glUnmapNamedBuffer(handle);
        } else {
            glBindBuffer(info.target(), handle);
            glUnmapBuffer(info.target());
            glBindBuffer(info.target(), 0);
        }

        mappedBuffers.remove(buffer);
    }

    /**
     * Flush mapped buffer range.
     */
    public void flushMappedBufferRange(long buffer, long offset, long size) {
        MappedBufferInfo info = mappedBuffers.get(buffer);
        if (info == null) return;

        if ((info.accessFlags() & GLMapFlags.FLUSH_EXPLICIT) != 0) {
            int handle = (int) buffer;
            if (useDSA()) {
                glFlushMappedNamedBufferRange(handle, offset, size);
            } else {
                glBindBuffer(info.target(), handle);
                glFlushMappedBufferRange(info.target(), offset, size);
                glBindBuffer(info.target(), 0);
            }
        }
    }

    @Override
    public long getBufferDeviceAddress(long buffer) {
        // OpenGL has limited support via NV_shader_buffer_load
        if (!capabilities.bufferDeviceAddress()) {
            return 0;
        }

        // Would need to use glMakeBufferResidentNV and glGetBufferParameterui64vNV
        // This is vendor-specific, so return 0 for now
        return 0;
    }

    /**
     * Get buffer resource info.
     */
    public Optional<BufferResource> getBufferResource(long buffer) {
        return Optional.ofNullable(bufferResources.get((int) buffer));
    }

    /**
     * Invalidate buffer data (hint to driver).
     */
    public void invalidateBufferData(long buffer) {
        if (glVersion.atLeast(4, 3)) {
            if (useDSA()) {
                glInvalidateBufferData((int) buffer);
            } else {
                // No non-DSA equivalent
            }
        }
    }

    /**
     * Invalidate buffer subdata.
     */
    public void invalidateBufferSubData(long buffer, long offset, long length) {
        if (glVersion.atLeast(4, 3)) {
            if (useDSA()) {
                glInvalidateBufferSubData((int) buffer, offset, length);
            }
        }
    }

    // ========================================================================
    // TEXTURE OPERATIONS
    // ========================================================================

    @Override
    public long createTexture2D(int width, int height, int format, int mipLevels) {
        checkInitialized();

        int handle;
        int glInternalFormat = translateInternalFormat(format);

        if (useDSA()) {
            handle = glCreateTextures(GL_TEXTURE_2D);
            glTextureStorage2D(handle, mipLevels, glInternalFormat, width, height);

            // Set default sampling parameters
            glTextureParameteri(handle, GL_TEXTURE_MIN_FILTER,
                mipLevels > 1 ? GL_LINEAR_MIPMAP_LINEAR : GL_LINEAR);
            glTextureParameteri(handle, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTextureParameteri(handle, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTextureParameteri(handle, GL_TEXTURE_WRAP_T, GL_REPEAT);

            // Set max anisotropy if available
            if (capabilities.anisotropicFiltering()) {
                glTextureParameterf(handle, GL_TEXTURE_MAX_ANISOTROPY,
                    Math.min(8.0f, capabilities.maxAnisotropy()));
            }
        } else {
            handle = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, handle);

            if (capabilities.textureStorage()) {
                glTexStorage2D(GL_TEXTURE_2D, mipLevels, glInternalFormat, width, height);
            } else {
                int glFormat = translateFormat(format);
                int glType = translateType(format);
                for (int i = 0; i < mipLevels; i++) {
                    int w = Math.max(1, width >> i);
                    int h = Math.max(1, height >> i);
                    glTexImage2D(GL_TEXTURE_2D, i, glInternalFormat, w, h, 0, glFormat, glType, (ByteBuffer) null);
                }
            }

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,
                mipLevels > 1 ? GL_LINEAR_MIPMAP_LINEAR : GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

            if (capabilities.anisotropicFiltering()) {
                glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY,
                    Math.min(8.0f, capabilities.maxAnisotropy()));
            }

            glBindTexture(GL_TEXTURE_2D, 0);
        }

        // Track resource
        long estimatedSize = (long) width * height * getFormatBytesPerPixel(format);
        TextureResource resource = new TextureResource(
            handle, GL_TEXTURE_2D, width, height, 1,
            glInternalFormat, mipLevels, 1, 1, 0, false, null
        );
        textureResources.put(handle, resource);
        totalTextureMemory.addAndGet(estimatedSize);

        return handle;
    }

    /**
     * Create texture with debug name.
     */
    public long createTexture2D(int width, int height, int format, int mipLevels, String debugName) {
        long texture = createTexture2D(width, height, format, mipLevels);
        if (texture != 0 && debugName != null) {
            setObjectLabel(GL_TEXTURE, (int) texture, debugName);
        }
        return texture;
    }

    /**
     * Create texture resource with full options.
     */
    public TextureResource createTextureResource(
            int target, int width, int height, int depth,
            int format, int mipLevels, int arrayLayers, int samples,
            String debugName) {

        checkInitialized();

        int handle;
        int glInternalFormat = translateInternalFormat(format);

        if (useDSA()) {
            handle = glCreateTextures(target);

            switch (target) {
                case GL_TEXTURE_2D -> glTextureStorage2D(handle, mipLevels, glInternalFormat, width, height);
                case GL_TEXTURE_3D -> glTextureStorage3D(handle, mipLevels, glInternalFormat, width, height, depth);
                case GL_TEXTURE_2D_ARRAY -> glTextureStorage3D(handle, mipLevels, glInternalFormat, width, height, arrayLayers);
                case GL_TEXTURE_CUBE_MAP -> glTextureStorage2D(handle, mipLevels, glInternalFormat, width, height);
                case GL_TEXTURE_2D_MULTISAMPLE -> glTextureStorage2DMultisample(handle, samples, glInternalFormat, width, height, true);
                default -> throw new IllegalArgumentException("Unsupported texture target: " + target);
            }

            // Default parameters
            if (target != GL_TEXTURE_2D_MULTISAMPLE) {
                glTextureParameteri(handle, GL_TEXTURE_MIN_FILTER,
                    mipLevels > 1 ? GL_LINEAR_MIPMAP_LINEAR : GL_LINEAR);
                glTextureParameteri(handle, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            }
        } else {
            handle = glGenTextures();
            glBindTexture(target, handle);

            switch (target) {
                case GL_TEXTURE_2D -> glTexStorage2D(target, mipLevels, glInternalFormat, width, height);
                case GL_TEXTURE_3D -> glTexStorage3D(target, mipLevels, glInternalFormat, width, height, depth);
                case GL_TEXTURE_2D_ARRAY -> glTexStorage3D(target, mipLevels, glInternalFormat, width, height, arrayLayers);
                case GL_TEXTURE_CUBE_MAP -> glTexStorage2D(target, mipLevels, glInternalFormat, width, height);
                case GL_TEXTURE_2D_MULTISAMPLE -> glTexStorage2DMultisample(target, samples, glInternalFormat, width, height, true);
            }

            if (target != GL_TEXTURE_2D_MULTISAMPLE) {
                glTexParameteri(target, GL_TEXTURE_MIN_FILTER,
                    mipLevels > 1 ? GL_LINEAR_MIPMAP_LINEAR : GL_LINEAR);
                glTexParameteri(target, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            }

            glBindTexture(target, 0);
        }

        // Debug name
        if (debugName != null && capabilities.debugOutput()) {
            setObjectLabel(GL_TEXTURE, handle, debugName);
        }

        // Make bindless if supported
        long bindlessHandle = 0;
        boolean isBindless = false;
        if (capabilities.bindlessTextures() && config.enableBindlessTextures()) {
            bindlessHandle = glGetTextureHandleARB(handle);
            if (bindlessHandle != 0) {
                glMakeTextureHandleResidentARB(bindlessHandle);
                isBindless = true;
            }
        }

        TextureResource resource = new TextureResource(
            handle, target, width, height, depth,
            glInternalFormat, mipLevels, arrayLayers, samples,
            bindlessHandle, isBindless, debugName
        );
        textureResources.put(handle, resource);

        long estimatedSize = (long) width * height * depth * arrayLayers * getFormatBytesPerPixel(format);
        totalTextureMemory.addAndGet(estimatedSize);

        return resource;
    }

    private int translateFormat(int format) {
        return switch (format) {
            case TextureFormat.R8 -> GL_RED;
            case TextureFormat.RG8 -> GL_RG;
            case TextureFormat.RGB8 -> GL_RGB;
            case TextureFormat.RGBA8, TextureFormat.RGBA16F, TextureFormat.RGBA32F -> GL_RGBA;
            case TextureFormat.DEPTH24_STENCIL8 -> GL_DEPTH_STENCIL;
            case TextureFormat.DEPTH32F -> GL_DEPTH_COMPONENT;
            case TextureFormat.R32UI -> GL_RED_INTEGER;
            case TextureFormat.RG32F -> GL_RG;
            case TextureFormat.R16F -> GL_RED;
            case TextureFormat.R32F -> GL_RED;
            default -> GL_RGBA;
        };
    }

    private int translateInternalFormat(int format) {
        return switch (format) {
            case TextureFormat.R8 -> GL_R8;
            case TextureFormat.RG8 -> GL_RG8;
            case TextureFormat.RGB8 -> GL_RGB8;
            case TextureFormat.RGBA8 -> GL_RGBA8;
            case TextureFormat.RGBA16F -> GL_RGBA16F;
            case TextureFormat.RGBA32F -> GL_RGBA32F;
            case TextureFormat.DEPTH24_STENCIL8 -> GL_DEPTH24_STENCIL8;
            case TextureFormat.DEPTH32F -> GL_DEPTH_COMPONENT32F;
            case TextureFormat.R32UI -> GL_R32UI;
            case TextureFormat.RG32F -> GL_RG32F;
            case TextureFormat.R16F -> GL_R16F;
            case TextureFormat.R32F -> GL_R32F;
            case TextureFormat.SRGB8_ALPHA8 -> GL_SRGB8_ALPHA8;
            default -> GL_RGBA8;
        };
    }

    private int translateType(int format) {
        return switch (format) {
            case TextureFormat.RGBA8, TextureFormat.RGB8, TextureFormat.RG8, TextureFormat.R8 -> GL_UNSIGNED_BYTE;
            case TextureFormat.RGBA16F, TextureFormat.RGBA32F, TextureFormat.RG32F, TextureFormat.R16F, TextureFormat.R32F -> GL_FLOAT;
            case TextureFormat.DEPTH24_STENCIL8 -> GL_UNSIGNED_INT_24_8;
            case TextureFormat.DEPTH32F -> GL_FLOAT;
            case TextureFormat.R32UI -> GL_UNSIGNED_INT;
            default -> GL_UNSIGNED_BYTE;
        };
    }

    private int getFormatBytesPerPixel(int format) {
        return switch (format) {
            case TextureFormat.R8 -> 1;
            case TextureFormat.RG8 -> 2;
            case TextureFormat.RGB8 -> 3;
            case TextureFormat.RGBA8 -> 4;
            case TextureFormat.RGBA16F -> 8;
            case TextureFormat.RGBA32F -> 16;
            case TextureFormat.RG32F -> 8;
            case TextureFormat.R32F, TextureFormat.R32UI -> 4;
            case TextureFormat.R16F -> 2;
            case TextureFormat.DEPTH32F -> 4;
            case TextureFormat.DEPTH24_STENCIL8 -> 4;
            default -> 4;
        };
    }

    @Override
    public void destroyTexture(long texture) {
        if (texture == 0) return;

        int handle = (int) texture;

        // Make non-resident if bindless
        TextureResource resource = textureResources.remove(handle);
        if (resource != null) {
            if (resource.isBindless() && resource.bindlessHandle() != 0) {
                glMakeTextureHandleNonResidentARB(resource.bindlessHandle());
            }
            totalTextureMemory.addAndGet(
                -(long) resource.width() * resource.height() * resource.depth() * 4);
        }

        glDeleteTextures(handle);
    }

    @Override
    public void textureUpload(long texture, int level, int x, int y, int width, int height, ByteBuffer data) {
        checkInitialized();

        int handle = (int) texture;
        TextureResource resource = textureResources.get(handle);
        int format = resource != null ? translateFormatForUpload(resource.internalFormat()) : GL_RGBA;
        int type = resource != null ? translateTypeForUpload(resource.internalFormat()) : GL_UNSIGNED_BYTE;

        if (useDSA()) {
            glTextureSubImage2D(handle, level, x, y, width, height, format, type, data);
        } else {
            glBindTexture(GL_TEXTURE_2D, handle);
            glTexSubImage2D(GL_TEXTURE_2D, level, x, y, width, height, format, type, data);
            glBindTexture(GL_TEXTURE_2D, 0);
        }

        textureUploads.increment();
    }

    private int translateFormatForUpload(int internalFormat) {
        return switch (internalFormat) {
            case GL_R8, GL_R16F, GL_R32F -> GL_RED;
            case GL_RG8, GL_RG16F, GL_RG32F -> GL_RG;
            case GL_RGB8, GL_SRGB8 -> GL_RGB;
            case GL_RGBA8, GL_SRGB8_ALPHA8, GL_RGBA16F, GL_RGBA32F -> GL_RGBA;
            case GL_R32UI -> GL_RED_INTEGER;
            case GL_DEPTH_COMPONENT32F -> GL_DEPTH_COMPONENT;
            case GL_DEPTH24_STENCIL8 -> GL_DEPTH_STENCIL;
            default -> GL_RGBA;
        };
    }

    private int translateTypeForUpload(int internalFormat) {
        return switch (internalFormat) {
            case GL_R8, GL_RG8, GL_RGB8, GL_RGBA8, GL_SRGB8, GL_SRGB8_ALPHA8 -> GL_UNSIGNED_BYTE;
            case GL_R16F, GL_RG16F, GL_RGBA16F -> GL_HALF_FLOAT;
            case GL_R32F, GL_RG32F, GL_RGBA32F, GL_DEPTH_COMPONENT32F -> GL_FLOAT;
            case GL_R32UI -> GL_UNSIGNED_INT;
            case GL_DEPTH24_STENCIL8 -> GL_UNSIGNED_INT_24_8;
            default -> GL_UNSIGNED_BYTE;
        };
    }

    /**
     * Generate mipmaps for texture.
     */
    public void generateMipmaps(long texture) {
        int handle = (int) texture;
        TextureResource resource = textureResources.get(handle);
        int target = resource != null ? resource.target() : GL_TEXTURE_2D;

        if (useDSA()) {
            glGenerateTextureMipmap(handle);
        } else {
            glBindTexture(target, handle);
            glGenerateMipmap(target);
            glBindTexture(target, 0);
        }
    }

    /**
     * Bind texture to unit.
     */
    public void bindTexture(int unit, long texture) {
        int handle = (int) texture;

        if (currentTextures[unit] == handle) return;
        currentTextures[unit] = handle;

        if (useDSA()) {
            glBindTextureUnit(unit, handle);
        } else {
            glActiveTexture(GL_TEXTURE0 + unit);
            TextureResource resource = textureResources.get(handle);
            int target = resource != null ? resource.target() : GL_TEXTURE_2D;
            glBindTexture(target, handle);
        }

        textureBinds.increment();
    }

    /**
     * Bind multiple textures.
     */
    public void bindTextures(int firstUnit, int[] textures) {
        if (capabilities.multiBinds()) {
            glBindTextures(firstUnit, textures);
            for (int i = 0; i < textures.length; i++) {
                currentTextures[firstUnit + i] = textures[i];
            }
            textureBinds.add(textures.length);
        } else {
            for (int i = 0; i < textures.length; i++) {
                bindTexture(firstUnit + i, textures[i]);
            }
        }
    }

    /**
     * Get bindless texture handle.
     */
    public long getBindlessHandle(long texture) {
        TextureResource resource = textureResources.get((int) texture);
        return resource != null ? resource.bindlessHandle() : 0;
    }

    /**
     * Get texture resource info.
     */
    public Optional<TextureResource> getTextureResource(long texture) {
        return Optional.ofNullable(textureResources.get((int) texture));
    }

    // ========================================================================
    // SAMPLER OPERATIONS
    // ========================================================================

    /**
     * Create sampler.
     */
    public int createSampler(int minFilter, int magFilter, int wrapS, int wrapT, float anisotropy) {
        int sampler = glGenSamplers();

        glSamplerParameteri(sampler, GL_TEXTURE_MIN_FILTER, minFilter);
        glSamplerParameteri(sampler, GL_TEXTURE_MAG_FILTER, magFilter);
        glSamplerParameteri(sampler, GL_TEXTURE_WRAP_S, wrapS);
        glSamplerParameteri(sampler, GL_TEXTURE_WRAP_T, wrapT);

        if (anisotropy > 1.0f && capabilities.anisotropicFiltering()) {
            glSamplerParameterf(sampler, GL_TEXTURE_MAX_ANISOTROPY,
                Math.min(anisotropy, capabilities.maxAnisotropy()));
        }

        SamplerResource resource = new SamplerResource(
            sampler, minFilter, magFilter, wrapS, wrapT, GL_REPEAT, anisotropy, null
        );
        samplerResources.put(sampler, resource);

        return sampler;
    }

    /**
     * Bind sampler to unit.
     */
    public void bindSampler(int unit, int sampler) {
        if (currentSamplers[unit] == sampler) return;
        currentSamplers[unit] = sampler;
        glBindSampler(unit, sampler);
    }

    /**
     * Bind multiple samplers.
     */
    public void bindSamplers(int firstUnit, int[] samplers) {
        if (capabilities.multiBinds()) {
            glBindSamplers(firstUnit, samplers);
            for (int i = 0; i < samplers.length; i++) {
                currentSamplers[firstUnit + i] = samplers[i];
            }
        } else {
            for (int i = 0; i < samplers.length; i++) {
                bindSampler(firstUnit + i, samplers[i]);
            }
        }
    }

    // ========================================================================
    // SHADER OPERATIONS
    // ========================================================================

    @Override
    public long createShader(int stage, String source) {
        checkInitialized();

        int glType = translateShaderStage(stage);
        int shader = glCreateShader(glType);

        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new ShaderCompilationException("Shader compilation failed: " + log);
        }

        ShaderResource resource = new ShaderResource(shader, glType, source, false, null);
        shaderResources.put(shader, resource);

        return shader;
    }

    /**
     * Create shader with debug name.
     */
    public long createShader(int stage, String source, String debugName) {
        long shader = createShader(stage, source);
        if (shader != 0 && debugName != null) {
            setObjectLabel(GL_SHADER, (int) shader, debugName);
        }
        return shader;
    }

    private int translateShaderStage(int stage) {
        if ((stage & ShaderStage.VERTEX) != 0) return GL_VERTEX_SHADER;
        if ((stage & ShaderStage.FRAGMENT) != 0) return GL_FRAGMENT_SHADER;
        if ((stage & ShaderStage.GEOMETRY) != 0) return GL_GEOMETRY_SHADER;
        if ((stage & ShaderStage.TESS_CONTROL) != 0) return GL_TESS_CONTROL_SHADER;
        if ((stage & ShaderStage.TESS_EVAL) != 0) return GL_TESS_EVALUATION_SHADER;
        if ((stage & ShaderStage.COMPUTE) != 0) return GL_COMPUTE_SHADER;
        return GL_VERTEX_SHADER;
    }

    @Override
    public long createShaderFromSPIRV(int stage, ByteBuffer spirv) {
        checkInitialized();

        if (!capabilities.spirvShaders()) {
            throw new UnsupportedOperationException("SPIR-V shaders require GL 4.6 or GL_ARB_gl_spirv");
        }

        int glType = translateShaderStage(stage);
        int shader = glCreateShader(glType);

        glShaderBinary(new int[]{shader}, GL_SHADER_BINARY_FORMAT_SPIR_V, spirv);
        glSpecializeShader(shader, "main", new int[0], new int[0]);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new ShaderCompilationException("SPIR-V shader specialization failed: " + log);
        }

        ShaderResource resource = new ShaderResource(shader, glType, null, true, null);
        shaderResources.put(shader, resource);

        return shader;
    }

    @Override
    public void destroyShader(long shader) {
        if (shader == 0) return;
        shaderResources.remove((int) shader);
        glDeleteShader((int) shader);
    }

    @Override
    public long createProgram(long... shaders) {
        checkInitialized();

        int program = glCreateProgram();

        for (long shader : shaders) {
            glAttachShader(program, (int) shader);
        }

        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            throw new ShaderCompilationException("Program linking failed: " + log);
        }

        // Detach shaders after linking
        for (long shader : shaders) {
            glDetachShader(program, (int) shader);
        }

        // Cache uniform locations
        Map<String, Integer> uniformLocations = new HashMap<>();
        Map<String, Integer> uniformBlockIndices = new HashMap<>();
        Map<String, Integer> ssboIndices = new HashMap<>();

        int numUniforms = glGetProgrami(program, GL_ACTIVE_UNIFORMS);
        for (int i = 0; i < numUniforms; i++) {
            String name = glGetActiveUniformName(program, i);
            int location = glGetUniformLocation(program, name);
            if (location >= 0) {
                uniformLocations.put(name, location);
            }
        }

        int numUBOs = glGetProgrami(program, GL_ACTIVE_UNIFORM_BLOCKS);
        for (int i = 0; i < numUBOs; i++) {
            String name = glGetActiveUniformBlockName(program, i);
            uniformBlockIndices.put(name, i);
        }

        // Get SSBO bindings if available
        if (capabilities.shaderStorageBufferObject()) {
            // Would need to query GL_SHADER_STORAGE_BLOCK resources
        }

        List<ShaderResource> shaderList = new ArrayList<>();
        for (long shader : shaders) {
            ShaderResource sr = shaderResources.get((int) shader);
            if (sr != null) shaderList.add(sr);
        }

        ProgramResource resource = new ProgramResource(
            program, shaderList, uniformLocations, uniformBlockIndices, ssboIndices, false, null
        );
        programResources.put(program, resource);

        return program;
    }

    /**
     * Create program with debug name.
     */
    public long createProgram(String debugName, long... shaders) {
        long program = createProgram(shaders);
        if (program != 0 && debugName != null) {
            setObjectLabel(GL_PROGRAM, (int) program, debugName);
        }
        return program;
    }

    @Override
    public void destroyProgram(long program) {
        if (program == 0) return;

        if (currentProgram == (int) program) {
            currentProgram = 0;
            glUseProgram(0);
        }

        programResources.remove((int) program);
        glDeleteProgram((int) program);
    }

    /**
     * Get cached uniform location.
     */
    public int getUniformLocation(long program, String name) {
        ProgramResource resource = programResources.get((int) program);
        if (resource != null) {
            Integer loc = resource.uniformLocations().get(name);
            if (loc != null) return loc;
        }
        return glGetUniformLocation((int) program, name);
    }

    /**
     * Set uniform value.
     */
    public void setUniform(long program, String name, float value) {
        int location = getUniformLocation(program, name);
        if (location >= 0) {
            if (useDSA()) {
                glProgramUniform1f((int) program, location, value);
            } else {
                bindProgram(program);
                glUniform1f(location, value);
            }
        }
    }

    public void setUniform(long program, String name, int value) {
        int location = getUniformLocation(program, name);
        if (location >= 0) {
            if (useDSA()) {
                glProgramUniform1i((int) program, location, value);
            } else {
                bindProgram(program);
                glUniform1i(location, value);
            }
        }
    }

    public void setUniform(long program, String name, float x, float y, float z) {
        int location = getUniformLocation(program, name);
        if (location >= 0) {
            if (useDSA()) {
                glProgramUniform3f((int) program, location, x, y, z);
            } else {
                bindProgram(program);
                glUniform3f(location, x, y, z);
            }
        }
    }

    public void setUniform(long program, String name, float x, float y, float z, float w) {
        int location = getUniformLocation(program, name);
        if (location >= 0) {
            if (useDSA()) {
                glProgramUniform4f((int) program, location, x, y, z, w);
            } else {
                bindProgram(program);
                glUniform4f(location, x, y, z, w);
            }
        }
    }

    public void setUniformMatrix4(long program, String name, boolean transpose, FloatBuffer matrix) {
        int location = getUniformLocation(program, name);
        if (location >= 0) {
            if (useDSA()) {
                glProgramUniformMatrix4fv((int) program, location, transpose, matrix);
            } else {
                bindProgram(program);
                glUniformMatrix4fv(location, transpose, matrix);
            }
        }
    }

    /**
     * Get program resource info.
     */
    public Optional<ProgramResource> getProgramResource(long program) {
        return Optional.ofNullable(programResources.get((int) program));
    }

    // ========================================================================
    // VAO OPERATIONS
    // ========================================================================

    /**
     * Create VAO.
     */
    public int createVAO() {
        Integer pooled = vaoPool.poll();
        if (pooled != null) {
            return pooled;
        }
        return glGenVertexArrays();
    }

    /**
     * Destroy VAO (returns to pool).
     */
    public void destroyVAO(int vao) {
        // Reset VAO state and return to pool
        glBindVertexArray(vao);
        for (int i = 0; i < capabilities.maxVertexAttribs(); i++) {
            glDisableVertexAttribArray(i);
        }
        glBindVertexArray(0);

        vaoResources.remove(vao);
        vaoPool.offer(vao);
    }

    /**
     * Bind VAO.
     */
    public void bindVAO(int vao) {
        if (currentVAO == vao) return;
        currentVAO = vao;
        glBindVertexArray(vao);
        vaoBinds.increment();
    }

    /**
     * Setup vertex attribute.
     */
    public void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long offset) {
        glVertexAttribPointer(index, size, type, normalized, stride, offset);
        glEnableVertexAttribArray(index);
    }

    /**
     * Setup vertex attribute with divisor.
     */
    public void vertexAttribDivisor(int index, int divisor) {
        glVertexAttribDivisor(index, divisor);
    }

    // ========================================================================
    // DRAW OPERATIONS
    // ========================================================================

    @Override
    public void bindVertexBuffer(int binding, long buffer, long offset, int stride) {
        if (glVersion.atLeast(4, 3)) {
            glBindVertexBuffer(binding, (int) buffer, offset, stride);
        } else {
            glBindBuffer(GL_ARRAY_BUFFER, (int) buffer);
        }
    }

    @Override
    public void bindIndexBuffer(long buffer, long offset, boolean is32Bit) {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, (int) buffer);
    }

    @Override
    public void bindProgram(long program) {
        int handle = (int) program;
        if (currentProgram == handle) return;
        currentProgram = handle;
        glUseProgram(handle);
        programBinds.increment();
    }

    @Override
    public void drawIndexed(int indexCount, int instanceCount, int firstIndex, int vertexOffset, int firstInstance) {
        long indexOffset = (long) firstIndex * 4;

        if (capabilities.baseInstance() && firstInstance != 0) {
            glDrawElementsInstancedBaseVertexBaseInstance(
                GL_TRIANGLES, indexCount, GL_UNSIGNED_INT,
                indexOffset, instanceCount, vertexOffset, firstInstance);
        } else if (instanceCount > 1 || vertexOffset != 0) {
            glDrawElementsInstancedBaseVertex(
                GL_TRIANGLES, indexCount, GL_UNSIGNED_INT,
                indexOffset, instanceCount, vertexOffset);
        } else if (instanceCount > 1) {
            glDrawElementsInstanced(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, indexOffset, instanceCount);
        } else {
            glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, indexOffset);
        }

        drawCalls.increment();
    }

    /**
     * Draw arrays (non-indexed).
     */
    public void draw(int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
        if (capabilities.baseInstance() && firstInstance != 0) {
            glDrawArraysInstancedBaseInstance(GL_TRIANGLES, firstVertex, vertexCount, instanceCount, firstInstance);
        } else if (instanceCount > 1) {
            glDrawArraysInstanced(GL_TRIANGLES, firstVertex, vertexCount, instanceCount);
        } else {
            glDrawArrays(GL_TRIANGLES, firstVertex, vertexCount);
        }

        drawCalls.increment();
    }

    @Override
    public void drawIndexedIndirect(long buffer, long offset, int drawCount, int stride) {
        if (!capabilities.multiDrawIndirect()) {
            throw new UnsupportedOperationException("Multi-draw indirect not supported");
        }

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, (int) buffer);
        glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_INT, offset, drawCount, stride);
        drawCalls.add(drawCount);
    }

    @Override
    public void drawIndexedIndirectCount(long commandBuffer, long commandOffset,
                                          long countBuffer, long countOffset,
                                          int maxDrawCount, int stride) {
        if (!capabilities.indirectCount()) {
            throw new UnsupportedOperationException("Indirect count not supported (requires GL 4.6)");
        }

        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, (int) commandBuffer);
        glBindBuffer(GL_PARAMETER_BUFFER, (int) countBuffer);

        glMultiDrawElementsIndirectCount(
            GL_TRIANGLES, GL_UNSIGNED_INT,
            commandOffset, countOffset, maxDrawCount, stride);

        drawCalls.increment();
    }

    /**
     * Draw mesh tasks (NV extension).
     */
    public void drawMeshTasks(int first, int count) {
        if (!capabilities.meshShaders()) {
            throw new UnsupportedOperationException("Mesh shaders not supported");
        }
        nglDrawMeshTasksNV(first, count);
        drawCalls.increment();
    }

    /**
     * Draw mesh tasks indirect.
     */
    public void drawMeshTasksIndirect(long buffer, long offset, int drawCount, int stride) {
        if (!capabilities.meshShaders()) {
            throw new UnsupportedOperationException("Mesh shaders not supported");
        }
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, (int) buffer);
        nglMultiDrawMeshTasksIndirectNV(offset, drawCount, stride);
        drawCalls.add(drawCount);
    }

    // ========================================================================
    // COMPUTE OPERATIONS
    // ========================================================================

    @Override
    public void bindComputeProgram(long program) {
        bindProgram(program);
    }

    @Override
    public void dispatchCompute(int groupsX, int groupsY, int groupsZ) {
        if (!capabilities.computeShaders()) {
            throw new UnsupportedOperationException("Compute shaders not supported");
        }
        glDispatchCompute(groupsX, groupsY, groupsZ);
        dispatchCalls.increment();
    }

    @Override
    public void dispatchComputeIndirect(long buffer, long offset) {
        if (!capabilities.computeShaders()) {
            throw new UnsupportedOperationException("Compute shaders not supported");
        }
        glBindBuffer(GL_DISPATCH_INDIRECT_BUFFER, (int) buffer);
        glDispatchComputeIndirect(offset);
        dispatchCalls.increment();
    }

    /**
     * Dispatch compute with automatic workgroup calculation.
     */
    public void dispatchCompute(int totalX, int totalY, int totalZ, int localX, int localY, int localZ) {
        int groupsX = (totalX + localX - 1) / localX;
        int groupsY = (totalY + localY - 1) / localY;
        int groupsZ = (totalZ + localZ - 1) / localZ;
        dispatchCompute(groupsX, groupsY, groupsZ);
    }

    // ========================================================================
    // UNIFORM BUFFER & SSBO BINDING
    // ========================================================================

    /**
     * Bind uniform buffer.
     */
    public void bindUniformBuffer(int binding, long buffer, long offset, long size) {
        int handle = (int) buffer;
        if (currentUBOs[binding] == handle) return;
        currentUBOs[binding] = handle;

        glBindBufferRange(GL_UNIFORM_BUFFER, binding, handle, offset, size);
    }

    /**
     * Bind shader storage buffer.
     */
    public void bindShaderStorageBuffer(int binding, long buffer, long offset, long size) {
        int handle = (int) buffer;
        if (currentSSBOs[binding] == handle) return;
        currentSSBOs[binding] = handle;

        glBindBufferRange(GL_SHADER_STORAGE_BUFFER, binding, handle, offset, size);
    }

    /**
     * Bind image texture.
     */
    public void bindImageTexture(int unit, long texture, int level, boolean layered, int layer, int access, int format) {
        glBindImageTexture(unit, (int) texture, level, layered, layer, access, format);
    }

    // ========================================================================
    // SYNCHRONIZATION
    // ========================================================================

    @Override
    public void memoryBarrier(int barrierBits) {
        int glBarriers = translateBarriers(barrierBits);
        glMemoryBarrier(glBarriers);
        memoryBarriers.increment();
    }

    private int translateBarriers(int bits) {
        int gl = 0;
        if ((bits & BarrierType.VERTEX_ATTRIB) != 0) gl |= GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT;
        if ((bits & BarrierType.INDEX_READ) != 0) gl |= GL_ELEMENT_ARRAY_BARRIER_BIT;
        if ((bits & BarrierType.UNIFORM_READ) != 0) gl |= GL_UNIFORM_BARRIER_BIT;
        if ((bits & BarrierType.TEXTURE_FETCH) != 0) gl |= GL_TEXTURE_FETCH_BARRIER_BIT;
        if ((bits & BarrierType.SHADER_IMAGE) != 0) gl |= GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
        if ((bits & BarrierType.INDIRECT_COMMAND) != 0) gl |= GL_COMMAND_BARRIER_BIT;
        if ((bits & BarrierType.BUFFER_UPDATE) != 0) gl |= GL_BUFFER_UPDATE_BARRIER_BIT;
        if ((bits & BarrierType.SHADER_STORAGE) != 0) gl |= GL_SHADER_STORAGE_BARRIER_BIT;
        if (bits == BarrierType.ALL) gl = GL_ALL_BARRIER_BITS;
        return gl;
    }

    /**
     * Texture barrier.
     */
    public void textureBarrier() {
        if (glVersion.atLeast(4, 5)) {
            glTextureBarrier();
        }
    }

    @Override
    public void finish() {
        glFinish();
    }

    /**
     * Flush commands without waiting.
     */
    public void flush() {
        glFlush();
    }

    /**
     * Create fence sync.
     */
    public long createFenceSync() {
        return glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
    }

    /**
     * Wait for fence sync.
     */
    public boolean waitSync(long sync, long timeoutNanos) {
        int result = glClientWaitSync(sync, GL_SYNC_FLUSH_COMMANDS_BIT, timeoutNanos);
        return result == GL_ALREADY_SIGNALED || result == GL_CONDITION_SATISFIED;
    }

    /**
     * Delete fence sync.
     */
    public void deleteSync(long sync) {
        glDeleteSync(sync);
    }

    /**
     * Wait for frame to complete.
     */
    public void waitForFrame(int frameIndex) {
        if (frameIndex < frameSyncObjects.size()) {
            SyncObject sync = frameSyncObjects.get(frameIndex);
            if (sync != null) {
                waitSync(sync.handle(), Long.MAX_VALUE);
                deleteSync(sync.handle());
            }
        }
    }

    // ========================================================================
    // RENDER PASS / FBO
    // ========================================================================

    @Override
    public void beginRenderPass(RenderPassInfo info) {
        if (info.colorAttachments != null && info.colorAttachments.length > 0) {
            // Would bind FBO here
        }

        glViewport(info.x, info.y, info.width, info.height);

        if (info.clearOnLoad) {
            int clearMask = 0;

            if (info.clearColor != null) {
                glClearColor(info.clearColor[0], info.clearColor[1], info.clearColor[2], info.clearColor[3]);
                clearMask |= GL_COLOR_BUFFER_BIT;
            }

            if (info.depthAttachment != 0 || info.clearDepth != 1.0f) {
                glClearDepth(info.clearDepth);
                clearMask |= GL_DEPTH_BUFFER_BIT;
            }

            if (info.clearStencil != 0) {
                glClearStencil(info.clearStencil);
                clearMask |= GL_STENCIL_BUFFER_BIT;
            }

            if (clearMask != 0) {
                glClear(clearMask);
            }
        }

        // Debug group
        if (capabilities.debugOutput() && info.name != null) {
            pushDebugGroup(info.name);
        }
    }

    @Override
    public void endRenderPass() {
        if (capabilities.debugOutput()) {
            popDebugGroup();
        }
    }

    /**
     * Bind FBO.
     */
    public void bindFramebuffer(int fbo) {
        if (currentFBO == fbo) return;
        currentFBO = fbo;
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        fboBinds.increment();
    }

    /**
     * Blit framebuffer.
     */
    public void blitFramebuffer(
            int srcX0, int srcY0, int srcX1, int srcY1,
            int dstX0, int dstY0, int dstX1, int dstY1,
            int mask, int filter) {
        glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    // ========================================================================
    // FRAME MANAGEMENT
    // ========================================================================

    @Override
    public void beginFrame() {
        int frame = currentFrame.get();

        // Wait for this frame's sync
        waitForFrame(frame);

        // Reset staging offset
        if (frame == 0) {
            stagingOffset.set(0);
        }

        frameCount.incrementAndGet();
    }

    @Override
    public void endFrame() {
        int frame = currentFrame.get();

        // Insert fence sync
        long sync = createFenceSync();
        while (frameSyncObjects.size() <= frame) {
            frameSyncObjects.add(null);
        }
        frameSyncObjects.set(frame, new SyncObject(sync, GL_SYNC_GPU_COMMANDS_COMPLETE, frameCount.get()));

        // Advance frame index
        int nextFrame = (frame + 1) % config.maxFramesInFlight();
        currentFrame.set(nextFrame);
    }

    @Override
    public int getCurrentFrameIndex() {
        return currentFrame.get();
    }

    /**
     * Get total frame count.
     */
    public long getFrameCount() {
        return frameCount.get();
    }

    // ========================================================================
    // PROFILING
    // ========================================================================

    /**
     * Begin timer query.
     */
    public void beginTimerQuery(String name) {
        if (!config.enableProfiling() || !capabilities.timerQuery()) return;

        Integer query = timerQueryPool.poll();
        if (query == null) return;

        glBeginQuery(GL_TIME_ELAPSED, query);
        activeTimerQueries.put(query, name);
    }

    /**
     * End timer query.
     */
    public void endTimerQuery() {
        if (!config.enableProfiling() || !capabilities.timerQuery()) return;
        glEndQuery(GL_TIME_ELAPSED);
    }

    /**
     * Collect timer query results.
     */
    public void collectTimerResults() {
        for (Map.Entry<Integer, String> entry : activeTimerQueries.entrySet()) {
            int query = entry.getKey();

            if (glGetQueryObjecti(query, GL_QUERY_RESULT_AVAILABLE) == GL_TRUE) {
                long elapsed = glGetQueryObjectui64(query, GL_QUERY_RESULT);
                double ms = elapsed / 1_000_000.0;

                timerResults.offer(new TimerQueryResult(entry.getValue(), 0, elapsed, ms));

                timerQueryPool.offer(query);
                activeTimerQueries.remove(query);
            }
        }

        // Limit result history
        while (timerResults.size() > 1000) {
            timerResults.poll();
        }
    }

    /**
     * Get timer results.
     */
    public List<TimerQueryResult> getTimerResults() {
        return new ArrayList<>(timerResults);
    }

    /**
     * Push debug group.
     */
    public void pushDebugGroup(String name) {
        if (capabilities.debugOutput()) {
            glPushDebugGroup(GL_DEBUG_SOURCE_APPLICATION, 0, name);
        }
    }

    /**
     * Pop debug group.
     */
    public void popDebugGroup() {
        if (capabilities.debugOutput()) {
            glPopDebugGroup();
        }
    }

    /**
     * Insert debug marker.
     */
    public void insertDebugMarker(String name) {
        if (capabilities.debugOutput()) {
            glDebugMessageInsert(GL_DEBUG_SOURCE_APPLICATION, GL_DEBUG_TYPE_MARKER,
                0, GL_DEBUG_SEVERITY_NOTIFICATION, name);
        }
    }

    /**
     * Set object label.
     */
    public void setObjectLabel(int type, int object, String label) {
        if (capabilities.debugOutput() && label != null) {
            glObjectLabel(type, object, label);
        }
    }

    /**
     * Get debug messages.
     */
    public List<DebugMessage> getDebugMessages() {
        return new ArrayList<>(debugMessages);
    }

    // ========================================================================
    // CAPABILITIES INTERFACE
    // ========================================================================

    @Override
    public boolean supportsMultiDrawIndirect() {
        return capabilities != null && capabilities.multiDrawIndirect();
    }

    @Override
    public boolean supportsIndirectCount() {
        return capabilities != null && capabilities.indirectCount();
    }

    @Override
    public boolean supportsComputeShaders() {
        return capabilities != null && capabilities.computeShaders();
    }

    @Override
    public boolean supportsMeshShaders() {
        return capabilities != null && capabilities.meshShaders();
    }

    @Override
    public boolean supportsBufferDeviceAddress() {
        return capabilities != null && capabilities.bufferDeviceAddress();
    }

    @Override
    public boolean supportsPersistentMapping() {
        return capabilities != null && capabilities.persistentMapping();
    }

    @Override
    public int getMaxComputeWorkGroupSize() {
        return capabilities != null ? capabilities.maxComputeWorkGroupInvocations() : 0;
    }

    @Override
    public int getMaxDrawIndirectCount() {
        return capabilities != null ? capabilities.maxDrawIndirectCount() : 0;
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Get backend statistics.
     */
    public Statistics getStatistics() {
        return new Statistics(
            drawCalls.sum(),
            dispatchCalls.sum(),
            bufferUploads.sum(),
            textureUploads.sum(),
            programBinds.sum(),
            textureBinds.sum(),
            vaoBinds.sum(),
            fboBinds.sum(),
            memoryBarriers.sum(),
            stateChanges.sum(),
            totalBufferMemory.get(),
            totalTextureMemory.get(),
            bufferResources.size(),
            textureResources.size(),
            shaderResources.size(),
            programResources.size(),
            vaoResources.size(),
            fboResources.size(),
            frameCount.get(),
            Duration.between(creationTime, Instant.now())
        );
    }

    public record Statistics(
        long drawCalls,
        long dispatchCalls,
        long bufferUploads,
        long textureUploads,
        long programBinds,
        long textureBinds,
        long vaoBinds,
        long fboBinds,
        long memoryBarriers,
        long stateChanges,
        long totalBufferMemory,
        long totalTextureMemory,
        int bufferCount,
        int textureCount,
        int shaderCount,
        int programCount,
        int vaoCount,
        int fboCount,
        long frameCount,
        Duration uptime
    ) {
        public String format() {
            return String.format("""
                OpenGLBackend Statistics:
                  Draw calls: %,d
                  Dispatch calls: %,d
                  Buffer uploads: %,d
                  Texture uploads: %,d
                  Program binds: %,d
                  Texture binds: %,d
                  VAO binds: %,d
                  FBO binds: %,d
                  Memory barriers: %,d
                  Buffer memory: %.2f MB
                  Texture memory: %.2f MB
                  Resources: %d buffers, %d textures, %d shaders, %d programs
                  Frames: %,d
                  Uptime: %s
                """,
                drawCalls, dispatchCalls, bufferUploads, textureUploads,
                programBinds, textureBinds, vaoBinds, fboBinds, memoryBarriers,
                totalBufferMemory / (1024.0 * 1024.0),
                totalTextureMemory / (1024.0 * 1024.0),
                bufferCount, textureCount, shaderCount, programCount,
                frameCount, uptime);
        }
    }

    /**
     * Reset statistics.
     */
    public void resetStatistics() {
        drawCalls.reset();
        dispatchCalls.reset();
        bufferUploads.reset();
        textureUploads.reset();
        programBinds.reset();
        textureBinds.reset();
        vaoBinds.reset();
        fboBinds.reset();
        memoryBarriers.reset();
        stateChanges.reset();
    }

    // ========================================================================
    // LIFECYCLE
    // ========================================================================

    @Override
    public void close() {
        if (closed) return;
        closed = true;

        Astralis.LOGGER.info("[OpenGL] Shutting down backend...");

        // Wait for GPU
        finish();

        // Destroy all resources
        for (int buffer : new ArrayList<>(bufferResources.keySet())) {
            destroyBuffer(buffer);
        }
        for (int texture : new ArrayList<>(textureResources.keySet())) {
            destroyTexture(texture);
        }
        for (int sampler : new ArrayList<>(samplerResources.keySet())) {
            glDeleteSamplers(sampler);
        }
        for (int shader : new ArrayList<>(shaderResources.keySet())) {
            destroyShader(shader);
        }
        for (int program : new ArrayList<>(programResources.keySet())) {
            destroyProgram(program);
        }

        // Destroy pooled resources
        for (Integer query : timerQueryPool) {
            glDeleteQueries(query);
        }
        for (Integer vao : vaoPool) {
            glDeleteVertexArrays(vao);
        }
        for (Integer fbo : fboPool) {
            glDeleteFramebuffers(fbo);
        }

        // Destroy sync objects
        for (SyncObject sync : frameSyncObjects) {
            if (sync != null) {
                deleteSync(sync.handle());
            }
        }

        // Destroy staging buffer
        if (stagingBuffer != 0) {
            glDeleteBuffers(stagingBuffer);
        }

        Astralis.LOGGER.info("[OpenGL] Backend shutdown complete. Stats:\n{}",
            getStatistics().format());
    }

    // ========================================================================
    // EXCEPTIONS
    // ========================================================================

    public static class ShaderCompilationException extends RuntimeException {
        public ShaderCompilationException(String message) {
            super(message);
        }
    }

    // ========================================================================
    // DEBUG
    // ========================================================================

    @Override
    public String toString() {
        return String.format("OpenGLBackend[%s, initialized=%s, resources=%d]",
            glVersion != null ? glVersion.versionString() : "N/A",
            initialized,
            bufferResources.size() + textureResources.size() + programResources.size());
    }
}
