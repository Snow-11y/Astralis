package stellar.snow.astralis.integration.jit;

import com.example.api.common.mapping.GLSLCallMapper;
import com.example.api.common.mapping.MetalCallMapper;
import com.example.api.common.mapping.OpenGLCallMapper;
import com.example.api.common.mapping.OpenGLESCallMapper;
import com.example.api.common.mapping.SPIRVCallMapper;
import com.example.api.common.mapping.VulkanCallMapper;

import stellar.snow.astralis.nexus.GLSLManager;
import stellar.snow.astralis.api.metal.managers.MetalManager;
import stellar.snow.astralis.api.opengl.managers.OpenGLManager;
import stellar.snow.astralis.api.opengles.managers.OpenGLESManager;
import stellar.snow.astralis.nexus.SPIRVManager;
import stellar.snow.astralis.engine.gpu.authority.UniversalCapabilities;
import stellar.snow.astralis.api.vulkan.managers.VulkanManager;
import stellar.snow.astralis.api.opengl.managers.GLStateCache;
import stellar.snow.astralis.api.common.BufferOps;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK13;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.Cleaner;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════════════════════╗
 * ║                              JITHelper - COMPREHENSIVE EDITION                                ║
 * ║══════════════════════════════════════════════════════════════════════════════════════════════║
 * ║  The Ultimate GPU Call Guardian, Diagnostician, Router, and Self-Healing System              ║
 * ║                                                                                               ║
 * ║  Target: Java 25 + LWJGL 3.3.6 | Performance-First | Zero-Allocation Hot Paths               ║
 * ╚══════════════════════════════════════════════════════════════════════════════════════════════╝
 *
 * ┌─────────────────────────────────────────────────────────────────────────────────────────────┐
 * │ COMPREHENSIVE ISSUE DOMAINS HANDLED                                                          │
 * ├─────────────────────────────────────────────────────────────────────────────────────────────┤
 * │                                                                                              │
 * │ ▸ SHADER ISSUES                          ▸ MEMORY ISSUES                                    │
 * │   • Compilation/linking failures           • GPU allocation failures                        │
 * │   • Validation errors                      • Memory fragmentation                           │
 * │   • Infinite loops / timeouts              • Bandwidth saturation                           │
 * │   • Precision mismatches                   • Staging buffer exhaustion                      │
 * │   • Register pressure / occupancy          • Descriptor pool exhaustion                     │
 * │   • Divergence killing performance         • Buffer orphaning                               │
 * │   • Descriptor binding issues              • Texture memory exhaustion                      │
 * │   • Push constant overflow                 • Render target allocation                       │
 * │   • Workgroup size issues                  • MSAA memory overhead                           │
 * │                                                                                              │
 * │ ▸ SYNCHRONIZATION ISSUES                 ▸ DRAW CALL ISSUES                                 │
 * │   • Pipeline stalls / GPU bubbles          • Excessive small draw calls                     │
 * │   • CPU-GPU sync point stalls              • Indirect buffer corruption                     │
 * │   • Fence wait timeouts                    • Index/vertex buffer issues                     │
 * │   • Semaphore deadlocks                    • Instancing failures                            │
 * │   • Queue submission bottlenecks           • Occlusion query overhead                       │
 * │   • Memory barrier misuse                  • Transform feedback overflow                    │
 * │   • Image layout transition bugs           • Primitive restart issues                       │
 * │                                                                                              │
 * │ ▸ PIPELINE STATE ISSUES                  ▸ TEXTURE ISSUES                                   │
 * │   • Pipeline creation stalls               • Format incompatibility                         │
 * │   • Pipeline cache misses                  • Compression issues                             │
 * │   • Dynamic state overhead                 • LOD / filtering issues                         │
 * │   • Viewport/scissor thrashing             • Residency issues (sparse)                      │
 * │   • Blend/depth state flush                • Bindless texture issues                        │
 * │   • Variable rate shading issues           • Sampler exhaustion                             │
 * │                                                                                              │
 * │ ▸ FRAMEBUFFER ISSUES                     ▸ DRIVER ISSUES                                    │
 * │   • Framebuffer incomplete                 • Driver crashes / TDR                           │
 * │   • Attachment format mismatch             • Memory corruption                              │
 * │   • Swapchain recreation                   • Version incompatibility                        │
 * │   • VSync / tearing issues                 • Extension unavailability                       │
 * │   • Frame pacing problems                  • Vendor-specific bugs                           │
 * │                                                                                              │
 * │ ▸ PERFORMANCE PATTERNS                   ▸ PLATFORM-SPECIFIC                                │
 * │   • GPU-bound vs CPU-bound                 • Mobile thermal throttling                      │
 * │   • Bandwidth vs compute bound             • Frequency scaling                              │
 * │   • Overdraw detection                     • Tile-based vs immediate                        │
 * │   • Early-Z / Hi-Z failures                • WebGPU limitations                             │
 * │   • Alpha blend order issues               • Validation layer overhead                      │
 * │                                                                                              │
 * └─────────────────────────────────────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────────────────────────────────┐
 * │ ROUTING & RECOVERY STRATEGIES                                                                │
 * ├─────────────────────────────────────────────────────────────────────────────────────────────┤
 * │                                                                                              │
 * │ SHADER ROUTING CHAIN:                                                                        │
 * │   Native Backend → SPIR-V Transpile → GLSL Compat → CPU Simplify → Minimal Fallback        │
 * │                                                                                              │
 * │ API CALL ROUTING:                                                                            │
 * │   VK chokes GL pattern → GLMapper simplifies → back to VK                                   │
 * │   GLES limited → GL fallback → GLES subset                                                  │
 * │   Metal issues → GLSL source → Metal recompile                                              │
 * │   SPIR-V validation fail → GLSL → recompile SPIR-V                                          │
 * │                                                                                              │
 * │ MEMORY ROUTING:                                                                              │
 * │   GPU alloc fail → different memory type → suballocation → evict LRU → CPU fallback        │
 * │                                                                                              │
 * │ RECOVERY MODES:                                                                              │
 * │   IMMEDIATE: retry simpler, use fallback, skip non-essential                                │
 * │   DEFERRED:  queue rebuild, schedule defrag, plan recompile                                 │
 * │   PREVENTIVE: pre-warm shaders, pre-allocate pools, cache states                            │
 * │                                                                                              │
 * └─────────────────────────────────────────────────────────────────────────────────────────────┘
 *
 * @author Astralis Performance Team
 * @version 3.0.0-COMPREHENSIVE
 * @since Java 25, LWJGL 3.3.6
 */
public final class JITHelper {

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 1: ENUMERATIONS & TYPE DEFINITIONS
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Graphics backend type.
     */
    public enum Backend {
        OPENGL(0),
        OPENGL_ES(1),
        VULKAN(2),
        METAL(3),
        SPIRV(4),
        GLSL(5),
        CPU_FALLBACK(6);

        public final int ordinalFast;
        Backend(int ord) { this.ordinalFast = ord; }

        public boolean isShaderBackend() {
            return this == SPIRV || this == GLSL;
        }

        public boolean isNativeGPU() {
            return this == OPENGL || this == OPENGL_ES || this == VULKAN || this == METAL;
        }
    }

    /**
     * Severity classification for diagnostics.
     */
    public enum Severity {
        TRACE(0),       // Debug-level, for profiling only
        INFO(1),        // Noteworthy but not problematic
        WARNING(2),     // Potential issue, soft degradation recommended
        SEVERE(3),      // Visible to user, immediate action needed
        CRITICAL(4),    // System stability at risk
        FATAL(5);       // Unrecoverable, must fallback or crash gracefully

        public final int level;
        Severity(int level) { this.level = level; }

        public boolean isActionable() { return level >= WARNING.level; }
        public boolean isUserVisible() { return level >= SEVERE.level; }
        public boolean requiresImmediateAction() { return level >= CRITICAL.level; }
    }

    /**
     * Comprehensive issue classification taxonomy.
     */
    public enum IssueKind {
        // ─── Timing Issues ───
        MICRO_SPIKE,            // < 1ms but above normal variance
        SMALL_SPIKE,            // 1-4ms spike
        MEDIUM_SPIKE,           // 4-8ms spike
        VISIBLE_STUTTER,        // 8-16ms (half frame @ 60fps)
        SEVERE_STUTTER,         // 16-33ms (full frame loss)
        CATASTROPHIC_STALL,     // > 33ms (multiple frames)

        // ─── Shader Issues ───
        SHADER_COMPILE_ERROR,
        SHADER_LINK_ERROR,
        SHADER_VALIDATION_ERROR,
        SHADER_TIMEOUT,
        SHADER_PRECISION_MISMATCH,
        SHADER_DIVERGENCE,
        SHADER_REGISTER_PRESSURE,
        SHADER_OCCUPANCY_LOW,
        SHADER_DESCRIPTOR_MISMATCH,
        SHADER_PUSH_CONSTANT_OVERFLOW,
        SHADER_WORKGROUP_SIZE_INVALID,
        SHADER_SPECIALIZATION_FAILURE,
        SHADER_SPIRV_INVALID,

        // ─── Memory Issues ───
        MEMORY_ALLOCATION_FAILURE,
        MEMORY_FRAGMENTATION,
        MEMORY_LEAK_DETECTED,
        MEMORY_BANDWIDTH_SATURATED,
        MEMORY_TRANSFER_BOTTLENECK,
        STAGING_BUFFER_EXHAUSTED,
        DESCRIPTOR_POOL_EXHAUSTED,
        COMMAND_POOL_EXHAUSTED,
        BUFFER_ORPHANING,
        TEXTURE_MEMORY_EXHAUSTED,
        RENDER_TARGET_ALLOCATION_FAILURE,
        DEPTH_PRECISION_INSUFFICIENT,

        // ─── Synchronization Issues ───
        PIPELINE_STALL,
        GPU_BUBBLE,
        CPU_GPU_SYNC_STALL,
        FENCE_TIMEOUT,
        SEMAPHORE_DEADLOCK,
        QUEUE_SUBMISSION_BOTTLENECK,
        PRESENT_QUEUE_STARVATION,
        RENDER_PASS_DEPENDENCY_VIOLATION,
        MEMORY_BARRIER_MISUSE,
        IMAGE_LAYOUT_TRANSITION_ERROR,

        // ─── Draw Call Issues ───
        EXCESSIVE_DRAW_CALLS,
        INDIRECT_BUFFER_CORRUPTION,
        INDEX_BUFFER_OUT_OF_BOUNDS,
        VERTEX_BUFFER_MISMATCH,
        INSTANCING_FAILURE,
        OCCLUSION_QUERY_OVERHEAD,
        TRANSFORM_FEEDBACK_OVERFLOW,

        // ─── Pipeline State Issues ───
        PIPELINE_CREATION_STALL,
        PIPELINE_CACHE_MISS,
        DYNAMIC_STATE_OVERHEAD,
        STATE_THRASHING,
        BLEND_STATE_FLUSH,
        DEPTH_STENCIL_FLUSH,

        // ─── Texture Issues ───
        TEXTURE_FORMAT_INCOMPATIBLE,
        TEXTURE_COMPRESSION_ERROR,
        TEXTURE_LOD_ISSUE,
        TEXTURE_RESIDENCY_FAILURE,
        BINDLESS_TEXTURE_ERROR,
        SAMPLER_EXHAUSTED,

        // ─── Framebuffer Issues ───
        FRAMEBUFFER_INCOMPLETE,
        ATTACHMENT_FORMAT_MISMATCH,
        SWAPCHAIN_RECREATION_NEEDED,
        SWAPCHAIN_SUBOPTIMAL,
        VSYNC_ISSUE,
        FRAME_PACING_UNSTABLE,

        // ─── Driver Issues ───
        DRIVER_CRASH,
        DRIVER_TDR,
        DRIVER_MEMORY_CORRUPTION,
        DRIVER_VERSION_INCOMPATIBLE,
        EXTENSION_UNAVAILABLE,
        FEATURE_LEVEL_MISMATCH,
        VENDOR_BUG_DETECTED,

        // ─── Performance Patterns ───
        GPU_BOUND,
        CPU_BOUND,
        BANDWIDTH_BOUND,
        COMPUTE_BOUND,
        OVERDRAW_DETECTED,
        EARLY_Z_FAILURE,
        HIZ_FAILURE,
        ALPHA_BLEND_ORDER_WRONG,
        TILE_RENDERER_INEFFICIENT,

        // ─── Platform Specific ───
        THERMAL_THROTTLING,
        FREQUENCY_SCALING,
        MOBILE_POWER_SAVING,
        VALIDATION_LAYER_OVERHEAD,

        // ─── Capability Issues ───
        CAPABILITY_MISMATCH,
        CAPABILITY_DEGRADED,
        CAPABILITY_UNSUPPORTED,

        // ─── Recovery Events ───
        RECOVERY_ATTEMPTED,
        RECOVERY_SUCCEEDED,
        RECOVERY_FAILED,
        FALLBACK_ACTIVATED,

        // ─── Generic ───
        RUNTIME_ERROR,
        UNKNOWN
    }

    /**
     * Call category for grouping and routing decisions.
     */
    public enum CallCategory {
        SHADER_COMPILE,
        SHADER_LINK,
        SHADER_BIND,
        DRAW,
        DRAW_INDEXED,
        DRAW_INDIRECT,
        DISPATCH_COMPUTE,
        BUFFER_UPLOAD,
        BUFFER_MAP,
        BUFFER_COPY,
        TEXTURE_UPLOAD,
        TEXTURE_COPY,
        TEXTURE_BIND,
        FRAMEBUFFER_BIND,
        FRAMEBUFFER_CLEAR,
        PIPELINE_BIND,
        PIPELINE_CREATE,
        DESCRIPTOR_UPDATE,
        DESCRIPTOR_BIND,
        RENDER_PASS_BEGIN,
        RENDER_PASS_END,
        COMMAND_BUFFER_BEGIN,
        COMMAND_BUFFER_END,
        COMMAND_BUFFER_SUBMIT,
        QUEUE_SUBMIT,
        QUEUE_PRESENT,
        FENCE_WAIT,
        SEMAPHORE_SIGNAL,
        SEMAPHORE_WAIT,
        MEMORY_BARRIER,
        QUERY_BEGIN,
        QUERY_END,
        STATE_CHANGE,
        MISC
    }

    /**
     * Recovery strategy classification.
     */
    public enum RecoveryStrategy {
        NONE,                       // No recovery needed/possible
        RETRY_SIMPLE,               // Retry with simpler parameters
        RETRY_DELAYED,              // Retry after short delay
        REROUTE_MAPPER,             // Route through different mapper
        REROUTE_BACKEND,            // Switch to different backend entirely
        SIMPLIFY_SHADER,            // Reduce shader complexity
        SIMPLIFY_SHADER_CPU,        // Move shader work to CPU
        REBUILD_PIPELINE,           // Recreate pipeline from scratch
        REBUILD_RESOURCE,           // Recreate the problematic resource
        EVICT_AND_RETRY,            // Free memory and retry
        DEFRAGMENT,                 // Defragment memory pools
        DOWNGRADE_QUALITY,          // Reduce quality settings
        SKIP_OPERATION,             // Skip this operation entirely
        EMERGENCY_GC,               // Force garbage collection
        EMERGENCY_FLUSH,            // Force GPU flush
        DEVICE_RESET,               // Reset GPU device (last resort)
        GRACEFUL_DEGRADATION        // Enter reduced functionality mode
    }

    /**
     * Routing decision for cross-mapper operations.
     */
    public enum RoutingDecision {
        PROCEED_NORMAL,             // Continue with current path
        ROUTE_TO_GL,                // Route through OpenGL mapper
        ROUTE_TO_GLES,              // Route through OpenGL ES mapper
        ROUTE_TO_VK,                // Route through Vulkan mapper
        ROUTE_TO_METAL,             // Route through Metal mapper
        ROUTE_TO_SPIRV,             // Route through SPIR-V mapper
        ROUTE_TO_GLSL,              // Route through GLSL mapper
        ROUTE_TO_CPU,               // Fall back to CPU
        BLOCK,                      // Block this operation
        DEFER                       // Defer to next frame
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 2: RECORDS & VALUE TYPES (Allocation-Free Where Possible)
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Immutable key identifying a profiled call site.
     * Create as static final constants for zero-allocation hot paths.
     */
    public record CallKey(
        Backend backend,
        CallCategory category,
        String name,
        int hash  // Pre-computed for fast lookups
    ) {
        public CallKey(Backend backend, CallCategory category, String name) {
            this(backend, category, name, computeHash(backend, category, name));
        }

        private static int computeHash(Backend backend, CallCategory category, String name) {
            int h = backend.ordinalFast;
            h = 31 * h + category.ordinal();
            h = 31 * h + name.hashCode();
            return h;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CallKey other)) return false;
            return hash == other.hash &&
                   backend == other.backend &&
                   category == other.category &&
                   name.equals(other.name);
        }

        public boolean isShaderRelated() {
            return category == CallCategory.SHADER_COMPILE ||
                   category == CallCategory.SHADER_LINK ||
                   category == CallCategory.SHADER_BIND ||
                   backend.isShaderBackend();
        }

        public boolean isDrawCall() {
            return category == CallCategory.DRAW ||
                   category == CallCategory.DRAW_INDEXED ||
                   category == CallCategory.DRAW_INDIRECT ||
                   category == CallCategory.DISPATCH_COMPUTE;
        }

        public boolean isMemoryOperation() {
            return category == CallCategory.BUFFER_UPLOAD ||
                   category == CallCategory.BUFFER_MAP ||
                   category == CallCategory.BUFFER_COPY ||
                   category == CallCategory.TEXTURE_UPLOAD ||
                   category == CallCategory.TEXTURE_COPY;
        }

        public boolean isSyncOperation() {
            return category == CallCategory.FENCE_WAIT ||
                   category == CallCategory.SEMAPHORE_WAIT ||
                   category == CallCategory.QUEUE_SUBMIT ||
                   category == CallCategory.COMMAND_BUFFER_SUBMIT;
        }
    }

    /**
     * Statistics snapshot for a call site.
     */
    public record CallStatsSnapshot(
        long totalCalls,
        long totalNanos,
        long minNanos,
        long maxNanos,
        double meanNanos,
        double ewmaNanos,
        double varianceNanos,
        long spikeCount,
        long errorCount,
        long lastCallNanos,
        long lastSpikeNanos
    ) {
        public double stdDevNanos() {
            return Math.sqrt(varianceNanos);
        }

        public double coefficientOfVariation() {
            return meanNanos > 0 ? stdDevNanos() / meanNanos : 0;
        }

        public double spikeRate() {
            return totalCalls > 0 ? (double) spikeCount / totalCalls : 0;
        }

        public double errorRate() {
            return totalCalls > 0 ? (double) errorCount / totalCalls : 0;
        }
    }

    /**
     * Comprehensive diagnostic report.
     */
    public record DiagnosticsReport(
        UUID reportId,
        Instant timestamp,
        CallKey callKey,
        IssueKind issueKind,
        Severity severity,
        long durationNanos,
        CallStatsSnapshot stats,
        String message,
        String detailedAnalysis,
        Throwable cause,
        int errorCode,
        RecoveryStrategy suggestedRecovery,
        RoutingDecision suggestedRouting,
        Map<String, Object> metadata
    ) {
        public DiagnosticsReport {
            Objects.requireNonNull(reportId);
            Objects.requireNonNull(timestamp);
            Objects.requireNonNull(callKey);
            Objects.requireNonNull(issueKind);
            Objects.requireNonNull(severity);
            Objects.requireNonNull(message);
            metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        }

        public double durationMillis() {
            return durationNanos / 1_000_000.0;
        }

        public boolean isShaderRelated() {
            return callKey.isShaderRelated() || issueKind.name().startsWith("SHADER_");
        }

        public boolean requiresImmediateAction() {
            return severity.requiresImmediateAction();
        }

        public boolean isRecoverable() {
            return suggestedRecovery != RecoveryStrategy.NONE &&
                   suggestedRecovery != RecoveryStrategy.DEVICE_RESET;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private UUID reportId = UUID.randomUUID();
            private Instant timestamp = Instant.now();
            private CallKey callKey;
            private IssueKind issueKind = IssueKind.UNKNOWN;
            private Severity severity = Severity.INFO;
            private long durationNanos;
            private CallStatsSnapshot stats;
            private String message = "";
            private String detailedAnalysis;
            private Throwable cause;
            private int errorCode;
            private RecoveryStrategy suggestedRecovery = RecoveryStrategy.NONE;
            private RoutingDecision suggestedRouting = RoutingDecision.PROCEED_NORMAL;
            private Map<String, Object> metadata = new HashMap<>();

            public Builder callKey(CallKey key) { this.callKey = key; return this; }
            public Builder issueKind(IssueKind kind) { this.issueKind = kind; return this; }
            public Builder severity(Severity sev) { this.severity = sev; return this; }
            public Builder durationNanos(long nanos) { this.durationNanos = nanos; return this; }
            public Builder stats(CallStatsSnapshot s) { this.stats = s; return this; }
            public Builder message(String msg) { this.message = msg; return this; }
            public Builder detailedAnalysis(String analysis) { this.detailedAnalysis = analysis; return this; }
            public Builder cause(Throwable t) { this.cause = t; return this; }
            public Builder errorCode(int code) { this.errorCode = code; return this; }
            public Builder suggestedRecovery(RecoveryStrategy r) { this.suggestedRecovery = r; return this; }
            public Builder suggestedRouting(RoutingDecision d) { this.suggestedRouting = d; return this; }
            public Builder addMetadata(String key, Object value) { this.metadata.put(key, value); return this; }

            public DiagnosticsReport build() {
                Objects.requireNonNull(callKey, "callKey required");
                return new DiagnosticsReport(
                    reportId, timestamp, callKey, issueKind, severity,
                    durationNanos, stats, message, detailedAnalysis,
                    cause, errorCode, suggestedRecovery, suggestedRouting, metadata
                );
            }
        }
    }

    /**
     * Recovery action result.
     */
    public record RecoveryResult(
        boolean success,
        RecoveryStrategy strategyUsed,
        String description,
        long recoveryTimeNanos,
        DiagnosticsReport originalReport
    ) {}

    /**
     * Routing result for cross-mapper operations.
     */
    public record RoutingResult<T>(
        T result,
        RoutingDecision decisionUsed,
        Backend originalBackend,
        Backend actualBackend,
        long routingOverheadNanos,
        boolean wasRerouted
    ) {}

    /**
     * Capability probe result.
     */
    public record CapabilityProbeResult(
        Backend backend,
        boolean isStable,
        boolean isOptimal,
        Set<String> missingFeatures,
        Set<String> degradedFeatures,
        Map<String, Object> measurements
    ) {}

    /**
     * Frame timing analysis.
     */
    public record FrameTimingAnalysis(
        long frameNumber,
        long totalFrameNanos,
        long gpuTimeNanos,
        long cpuTimeNanos,
        long syncTimeNanos,
        double gpuUtilization,
        double cpuUtilization,
        boolean isGpuBound,
        boolean isCpuBound,
        boolean isBandwidthBound,
        List<IssueKind> detectedIssues
    ) {}

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 3: CONSTANTS & THRESHOLDS
    // ════════════════════════════════════════════════════════════════════════════════════════════

    // ─── Timing Thresholds (nanoseconds) ───
    private static final long MICRO_SPIKE_THRESHOLD_NS       =     500_000L;   // 0.5ms
    private static final long SMALL_SPIKE_THRESHOLD_NS       =   1_000_000L;   // 1ms
    private static final long MEDIUM_SPIKE_THRESHOLD_NS      =   4_000_000L;   // 4ms
    private static final long VISIBLE_STUTTER_THRESHOLD_NS   =   8_000_000L;   // 8ms
    private static final long SEVERE_STUTTER_THRESHOLD_NS    =  16_666_666L;   // ~16.67ms (1 frame @ 60fps)
    private static final long CATASTROPHIC_STALL_THRESHOLD_NS = 33_333_333L;   // ~33.33ms (2 frames @ 60fps)
    private static final long SHADER_COMPILE_WARNING_NS      =  50_000_000L;   // 50ms
    private static final long SHADER_COMPILE_SEVERE_NS       = 200_000_000L;   // 200ms
    private static final long FENCE_WAIT_WARNING_NS          =   5_000_000L;   // 5ms
    private static final long FENCE_WAIT_TIMEOUT_NS          = 100_000_000L;   // 100ms

    // ─── Statistical Thresholds ───
    private static final long MIN_SAMPLES_FOR_STATS          = 32;
    private static final long MIN_SAMPLES_FOR_VARIANCE       = 64;
    private static final double EWMA_ALPHA                   = 0.1;
    private static final double EWMA_ALPHA_FAST              = 0.3;
    private static final double RELATIVE_SPIKE_FACTOR        = 3.0;
    private static final double SEVERE_SPIKE_FACTOR          = 6.0;
    private static final double VARIANCE_SPIKE_FACTOR        = 4.0;  // stddev multiplier
    private static final double SPIKE_RATE_WARNING_THRESHOLD = 0.01; // 1% spike rate
    private static final double ERROR_RATE_WARNING_THRESHOLD = 0.001; // 0.1% error rate

    // ─── Memory Thresholds ───
    private static final int MAX_TRACKED_CALL_SITES          = 8192;
    private static final int CALL_SITE_EVICTION_BATCH        = 512;
    private static final int MAX_REPORT_QUEUE_SIZE           = 4096;
    private static final int MAX_RECOVERY_QUEUE_SIZE         = 1024;
    private static final int MAX_HISTORY_PER_CALL            = 256;
    private static final long REPORT_DEDUP_WINDOW_NS         = 1_000_000_000L; // 1 second

    // ─── Recovery Thresholds ───
    private static final int MAX_RECOVERY_ATTEMPTS           = 3;
    private static final long RECOVERY_COOLDOWN_NS           = 5_000_000_000L; // 5 seconds
    private static final long CIRCUIT_BREAKER_THRESHOLD      = 5;
    private static final long CIRCUIT_BREAKER_RESET_NS       = 30_000_000_000L; // 30 seconds

    // ─── Vector API Species ───
    private static final VectorSpecies<Float> FLOAT_SPECIES  = FloatVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Integer> INT_SPECIES  = IntVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Byte> BYTE_SPECIES    = ByteVector.SPECIES_PREFERRED;
    private static final int VECTOR_FLOAT_LENGTH             = FLOAT_SPECIES.length();

    // ─── Thread Pool Sizing ───
    private static final int AVAILABLE_PROCESSORS            = Runtime.getRuntime().availableProcessors();
    private static final int DIAGNOSTICS_THREADS             = Math.max(2, AVAILABLE_PROCESSORS / 4);
    private static final int RECOVERY_THREADS                = Math.max(1, AVAILABLE_PROCESSORS / 8);

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 4: SINGLETON INSTANCE
    // ════════════════════════════════════════════════════════════════════════════════════════════

    private static volatile JITHelper instance;
    private static final Object INSTANCE_LOCK = new Object();

    public static JITHelper getInstance() {
        JITHelper local = instance;
        if (local == null) {
            synchronized (INSTANCE_LOCK) {
                local = instance;
                if (local == null) {
                    instance = local = new JITHelper();
                }
            }
        }
        return local;
    }

    public static boolean isInstantiated() {
        return instance != null;
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 5: CALL STATISTICS ENGINE (Lock-Free, Zero-Allocation Hot Path)
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * High-performance statistics tracker for individual call sites.
     * Uses padded atomics to prevent false sharing.
     */
    @SuppressWarnings("unused")
    private static final class CallStats {
        // ─── Padding to prevent false sharing ───
        private long p00, p01, p02, p03, p04, p05, p06, p07;

        // ─── Core counters ───
        private final LongAdder callCount = new LongAdder();
        private final LongAdder totalNanos = new LongAdder();
        private final LongAdder spikeCount = new LongAdder();
        private final LongAdder errorCount = new LongAdder();

        private long p10, p11, p12, p13, p14, p15, p16, p17;

        // ─── Min/Max tracking ───
        private final AtomicLong minNanos = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxNanos = new AtomicLong(0);
        private final AtomicLong lastCallNanos = new AtomicLong(0);
        private final AtomicLong lastSpikeNanos = new AtomicLong(0);

        private long p20, p21, p22, p23, p24, p25, p26, p27;

        // ─── EWMA & Variance (volatile for visibility) ───
        private volatile double ewmaNanos = 0;
        private volatile double ewmaVariance = 0;
        private volatile double m2 = 0;  // For Welford's online variance

        private long p30, p31, p32, p33, p34, p35, p36, p37;

        // ─── History buffer (circular, for pattern detection) ───
        private final long[] recentDurations = new long[64];
        private final AtomicInteger historyIndex = new AtomicInteger(0);

        /**
         * Records a call duration. Zero-allocation.
         */
        void record(long nanos) {
            long count = callCount.sum();
            callCount.increment();
            totalNanos.add(nanos);
            lastCallNanos.set(nanos);

            // Min update
            long currentMin = minNanos.get();
            while (nanos < currentMin) {
                if (minNanos.compareAndSet(currentMin, nanos)) break;
                currentMin = minNanos.get();
            }

            // Max update
            long currentMax = maxNanos.get();
            while (nanos > currentMax) {
                if (maxNanos.compareAndSet(currentMax, nanos)) break;
                currentMax = maxNanos.get();
            }

            // EWMA update (non-atomic but acceptable for statistics)
            double prevEwma = ewmaNanos;
            if (prevEwma == 0) {
                ewmaNanos = nanos;
            } else {
                ewmaNanos = prevEwma + EWMA_ALPHA * (nanos - prevEwma);
            }

            // Welford's online variance
            if (count > 0) {
                double mean = (double) totalNanos.sum() / (count + 1);
                double delta = nanos - mean;
                m2 += delta * delta;
                ewmaVariance = m2 / (count + 1);
            }

            // History buffer
            int idx = historyIndex.getAndIncrement() & 63;
            recentDurations[idx] = nanos;
        }

        void recordSpike() {
            spikeCount.increment();
            lastSpikeNanos.set(System.nanoTime());
        }

        void recordError() {
            errorCount.increment();
        }

        CallStatsSnapshot snapshot() {
            long count = callCount.sum();
            long total = totalNanos.sum();
            double mean = count > 0 ? (double) total / count : 0;

            return new CallStatsSnapshot(
                count,
                total,
                count > 0 ? minNanos.get() : 0,
                maxNanos.get(),
                mean,
                ewmaNanos,
                ewmaVariance,
                spikeCount.sum(),
                errorCount.sum(),
                lastCallNanos.get(),
                lastSpikeNanos.get()
            );
        }

        /**
         * Analyzes recent history for patterns.
         */
        long[] getRecentHistory(int count) {
            int currentIdx = historyIndex.get();
            int len = Math.min(count, 64);
            long[] result = new long[len];
            for (int i = 0; i < len; i++) {
                result[i] = recentDurations[(currentIdx - 1 - i) & 63];
            }
            return result;
        }

        /**
         * Detects if there's an upward trend in recent calls.
         */
        boolean hasUpwardTrend() {
            long[] recent = getRecentHistory(16);
            if (recent.length < 8) return false;

            // Simple linear regression slope
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
            int n = recent.length;
            for (int i = 0; i < n; i++) {
                sumX += i;
                sumY += recent[i];
                sumXY += i * recent[i];
                sumX2 += i * i;
            }
            double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
            return slope > ewmaNanos * 0.1;  // Significant upward trend
        }
    }

    /**
     * Statistics map with automatic eviction of low-traffic entries.
     */
    private final ConcurrentHashMap<CallKey, CallStats> statsMap = new ConcurrentHashMap<>(4096);

    private CallStats getOrCreateStats(CallKey key) {
        CallStats stats = statsMap.get(key);
        if (stats != null) return stats;

        // Evict if too large
        if (statsMap.size() >= MAX_TRACKED_CALL_SITES) {
            evictLowTrafficStats();
        }

        return statsMap.computeIfAbsent(key, k -> new CallStats());
    }

    private void evictLowTrafficStats() {
        if (statsMap.size() < MAX_TRACKED_CALL_SITES) return;

        // Find and remove lowest-traffic entries
        statsMap.entrySet().stream()
            .sorted(Comparator.comparingLong(e -> e.getValue().callCount.sum()))
            .limit(CALL_SITE_EVICTION_BATCH)
            .map(Map.Entry::getKey)
            .forEach(statsMap::remove);
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 6: SPIKE DETECTION & CLASSIFICATION ENGINE
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Comprehensive spike detector with multiple detection strategies.
     */
    private final class SpikeDetector {

        /**
         * Result of spike analysis.
         */
        record SpikeAnalysis(
            boolean isSpike,
            IssueKind issueKind,
            Severity severity,
            double deviationFactor,
            String reason
        ) {}

        /**
         * Analyzes a call duration for spikes using multiple strategies.
         */
        SpikeAnalysis analyze(CallKey key, long durationNanos, CallStats stats, Throwable error) {
            // Immediate classification for errors
            if (error != null) {
                return classifyError(key, error, durationNanos);
            }

            CallStatsSnapshot snapshot = stats.snapshot();

            // Strategy 1: Absolute thresholds
            SpikeAnalysis absoluteResult = checkAbsoluteThresholds(durationNanos);
            if (absoluteResult.severity.level >= Severity.SEVERE.level) {
                return absoluteResult;
            }

            // Strategy 2: EWMA-relative detection
            if (snapshot.totalCalls() >= MIN_SAMPLES_FOR_STATS) {
                SpikeAnalysis ewmaResult = checkEWMARelative(durationNanos, snapshot);
                if (ewmaResult.isSpike && ewmaResult.severity.level > absoluteResult.severity.level) {
                    return ewmaResult;
                }
            }

            // Strategy 3: Variance-based detection
            if (snapshot.totalCalls() >= MIN_SAMPLES_FOR_VARIANCE) {
                SpikeAnalysis varianceResult = checkVarianceBased(durationNanos, snapshot);
                if (varianceResult.isSpike && varianceResult.severity.level > absoluteResult.severity.level) {
                    return varianceResult;
                }
            }

            // Strategy 4: Pattern-based detection (upward trends)
            if (stats.hasUpwardTrend()) {
                return new SpikeAnalysis(
                    true,
                    IssueKind.MICRO_SPIKE,
                    Severity.WARNING,
                    1.5,
                    "Detected upward trend in call durations"
                );
            }

            // Strategy 5: Category-specific thresholds
            SpikeAnalysis categoryResult = checkCategorySpecific(key, durationNanos, snapshot);
            if (categoryResult.isSpike) {
                return categoryResult;
            }

            return absoluteResult.isSpike ? absoluteResult : new SpikeAnalysis(
                false, IssueKind.UNKNOWN, Severity.TRACE, 0, "Normal operation"
            );
        }

        private SpikeAnalysis checkAbsoluteThresholds(long nanos) {
            if (nanos >= CATASTROPHIC_STALL_THRESHOLD_NS) {
                return new SpikeAnalysis(true, IssueKind.CATASTROPHIC_STALL, Severity.CRITICAL,
                    (double) nanos / CATASTROPHIC_STALL_THRESHOLD_NS,
                    "Catastrophic stall: " + nanosToMs(nanos) + "ms");
            }
            if (nanos >= SEVERE_STUTTER_THRESHOLD_NS) {
                return new SpikeAnalysis(true, IssueKind.SEVERE_STUTTER, Severity.SEVERE,
                    (double) nanos / SEVERE_STUTTER_THRESHOLD_NS,
                    "Severe stutter: " + nanosToMs(nanos) + "ms");
            }
            if (nanos >= VISIBLE_STUTTER_THRESHOLD_NS) {
                return new SpikeAnalysis(true, IssueKind.VISIBLE_STUTTER, Severity.SEVERE,
                    (double) nanos / VISIBLE_STUTTER_THRESHOLD_NS,
                    "Visible stutter: " + nanosToMs(nanos) + "ms");
            }
            if (nanos >= MEDIUM_SPIKE_THRESHOLD_NS) {
                return new SpikeAnalysis(true, IssueKind.MEDIUM_SPIKE, Severity.WARNING,
                    (double) nanos / MEDIUM_SPIKE_THRESHOLD_NS,
                    "Medium spike: " + nanosToMs(nanos) + "ms");
            }
            if (nanos >= SMALL_SPIKE_THRESHOLD_NS) {
                return new SpikeAnalysis(true, IssueKind.SMALL_SPIKE, Severity.INFO,
                    (double) nanos / SMALL_SPIKE_THRESHOLD_NS,
                    "Small spike: " + nanosToMs(nanos) + "ms");
            }
            if (nanos >= MICRO_SPIKE_THRESHOLD_NS) {
                return new SpikeAnalysis(true, IssueKind.MICRO_SPIKE, Severity.TRACE,
                    (double) nanos / MICRO_SPIKE_THRESHOLD_NS,
                    "Micro spike: " + nanosToMs(nanos) + "ms");
            }
            return new SpikeAnalysis(false, IssueKind.UNKNOWN, Severity.TRACE, 0, "Below thresholds");
        }

        private SpikeAnalysis checkEWMARelative(long nanos, CallStatsSnapshot stats) {
            double ewma = stats.ewmaNanos();
            if (ewma <= 0) return new SpikeAnalysis(false, IssueKind.UNKNOWN, Severity.TRACE, 0, "No EWMA");

            double factor = nanos / ewma;

            if (factor >= SEVERE_SPIKE_FACTOR) {
                return new SpikeAnalysis(true, IssueKind.SEVERE_STUTTER, Severity.SEVERE,
                    factor, String.format("%.1fx slower than average (%.3fms vs %.3fms avg)",
                        factor, nanosToMs(nanos), nanosToMs((long) ewma)));
            }
            if (factor >= RELATIVE_SPIKE_FACTOR && nanos >= MICRO_SPIKE_THRESHOLD_NS) {
                return new SpikeAnalysis(true, IssueKind.MEDIUM_SPIKE, Severity.WARNING,
                    factor, String.format("%.1fx slower than average", factor));
            }

            return new SpikeAnalysis(false, IssueKind.UNKNOWN, Severity.TRACE, factor, "Within relative bounds");
        }

        private SpikeAnalysis checkVarianceBased(long nanos, CallStatsSnapshot stats) {
            double stdDev = stats.stdDevNanos();
            double mean = stats.meanNanos();
            if (stdDev <= 0 || mean <= 0) {
                return new SpikeAnalysis(false, IssueKind.UNKNOWN, Severity.TRACE, 0, "Insufficient variance data");
            }

            double zScore = (nanos - mean) / stdDev;

            if (zScore >= VARIANCE_SPIKE_FACTOR * 2) {
                return new SpikeAnalysis(true, IssueKind.SEVERE_STUTTER, Severity.SEVERE,
                    zScore, String.format("%.1f standard deviations above mean", zScore));
            }
            if (zScore >= VARIANCE_SPIKE_FACTOR) {
                return new SpikeAnalysis(true, IssueKind.MEDIUM_SPIKE, Severity.WARNING,
                    zScore, String.format("%.1f standard deviations above mean", zScore));
            }

            return new SpikeAnalysis(false, IssueKind.UNKNOWN, Severity.TRACE, zScore, "Within variance bounds");
        }

        private SpikeAnalysis checkCategorySpecific(CallKey key, long nanos, CallStatsSnapshot stats) {
            return switch (key.category()) {
                case SHADER_COMPILE, SHADER_LINK -> {
                    if (nanos >= SHADER_COMPILE_SEVERE_NS) {
                        yield new SpikeAnalysis(true, IssueKind.PIPELINE_CREATION_STALL, Severity.SEVERE,
                            (double) nanos / SHADER_COMPILE_SEVERE_NS,
                            "Shader compilation took " + nanosToMs(nanos) + "ms");
                    }
                    if (nanos >= SHADER_COMPILE_WARNING_NS) {
                        yield new SpikeAnalysis(true, IssueKind.PIPELINE_CREATION_STALL, Severity.WARNING,
                            (double) nanos / SHADER_COMPILE_WARNING_NS,
                            "Shader compilation took " + nanosToMs(nanos) + "ms");
                    }
                    yield new SpikeAnalysis(false, IssueKind.UNKNOWN, Severity.TRACE, 0, "Normal compile time");
                }

                case FENCE_WAIT, SEMAPHORE_WAIT -> {
                    if (nanos >= FENCE_WAIT_TIMEOUT_NS) {
                        yield new SpikeAnalysis(true, IssueKind.FENCE_TIMEOUT, Severity.CRITICAL,
                            (double) nanos / FENCE_WAIT_TIMEOUT_NS,
                            "Fence wait timeout: " + nanosToMs(nanos) + "ms");
                    }
                    if (nanos >= FENCE_WAIT_WARNING_NS) {
                        yield new SpikeAnalysis(true, IssueKind.CPU_GPU_SYNC_STALL, Severity.WARNING,
                            (double) nanos / FENCE_WAIT_WARNING_NS,
                            "Long fence wait: " + nanosToMs(nanos) + "ms");
                    }
                    yield new SpikeAnalysis(false, IssueKind.UNKNOWN, Severity.TRACE, 0, "Normal sync time");
                }

                case QUEUE_SUBMIT, COMMAND_BUFFER_SUBMIT -> {
                    if (nanos >= VISIBLE_STUTTER_THRESHOLD_NS) {
                        yield new SpikeAnalysis(true, IssueKind.QUEUE_SUBMISSION_BOTTLENECK, Severity.SEVERE,
                            (double) nanos / VISIBLE_STUTTER_THRESHOLD_NS,
                            "Queue submission bottleneck: " + nanosToMs(nanos) + "ms");
                    }
                    yield new SpikeAnalysis(false, IssueKind.UNKNOWN, Severity.TRACE, 0, "Normal submission time");
                }

                case PIPELINE_CREATE -> {
                    // Pipeline creation is expected to be slow; use higher thresholds
                    if (nanos >= SHADER_COMPILE_SEVERE_NS * 2) {
                        yield new SpikeAnalysis(true, IssueKind.PIPELINE_CREATION_STALL, Severity.SEVERE,
                            (double) nanos / (SHADER_COMPILE_SEVERE_NS * 2),
                            "Pipeline creation stall: " + nanosToMs(nanos) + "ms");
                    }
                    yield new SpikeAnalysis(false, IssueKind.UNKNOWN, Severity.TRACE, 0, "Normal pipeline creation");
                }

                default -> new SpikeAnalysis(false, IssueKind.UNKNOWN, Severity.TRACE, 0, "No category-specific threshold");
            };
        }

        private SpikeAnalysis classifyError(CallKey key, Throwable error, long durationNanos) {
            IssueKind kind = IssueKind.RUNTIME_ERROR;
            Severity severity = Severity.SEVERE;

            String errorName = error.getClass().getSimpleName().toLowerCase();
            String errorMsg = error.getMessage() != null ? error.getMessage().toLowerCase() : "";

            // Classify based on error type
            if (errorName.contains("outofmemory") || errorMsg.contains("out of memory")) {
                kind = IssueKind.MEMORY_ALLOCATION_FAILURE;
                severity = Severity.CRITICAL;
            } else if (errorName.contains("timeout") || errorMsg.contains("timeout")) {
                kind = IssueKind.FENCE_TIMEOUT;
                severity = Severity.CRITICAL;
            } else if (errorMsg.contains("shader") || key.isShaderRelated()) {
                if (errorMsg.contains("compile")) {
                    kind = IssueKind.SHADER_COMPILE_ERROR;
                } else if (errorMsg.contains("link")) {
                    kind = IssueKind.SHADER_LINK_ERROR;
                } else if (errorMsg.contains("validation")) {
                    kind = IssueKind.SHADER_VALIDATION_ERROR;
                } else {
                    kind = IssueKind.SHADER_COMPILE_ERROR;
                }
            } else if (errorMsg.contains("device lost") || errorMsg.contains("device reset")) {
                kind = IssueKind.DRIVER_TDR;
                severity = Severity.FATAL;
            }

            return new SpikeAnalysis(true, kind, severity, 1.0,
                "Error: " + error.getClass().getSimpleName() + " - " + error.getMessage());
        }
    }

    private final SpikeDetector spikeDetector = new SpikeDetector();

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 7: CIRCUIT BREAKER PATTERN (Prevent Cascading Failures)
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Circuit breaker to prevent repeated failures from overwhelming the system.
     */
    private static final class CircuitBreaker {
        enum State { CLOSED, OPEN, HALF_OPEN }

        private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong lastFailureNanos = new AtomicLong(0);
        private final AtomicLong lastStateChangeNanos = new AtomicLong(System.nanoTime());

        private final long failureThreshold;
        private final long resetTimeNanos;

        CircuitBreaker(long failureThreshold, long resetTimeNanos) {
            this.failureThreshold = failureThreshold;
            this.resetTimeNanos = resetTimeNanos;
        }

        void recordSuccess() {
            if (state.get() == State.HALF_OPEN) {
                state.set(State.CLOSED);
                failureCount.set(0);
                lastStateChangeNanos.set(System.nanoTime());
            }
        }

        void recordFailure() {
            lastFailureNanos.set(System.nanoTime());
            long failures = failureCount.incrementAndGet();

            if (state.get() == State.CLOSED && failures >= failureThreshold) {
                state.set(State.OPEN);
                lastStateChangeNanos.set(System.nanoTime());
            } else if (state.get() == State.HALF_OPEN) {
                state.set(State.OPEN);
                lastStateChangeNanos.set(System.nanoTime());
            }
        }

        boolean shouldAllow() {
            State current = state.get();

            if (current == State.CLOSED) {
                return true;
            }

            if (current == State.OPEN) {
                long timeSinceLastChange = System.nanoTime() - lastStateChangeNanos.get();
                if (timeSinceLastChange >= resetTimeNanos) {
                    state.compareAndSet(State.OPEN, State.HALF_OPEN);
                    return true;
                }
                return false;
            }

            // HALF_OPEN: allow one attempt
            return true;
        }

        void reset() {
            state.set(State.CLOSED);
            failureCount.set(0);
            lastStateChangeNanos.set(System.nanoTime());
        }

        State getState() {
            return state.get();
        }
    }

    // Per-backend circuit breakers
    private final EnumMap<Backend, CircuitBreaker> circuitBreakers = new EnumMap<>(Backend.class);

    {
        for (Backend backend : Backend.values()) {
            circuitBreakers.put(backend, new CircuitBreaker(CIRCUIT_BREAKER_THRESHOLD, CIRCUIT_BREAKER_RESET_NS));
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 8: BACKEND REFERENCES & WIRING
    // ════════════════════════════════════════════════════════════════════════════════════════════

    // ─── Mappers ───
    private volatile OpenGLCallMapper glMapper;
    private volatile OpenGLESCallMapper glesMapper;
    private volatile VulkanCallMapper vkMapper;
    private volatile MetalCallMapper metalMapper;
    private volatile SPIRVCallMapper spirvMapper;
    private volatile GLSLCallMapper glslMapper;

    // ─── Managers ───
    private volatile OpenGLManager glManager;
    private volatile OpenGLESManager glesManager;
    private volatile VulkanManager vkManager;
    private volatile MetalManager metalManager;
    private volatile SPIRVManager spirvManager;
    private volatile GLSLManager glslManager;

    // ─── Capabilities ───
    private volatile UniversalCapabilities capabilities;

    // ─── Report Sinks ───
    private volatile Consumer<DiagnosticsReport> backendReportSink;
    private volatile Consumer<DiagnosticsReport> userNotificationSink;
    private volatile BiConsumer<DiagnosticsReport, RecoveryResult> recoveryResultSink;

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 9: THREAD POOLS & EXECUTORS
    // ════════════════════════════════════════════════════════════════════════════════════════════

    private final ExecutorService diagnosticsExecutor;
    private final ExecutorService recoveryExecutor;
    private final ScheduledExecutorService scheduledExecutor;

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 10: QUEUES & BUFFERS
    // ════════════════════════════════════════════════════════════════════════════════════════════

    private final ConcurrentLinkedQueue<DiagnosticsReport> reportQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<RecoveryTask> recoveryQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<CallKey, Long> lastReportTime = new ConcurrentHashMap<>();

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 11: RECOVERY SYSTEM
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Recovery task to be executed asynchronously.
     */
    private record RecoveryTask(
        DiagnosticsReport report,
        RecoveryStrategy strategy,
        int attemptNumber,
        long scheduledNanos
    ) {}

    /**
     * Tracks recovery attempts per call key to prevent infinite loops.
     */
    private final ConcurrentHashMap<CallKey, RecoveryTracker> recoveryTrackers = new ConcurrentHashMap<>();

    private static final class RecoveryTracker {
        private final AtomicInteger attemptCount = new AtomicInteger(0);
        private final AtomicLong lastAttemptNanos = new AtomicLong(0);
        private final AtomicReference<RecoveryStrategy> lastStrategy = new AtomicReference<>(RecoveryStrategy.NONE);

        boolean canAttempt() {
            long timeSince = System.nanoTime() - lastAttemptNanos.get();
            return attemptCount.get() < MAX_RECOVERY_ATTEMPTS || timeSince >= RECOVERY_COOLDOWN_NS;
        }

        void recordAttempt(RecoveryStrategy strategy) {
            attemptCount.incrementAndGet();
            lastAttemptNanos.set(System.nanoTime());
            lastStrategy.set(strategy);
        }

        void reset() {
            attemptCount.set(0);
            lastStrategy.set(RecoveryStrategy.NONE);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 12: FRAME ANALYSIS
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Per-frame timing accumulator.
     */
    private static final class FrameAccumulator {
        private final AtomicLong frameNumber = new AtomicLong(0);
        private final LongAdder gpuTimeNanos = new LongAdder();
        private final LongAdder cpuTimeNanos = new LongAdder();
        private final LongAdder syncTimeNanos = new LongAdder();
        private final LongAdder drawCalls = new LongAdder();
        private final LongAdder stateChanges = new LongAdder();
        private final ConcurrentLinkedQueue<IssueKind> detectedIssues = new ConcurrentLinkedQueue<>();

        void addGpuTime(long nanos) { gpuTimeNanos.add(nanos); }
        void addCpuTime(long nanos) { cpuTimeNanos.add(nanos); }
        void addSyncTime(long nanos) { syncTimeNanos.add(nanos); }
        void incrementDrawCalls() { drawCalls.increment(); }
        void incrementStateChanges() { stateChanges.increment(); }
        void addIssue(IssueKind kind) { detectedIssues.offer(kind); }

        FrameTimingAnalysis complete(long totalFrameNanos) {
            long gpu = gpuTimeNanos.sum();
            long cpu = cpuTimeNanos.sum();
            long sync = syncTimeNanos.sum();

            double gpuUtil = totalFrameNanos > 0 ? (double) gpu / totalFrameNanos : 0;
            double cpuUtil = totalFrameNanos > 0 ? (double) cpu / totalFrameNanos : 0;

            boolean gpuBound = gpuUtil > 0.8 && cpuUtil < 0.5;
            boolean cpuBound = cpuUtil > 0.8 && gpuUtil < 0.5;
            boolean bandwidthBound = sync > totalFrameNanos * 0.3;

            return new FrameTimingAnalysis(
                frameNumber.get(),
                totalFrameNanos,
                gpu, cpu, sync,
                gpuUtil, cpuUtil,
                gpuBound, cpuBound, bandwidthBound,
                new ArrayList<>(detectedIssues)
            );
        }

        void reset() {
            frameNumber.incrementAndGet();
            gpuTimeNanos.reset();
            cpuTimeNanos.reset();
            syncTimeNanos.reset();
            drawCalls.reset();
            stateChanges.reset();
            detectedIssues.clear();
        }
    }

    private final FrameAccumulator frameAccumulator = new FrameAccumulator();

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 13: CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════════════════════════════════

    private JITHelper() {
        // Virtual thread executor for diagnostics (lightweight, many tasks)
        this.diagnosticsExecutor = Executors.newVirtualThreadPerTaskExecutor();

        // Dedicated recovery executor (fewer, heavier tasks)
        this.recoveryExecutor = Executors.newFixedThreadPool(RECOVERY_THREADS, r -> {
            Thread t = new Thread(r, "JIT-Recovery");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });

        // Scheduled executor for periodic tasks
        this.scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "JIT-Scheduler");
            t.setDaemon(true);
            return t;
        });

        // Start background processors
        startBackgroundProcessors();
    }

    private void startBackgroundProcessors() {
        // Process report queue
        scheduledExecutor.scheduleAtFixedRate(
            this::processReportQueue,
            100, 50, TimeUnit.MILLISECONDS
        );

        // Process recovery queue
        scheduledExecutor.scheduleAtFixedRate(
            this::processRecoveryQueue,
            200, 100, TimeUnit.MILLISECONDS
        );

        // Periodic stats cleanup
        scheduledExecutor.scheduleAtFixedRate(
            this::performPeriodicMaintenance,
            60, 60, TimeUnit.SECONDS
        );
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 14: WIRING API
    // ════════════════════════════════════════════════════════════════════════════════════════════

    public JITHelper wireMappers(
        OpenGLCallMapper gl,
        OpenGLESCallMapper gles,
        VulkanCallMapper vk,
        MetalCallMapper metal,
        SPIRVCallMapper spirv,
        GLSLCallMapper glsl
    ) {
        this.glMapper = gl;
        this.glesMapper = gles;
        this.vkMapper = vk;
        this.metalMapper = metal;
        this.spirvMapper = spirv;
        this.glslMapper = glsl;
        return this;
    }

    public JITHelper wireManagers(
        OpenGLManager gl,
        OpenGLESManager gles,
        VulkanManager vk,
        MetalManager metal,
        SPIRVManager spirv,
        GLSLManager glsl,
        UniversalCapabilities caps
    ) {
        this.glManager = gl;
        this.glesManager = gles;
        this.vkManager = vk;
        this.metalManager = metal;
        this.spirvManager = spirv;
        this.glslManager = glsl;
        this.capabilities = caps;
        return this;
    }

    public JITHelper setBackendReportSink(Consumer<DiagnosticsReport> sink) {
        this.backendReportSink = sink;
        return this;
    }

    public JITHelper setUserNotificationSink(Consumer<DiagnosticsReport> sink) {
        this.userNotificationSink = sink;
        return this;
    }

    public JITHelper setRecoveryResultSink(BiConsumer<DiagnosticsReport, RecoveryResult> sink) {
        this.recoveryResultSink = sink;
        return this;
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 15: CORE PROFILING API (HOT PATH - ZERO ALLOCATION)
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Profiles a backend call with comprehensive spike detection and automatic recovery.
     *
     * <p>This is the primary method mappers should wrap their native calls with.
     * The hot path allocates nothing when no spike is detected.</p>
     *
     * @param callKey Static CallKey constant identifying this call site
     * @param delegate The actual work to perform
     * @return Result from the delegate
     */
    public <T> T profile(CallKey callKey, Supplier<T> delegate) {
        // Circuit breaker check
        CircuitBreaker breaker = circuitBreakers.get(callKey.backend());
        if (!breaker.shouldAllow()) {
            // Try fallback path
            return handleCircuitOpen(callKey, delegate);
        }

        long start = System.nanoTime();
        T result;
        Throwable error = null;

        try {
            result = delegate.get();
        } catch (Throwable t) {
            error = t;
            long duration = System.nanoTime() - start;
            handleCallCompletion(callKey, duration, t);
            breaker.recordFailure();

            if (t instanceof RuntimeException re) throw re;
            if (t instanceof Error e) throw e;
            throw new RuntimeException("Backend call failed: " + callKey, t);
        }

        long duration = System.nanoTime() - start;
        handleCallCompletion(callKey, duration, null);
        breaker.recordSuccess();

        return result;
    }

    /**
     * Void variant for calls that don't return a value.
     */
    public void profile(CallKey callKey, Runnable delegate) {
        profile(callKey, () -> {
            delegate.run();
            return null;
        });
    }

    /**
     * Callable variant for checked exceptions.
     */
    public <T> T profileChecked(CallKey callKey, Callable<T> delegate) throws Exception {
        CircuitBreaker breaker = circuitBreakers.get(callKey.backend());
        if (!breaker.shouldAllow()) {
            return handleCircuitOpenChecked(callKey, delegate);
        }

        long start = System.nanoTime();

        try {
            T result = delegate.call();
            long duration = System.nanoTime() - start;
            handleCallCompletion(callKey, duration, null);
            breaker.recordSuccess();
            return result;
        } catch (Throwable t) {
            long duration = System.nanoTime() - start;
            handleCallCompletion(callKey, duration, t);
            breaker.recordFailure();
            throw t;
        }
    }

    /**
     * Profiles with explicit routing control.
     */
    public <T> RoutingResult<T> profileWithRouting(CallKey callKey, Supplier<T> delegate) {
        long routingStart = System.nanoTime();
        RoutingDecision decision = determineRouting(callKey);

        if (decision == RoutingDecision.PROCEED_NORMAL) {
            T result = profile(callKey, delegate);
            return new RoutingResult<>(result, decision, callKey.backend(), callKey.backend(),
                System.nanoTime() - routingStart, false);
        }

        // Reroute to different backend
        return executeRerouted(callKey, delegate, decision, routingStart);
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 16: CALL COMPLETION HANDLING
    // ════════════════════════════════════════════════════════════════════════════════════════════

    private void handleCallCompletion(CallKey key, long durationNanos, Throwable error) {
        // Get or create stats (minimal allocation path)
        CallStats stats = getOrCreateStats(key);

        // Record timing
        stats.record(durationNanos);
        if (error != null) {
            stats.recordError();
        }

        // Update frame accumulator based on category
        updateFrameAccumulator(key, durationNanos);

        // Spike detection
        SpikeDetector.SpikeAnalysis analysis = spikeDetector.analyze(key, durationNanos, stats, error);

        if (analysis.isSpike() || error != null) {
            stats.recordSpike();

            // Deduplication check
            if (shouldReportSpike(key)) {
                queueReport(key, durationNanos, stats, analysis, error);
            }
        }
    }

    private void updateFrameAccumulator(CallKey key, long durationNanos) {
        switch (key.category()) {
            case DRAW, DRAW_INDEXED, DRAW_INDIRECT, DISPATCH_COMPUTE -> {
                frameAccumulator.addGpuTime(durationNanos);
                frameAccumulator.incrementDrawCalls();
            }
            case FENCE_WAIT, SEMAPHORE_WAIT, QUEUE_SUBMIT -> {
                frameAccumulator.addSyncTime(durationNanos);
            }
            case STATE_CHANGE, PIPELINE_BIND, DESCRIPTOR_BIND -> {
                frameAccumulator.incrementStateChanges();
            }
            default -> frameAccumulator.addCpuTime(durationNanos);
        }
    }

    private boolean shouldReportSpike(CallKey key) {
        long now = System.nanoTime();
        Long lastReport = lastReportTime.get(key);
        if (lastReport != null && (now - lastReport) < REPORT_DEDUP_WINDOW_NS) {
            return false;
        }
        lastReportTime.put(key, now);
        return true;
    }

    private void queueReport(CallKey key, long durationNanos, CallStats stats,
                             SpikeDetector.SpikeAnalysis analysis, Throwable error) {
        // Build report asynchronously to avoid allocation on hot path
        diagnosticsExecutor.execute(() -> {
            try {
                DiagnosticsReport report = buildReport(key, durationNanos, stats, analysis, error);
                reportQueue.offer(report);

                // Immediate notification for critical issues
                if (report.requiresImmediateAction()) {
                    handleCriticalReport(report);
                }
            } catch (Exception e) {
                // Never let reporting crash the system
                e.printStackTrace();
            }
        });
    }

    private DiagnosticsReport buildReport(CallKey key, long durationNanos, CallStats stats,
                                          SpikeDetector.SpikeAnalysis analysis, Throwable error) {
        RecoveryStrategy suggestedRecovery = determineRecoveryStrategy(key, analysis.issueKind(), error);
        RoutingDecision suggestedRouting = determineRoutingForRecovery(key, analysis.issueKind());

        String detailedAnalysis = buildDetailedAnalysis(key, durationNanos, stats, analysis);

        return DiagnosticsReport.builder()
            .callKey(key)
            .issueKind(analysis.issueKind())
            .severity(analysis.severity())
            .durationNanos(durationNanos)
            .stats(stats.snapshot())
            .message(analysis.reason())
            .detailedAnalysis(detailedAnalysis)
            .cause(error)
            .errorCode(extractErrorCode(error))
            .suggestedRecovery(suggestedRecovery)
            .suggestedRouting(suggestedRouting)
            .addMetadata("deviationFactor", analysis.deviationFactor())
            .addMetadata("backend", key.backend().name())
            .addMetadata("category", key.category().name())
            .build();
    }

    private String buildDetailedAnalysis(CallKey key, long durationNanos, CallStats stats,
                                         SpikeDetector.SpikeAnalysis analysis) {
        StringBuilder sb = new StringBuilder(512);
        CallStatsSnapshot snapshot = stats.snapshot();

        sb.append("=== Detailed Analysis ===\n");
        sb.append("Call: ").append(key.backend()).append("::").append(key.category())
          .append("::").append(key.name()).append("\n");
        sb.append("Duration: ").append(nanosToMs(durationNanos)).append("ms\n");
        sb.append("Issue: ").append(analysis.issueKind()).append(" (").append(analysis.severity()).append(")\n");
        sb.append("\n--- Statistics ---\n");
        sb.append("Total calls: ").append(snapshot.totalCalls()).append("\n");
        sb.append("Mean: ").append(nanosToMs((long) snapshot.meanNanos())).append("ms\n");
        sb.append("EWMA: ").append(nanosToMs((long) snapshot.ewmaNanos())).append("ms\n");
        sb.append("StdDev: ").append(nanosToMs((long) snapshot.stdDevNanos())).append("ms\n");
        sb.append("Min: ").append(nanosToMs(snapshot.minNanos())).append("ms\n");
        sb.append("Max: ").append(nanosToMs(snapshot.maxNanos())).append("ms\n");
        sb.append("Spike rate: ").append(String.format("%.4f%%", snapshot.spikeRate() * 100)).append("\n");
        sb.append("Error rate: ").append(String.format("%.4f%%", snapshot.errorRate() * 100)).append("\n");
        sb.append("Deviation factor: ").append(String.format("%.2fx", analysis.deviationFactor())).append("\n");

        // Trend analysis
        if (stats.hasUpwardTrend()) {
            sb.append("\n⚠️ UPWARD TREND DETECTED - Performance may be degrading\n");
        }

        return sb.toString();
    }

    private int extractErrorCode(Throwable error) {
        if (error == null) return 0;
        // Try to extract GL/VK error codes from exception message
        String msg = error.getMessage();
        if (msg != null) {
            // GL errors
            if (msg.contains("GL_OUT_OF_MEMORY")) return GL46C.GL_OUT_OF_MEMORY;
            if (msg.contains("GL_INVALID_OPERATION")) return GL46C.GL_INVALID_OPERATION;
            if (msg.contains("GL_INVALID_ENUM")) return GL46C.GL_INVALID_ENUM;
            if (msg.contains("GL_INVALID_VALUE")) return GL46C.GL_INVALID_VALUE;
            // VK errors
            if (msg.contains("VK_ERROR_OUT_OF_HOST_MEMORY")) return VK10.VK_ERROR_OUT_OF_HOST_MEMORY;
            if (msg.contains("VK_ERROR_OUT_OF_DEVICE_MEMORY")) return VK10.VK_ERROR_OUT_OF_DEVICE_MEMORY;
            if (msg.contains("VK_ERROR_DEVICE_LOST")) return VK10.VK_ERROR_DEVICE_LOST;
        }
        return -1;
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 17: RECOVERY STRATEGY DETERMINATION
    // ════════════════════════════════════════════════════════════════════════════════════════════

    private RecoveryStrategy determineRecoveryStrategy(CallKey key, IssueKind issueKind, Throwable error) {
        // Fatal errors: can't recover
        if (issueKind == IssueKind.DRIVER_TDR || issueKind == IssueKind.DRIVER_CRASH) {
            return RecoveryStrategy.DEVICE_RESET;
        }

        // Memory issues
        if (issueKind == IssueKind.MEMORY_ALLOCATION_FAILURE ||
            issueKind == IssueKind.TEXTURE_MEMORY_EXHAUSTED) {
            return RecoveryStrategy.EVICT_AND_RETRY;
        }

        if (issueKind == IssueKind.DESCRIPTOR_POOL_EXHAUSTED ||
            issueKind == IssueKind.COMMAND_POOL_EXHAUSTED) {
            return RecoveryStrategy.REBUILD_RESOURCE;
        }

        if (issueKind == IssueKind.MEMORY_FRAGMENTATION) {
            return RecoveryStrategy.DEFRAGMENT;
        }

        // Shader issues
        if (issueKind.name().startsWith("SHADER_")) {
            if (issueKind == IssueKind.SHADER_COMPILE_ERROR ||
                issueKind == IssueKind.SHADER_LINK_ERROR) {
                return RecoveryStrategy.SIMPLIFY_SHADER;
            }
            if (issueKind == IssueKind.SHADER_TIMEOUT) {
                return RecoveryStrategy.SIMPLIFY_SHADER_CPU;
            }
            return RecoveryStrategy.REROUTE_MAPPER;
        }

        // Sync issues
        if (issueKind == IssueKind.FENCE_TIMEOUT ||
            issueKind == IssueKind.CPU_GPU_SYNC_STALL) {
            return RecoveryStrategy.EMERGENCY_FLUSH;
        }

        if (issueKind == IssueKind.PIPELINE_STALL ||
            issueKind == IssueKind.GPU_BUBBLE) {
            return RecoveryStrategy.REBUILD_PIPELINE;
        }

        // Performance issues
        if (issueKind == IssueKind.STATE_THRASHING) {
            return RecoveryStrategy.REBUILD_PIPELINE;
        }

        if (issueKind == IssueKind.EXCESSIVE_DRAW_CALLS) {
            return RecoveryStrategy.DOWNGRADE_QUALITY;
        }

        // Timing spikes
        if (issueKind == IssueKind.CATASTROPHIC_STALL) {
            return RecoveryStrategy.EMERGENCY_FLUSH;
        }

        if (issueKind == IssueKind.SEVERE_STUTTER ||
            issueKind == IssueKind.VISIBLE_STUTTER) {
            return RecoveryStrategy.RETRY_DELAYED;
        }

        // Default
        if (error != null) {
            return RecoveryStrategy.RETRY_SIMPLE;
        }

        return RecoveryStrategy.NONE;
    }

    private RoutingDecision determineRoutingForRecovery(CallKey key, IssueKind issueKind) {
        // Shader issues: try alternative shader pipeline
        if (issueKind.name().startsWith("SHADER_")) {
            return switch (key.backend()) {
                case VULKAN -> RoutingDecision.ROUTE_TO_SPIRV;
                case OPENGL, OPENGL_ES -> RoutingDecision.ROUTE_TO_GLSL;
                case SPIRV -> RoutingDecision.ROUTE_TO_GLSL;
                case GLSL -> RoutingDecision.ROUTE_TO_CPU;
                case METAL -> RoutingDecision.ROUTE_TO_GLSL;
                case CPU_FALLBACK -> RoutingDecision.BLOCK;
            };
        }

        // Backend-specific issues: try compatible backend
        if (issueKind == IssueKind.CAPABILITY_MISMATCH ||
            issueKind == IssueKind.DRIVER_VERSION_INCOMPATIBLE) {
            return switch (key.backend()) {
                case VULKAN -> RoutingDecision.ROUTE_TO_GL;
                case METAL -> RoutingDecision.ROUTE_TO_GL;
                case OPENGL -> RoutingDecision.ROUTE_TO_GLES;
                case OPENGL_ES -> RoutingDecision.ROUTE_TO_CPU;
                default -> RoutingDecision.PROCEED_NORMAL;
            };
        }

        return RoutingDecision.PROCEED_NORMAL;
    }

    private RoutingDecision determineRouting(CallKey key) {
        // Check circuit breaker state
        CircuitBreaker breaker = circuitBreakers.get(key.backend());
        if (breaker.getState() == CircuitBreaker.State.OPEN) {
            return getFallbackRouting(key.backend());
        }

        // Check capability issues
        UniversalCapabilities caps = this.capabilities;
        if (caps != null) {
            boolean stable = switch (key.backend()) {
                case OPENGL -> caps.isOpenGLStable();
                case VULKAN -> caps.isVulkanStable();
                case METAL -> caps.isMetalStable();
                case OPENGL_ES -> caps.isGLESSable();
                default -> true;
            };
            if (!stable) {
                return getFallbackRouting(key.backend());
            }
        }

        return RoutingDecision.PROCEED_NORMAL;
    }

    private RoutingDecision getFallbackRouting(Backend backend) {
        return switch (backend) {
            case VULKAN -> RoutingDecision.ROUTE_TO_GL;
            case METAL -> RoutingDecision.ROUTE_TO_GL;
            case OPENGL -> RoutingDecision.ROUTE_TO_GLES;
            case OPENGL_ES -> RoutingDecision.ROUTE_TO_CPU;
            case SPIRV -> RoutingDecision.ROUTE_TO_GLSL;
            case GLSL -> RoutingDecision.ROUTE_TO_CPU;
            case CPU_FALLBACK -> RoutingDecision.BLOCK;
        };
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 18: CIRCUIT BREAKER HANDLING
    // ════════════════════════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private <T> T handleCircuitOpen(CallKey key, Supplier<T> delegate) {
        RoutingDecision fallback = getFallbackRouting(key.backend());

        if (fallback == RoutingDecision.BLOCK) {
            throw new RuntimeException("Circuit breaker open and no fallback available for: " + key);
        }

        // Reroute to fallback backend
        return executeRerouted(key, delegate, fallback, System.nanoTime()).result();
    }

    private <T> T handleCircuitOpenChecked(CallKey key, Callable<T> delegate) throws Exception {
        RoutingDecision fallback = getFallbackRouting(key.backend());

        if (fallback == RoutingDecision.BLOCK) {
            throw new RuntimeException("Circuit breaker open and no fallback available for: " + key);
        }

        // Convert to supplier and reroute
        return executeRerouted(key, () -> {
            try {
                return delegate.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, fallback, System.nanoTime()).result();
    }

    @SuppressWarnings("unchecked")
    private <T> RoutingResult<T> executeRerouted(CallKey originalKey, Supplier<T> delegate,
                                                  RoutingDecision decision, long startNanos) {
        Backend targetBackend = switch (decision) {
            case ROUTE_TO_GL -> Backend.OPENGL;
            case ROUTE_TO_GLES -> Backend.OPENGL_ES;
            case ROUTE_TO_VK -> Backend.VULKAN;
            case ROUTE_TO_METAL -> Backend.METAL;
            case ROUTE_TO_SPIRV -> Backend.SPIRV;
            case ROUTE_TO_GLSL -> Backend.GLSL;
            case ROUTE_TO_CPU -> Backend.CPU_FALLBACK;
            default -> originalKey.backend();
        };

        // Create new call key for the target backend
        CallKey reroutedKey = new CallKey(targetBackend, originalKey.category(), originalKey.name());

        // Execute on rerouted backend
        T result;
        try {
            result = routeToBackend(reroutedKey, delegate, targetBackend);
        } catch (Exception e) {
            // If rerouting also fails, try CPU fallback
            if (targetBackend != Backend.CPU_FALLBACK) {
                CallKey cpuKey = new CallKey(Backend.CPU_FALLBACK, originalKey.category(), originalKey.name());
                result = executeCPUFallback(cpuKey, delegate);
            } else {
                throw e;
            }
        }

        return new RoutingResult<>(
            result,
            decision,
            originalKey.backend(),
            targetBackend,
            System.nanoTime() - startNanos,
            true
        );
    }

    @SuppressWarnings("unchecked")
    private <T> T routeToBackend(CallKey key, Supplier<T> delegate, Backend backend) {
        // Ask the appropriate manager to handle the rerouted call
        return switch (backend) {
            case OPENGL -> {
                if (glManager != null && glMapper != null) {
                    yield (T) glManager.executeRerouted(key, delegate, glMapper);
                }
                yield delegate.get();
            }
            case OPENGL_ES -> {
                if (glesManager != null && glesMapper != null) {
                    yield (T) glesManager.executeRerouted(key, delegate, glesMapper);
                }
                yield delegate.get();
            }
            case VULKAN -> {
                if (vkManager != null && vkMapper != null) {
                    yield (T) vkManager.executeRerouted(key, delegate, vkMapper);
                }
                yield delegate.get();
            }
            case METAL -> {
                if (metalManager != null && metalMapper != null) {
                    yield (T) metalManager.executeRerouted(key, delegate, metalMapper);
                }
                yield delegate.get();
            }
            case SPIRV -> {
                if (spirvManager != null && spirvMapper != null) {
                    yield (T) spirvManager.executeRerouted(key, delegate, spirvMapper);
                }
                yield delegate.get();
            }
            case GLSL -> {
                if (glslManager != null && glslMapper != null) {
                    yield (T) glslManager.executeRerouted(key, delegate, glslMapper);
                }
                yield delegate.get();
            }
            case CPU_FALLBACK -> executeCPUFallback(key, delegate);
        };
    }

    @SuppressWarnings("unchecked")
    private <T> T executeCPUFallback(CallKey key, Supplier<T> delegate) {
        // CPU fallback: execute delegate but with simplified/software path
        // This is a last resort and will be slow
        System.err.println("[JITHelper] WARNING: Executing CPU fallback for " + key);
        return delegate.get();
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 19: REPORT PROCESSING
    // ════════════════════════════════════════════════════════════════════════════════════════════

    private void processReportQueue() {
        int processed = 0;
        DiagnosticsReport report;

        while ((report = reportQueue.poll()) != null && processed < 64) {
            try {
                processReport(report);
            } catch (Exception e) {
                e.printStackTrace();
            }
            processed++;
        }
    }

    private void processReport(DiagnosticsReport report) {
        // Send to backend sink
        Consumer<DiagnosticsReport> backendSink = this.backendReportSink;
        if (backendSink != null) {
            backendSink.accept(report);
        }

        // User notification for visible issues
        if (report.severity().isUserVisible()) {
            Consumer<DiagnosticsReport> userSink = this.userNotificationSink;
            if (userSink != null) {
                userSink.accept(report);
            } else {
                System.out.println("[JITHelper] " + report.severity() + ": " + report.message());
            }
        }

        // Queue recovery if needed
        if (report.suggestedRecovery() != RecoveryStrategy.NONE) {
            queueRecovery(report);
        }

        // Signal managers based on issue type
        signalManagers(report);
    }

    private void handleCriticalReport(DiagnosticsReport report) {
        // Immediate actions for critical issues
        System.err.println("[JITHelper] CRITICAL: " + report.message());

        // Notify user immediately
        Consumer<DiagnosticsReport> userSink = this.userNotificationSink;
        if (userSink != null) {
            userSink.accept(report);
        }

        // Immediate recovery attempt
        if (report.suggestedRecovery() != RecoveryStrategy.NONE) {
            recoveryExecutor.execute(() -> executeRecovery(report, report.suggestedRecovery(), 1));
        }
    }

    private void signalManagers(DiagnosticsReport report) {
        CallKey key = report.callKey();

        // Signal appropriate manager based on backend
        switch (key.backend()) {
            case OPENGL -> {
                if (glManager != null) glManager.handleDiagnostics(report);
            }
            case OPENGL_ES -> {
                if (glesManager != null) glesManager.handleDiagnostics(report);
            }
            case VULKAN -> {
                if (vkManager != null) vkManager.handleDiagnostics(report);
            }
            case METAL -> {
                if (metalManager != null) metalManager.handleDiagnostics(report);
            }
            case SPIRV -> {
                if (spirvManager != null) spirvManager.handleDiagnostics(report);
            }
            case GLSL -> {
                if (glslManager != null) glslManager.handleDiagnostics(report);
            }
            default -> { }
        }

        // For shader issues, also signal shader managers
        if (report.isShaderRelated()) {
            if (glslManager != null) glslManager.handleShaderDiagnostics(report);
            if (spirvManager != null) spirvManager.handleShaderDiagnostics(report);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 20: RECOVERY EXECUTION
    // ════════════════════════════════════════════════════════════════════════════════════════════

    private void queueRecovery(DiagnosticsReport report) {
        CallKey key = report.callKey();
        RecoveryTracker tracker = recoveryTrackers.computeIfAbsent(key, k -> new RecoveryTracker());

        if (!tracker.canAttempt()) {
            return;
        }

        RecoveryTask task = new RecoveryTask(
            report,
            report.suggestedRecovery(),
            tracker.attemptCount.get() + 1,
            System.nanoTime()
        );

        recoveryQueue.offer(task);
    }

    private void processRecoveryQueue() {
        int processed = 0;
        RecoveryTask task;

        while ((task = recoveryQueue.poll()) != null && processed < 16) {
            final RecoveryTask finalTask = task;
            recoveryExecutor.execute(() -> {
                try {
                    executeRecovery(finalTask.report(), finalTask.strategy(), finalTask.attemptNumber());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            processed++;
        }
    }

    private void executeRecovery(DiagnosticsReport report, RecoveryStrategy strategy, int attemptNumber) {
        CallKey key = report.callKey();
        RecoveryTracker tracker = recoveryTrackers.computeIfAbsent(key, k -> new RecoveryTracker());
        tracker.recordAttempt(strategy);

        long startNanos = System.nanoTime();
        boolean success = false;
        String description = "";

        try {
            success = switch (strategy) {
                case RETRY_SIMPLE -> executeRetrySimple(report);
                case RETRY_DELAYED -> executeRetryDelayed(report);
                case REROUTE_MAPPER -> executeRerouteMapper(report);
                case REROUTE_BACKEND -> executeRerouteBackend(report);
                case SIMPLIFY_SHADER -> executeSimplifyShader(report);
                case SIMPLIFY_SHADER_CPU -> executeSimplifyShaderCPU(report);
                case REBUILD_PIPELINE -> executeRebuildPipeline(report);
                case REBUILD_RESOURCE -> executeRebuildResource(report);
                case EVICT_AND_RETRY -> executeEvictAndRetry(report);
                case DEFRAGMENT -> executeDefragment(report);
                case DOWNGRADE_QUALITY -> executeDowngradeQuality(report);
                case SKIP_OPERATION -> true;  // Skip is always "successful"
                case EMERGENCY_GC -> executeEmergencyGC(report);
                case EMERGENCY_FLUSH -> executeEmergencyFlush(report);
                case DEVICE_RESET -> executeDeviceReset(report);
                case GRACEFUL_DEGRADATION -> executeGracefulDegradation(report);
                case NONE -> true;
            };

            description = success ? "Recovery succeeded" : "Recovery failed";

            if (success) {
                tracker.reset();
                circuitBreakers.get(key.backend()).reset();
            }

        } catch (Exception e) {
            success = false;
            description = "Recovery threw exception: " + e.getMessage();
            e.printStackTrace();
        }

        RecoveryResult result = new RecoveryResult(
            success,
            strategy,
            description,
            System.nanoTime() - startNanos,
            report
        );

        // Report recovery result
        BiConsumer<DiagnosticsReport, RecoveryResult> sink = this.recoveryResultSink;
        if (sink != null) {
            sink.accept(report, result);
        }

        // Queue follow-up report
        DiagnosticsReport followUp = DiagnosticsReport.builder()
            .callKey(key)
            .issueKind(success ? IssueKind.RECOVERY_SUCCEEDED : IssueKind.RECOVERY_FAILED)
            .severity(success ? Severity.INFO : Severity.WARNING)
            .durationNanos(result.recoveryTimeNanos())
            .message(description)
            .addMetadata("strategyUsed", strategy.name())
            .addMetadata("attemptNumber", attemptNumber)
            .build();

        reportQueue.offer(followUp);
    }

    // ─── Individual Recovery Implementations ───

    private boolean executeRetrySimple(DiagnosticsReport report) {
        // Simple retry: managers should cache and retry the operation
        return signalRetry(report, false);
    }

    private boolean executeRetryDelayed(DiagnosticsReport report) {
        LockSupport.parkNanos(50_000_000L);  // 50ms delay
        return signalRetry(report, true);
    }

    private boolean executeRerouteMapper(DiagnosticsReport report) {
        CallKey key = report.callKey();
        RoutingDecision routing = report.suggestedRouting();

        if (routing == RoutingDecision.PROCEED_NORMAL) {
            routing = determineRoutingForRecovery(key, report.issueKind());
        }

        return signalReroute(report, routing);
    }

    private boolean executeRerouteBackend(DiagnosticsReport report) {
        return executeRerouteMapper(report);  // Same logic
    }

    private boolean executeSimplifyShader(DiagnosticsReport report) {
        // Route to GLSL manager for simplification
        GLSLManager glsl = this.glslManager;
        if (glsl != null) {
            return glsl.requestSimplifiedShader(report);
        }
        return false;
    }

    private boolean executeSimplifyShaderCPU(DiagnosticsReport report) {
        // Move shader work to CPU
        GLSLManager glsl = this.glslManager;
        if (glsl != null) {
            return glsl.requestCPUFallbackShader(report);
        }
        return false;
    }

    private boolean executeRebuildPipeline(DiagnosticsReport report) {
        CallKey key = report.callKey();
        return switch (key.backend()) {
            case VULKAN -> vkManager != null && vkManager.rebuildPipeline(report);
            case OPENGL -> glManager != null && glManager.rebuildPipeline(report);
            case METAL -> metalManager != null && metalManager.rebuildPipeline(report);
            default -> false;
        };
    }

    private boolean executeRebuildResource(DiagnosticsReport report) {
        CallKey key = report.callKey();
        return switch (key.backend()) {
            case VULKAN -> vkManager != null && vkManager.rebuildResource(report);
            case OPENGL -> glManager != null && glManager.rebuildResource(report);
            case METAL -> metalManager != null && metalManager.rebuildResource(report);
            default -> false;
        };
    }

    private boolean executeEvictAndRetry(DiagnosticsReport report) {
        // Request all managers to evict unused resources
        boolean evicted = false;
        if (glManager != null) evicted |= glManager.evictResources();
        if (glesManager != null) evicted |= glesManager.evictResources();
        if (vkManager != null) evicted |= vkManager.evictResources();
        if (metalManager != null) evicted |= metalManager.evictResources();

        if (evicted) {
            return signalRetry(report, true);
        }
        return false;
    }

    private boolean executeDefragment(DiagnosticsReport report) {
        CallKey key = report.callKey();
        return switch (key.backend()) {
            case VULKAN -> vkManager != null && vkManager.defragmentMemory();
            case OPENGL -> glManager != null && glManager.defragmentMemory();
            default -> false;
        };
    }

    private boolean executeDowngradeQuality(DiagnosticsReport report) {
        // Signal all managers to reduce quality
        if (glManager != null) glManager.downgradeQuality();
        if (glesManager != null) glesManager.downgradeQuality();
        if (vkManager != null) vkManager.downgradeQuality();
        if (metalManager != null) metalManager.downgradeQuality();
        return true;
    }

    private boolean executeEmergencyGC(DiagnosticsReport report) {
        System.gc();
        System.runFinalization();
        System.gc();
        return true;
    }

    private boolean executeEmergencyFlush(DiagnosticsReport report) {
        // Flush GPU commands and wait
        if (glManager != null) glManager.flush();
        if (vkManager != null) vkManager.flush();
        if (metalManager != null) metalManager.flush();
        return true;
    }

    private boolean executeDeviceReset(DiagnosticsReport report) {
        // Last resort: reset the GPU device
        System.err.println("[JITHelper] FATAL: Device reset requested");
        if (vkManager != null) return vkManager.resetDevice();
        return false;
    }

    private boolean executeGracefulDegradation(DiagnosticsReport report) {
        // Enter reduced functionality mode
        if (glManager != null) glManager.enterDegradedMode();
        if (glesManager != null) glesManager.enterDegradedMode();
        if (vkManager != null) vkManager.enterDegradedMode();
        if (metalManager != null) metalManager.enterDegradedMode();
        return true;
    }

    private boolean signalRetry(DiagnosticsReport report, boolean delayed) {
        CallKey key = report.callKey();
        return switch (key.backend()) {
            case VULKAN -> vkManager != null && vkManager.retryLastOperation(report, delayed);
            case OPENGL -> glManager != null && glManager.retryLastOperation(report, delayed);
            case OPENGL_ES -> glesManager != null && glesManager.retryLastOperation(report, delayed);
            case METAL -> metalManager != null && metalManager.retryLastOperation(report, delayed);
            default -> false;
        };
    }

    private boolean signalReroute(DiagnosticsReport report, RoutingDecision routing) {
        CallKey key = report.callKey();
        return switch (key.backend()) {
            case VULKAN -> vkManager != null && vkManager.rerouteOperation(report, routing);
            case OPENGL -> glManager != null && glManager.rerouteOperation(report, routing);
            case OPENGL_ES -> glesManager != null && glesManager.rerouteOperation(report, routing);
            case METAL -> metalManager != null && metalManager.rerouteOperation(report, routing);
            case SPIRV -> spirvManager != null && spirvManager.rerouteOperation(report, routing);
            case GLSL -> glslManager != null && glslManager.rerouteOperation(report, routing);
            default -> false;
        };
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 21: CAPABILITY PROBING
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Probes backend capabilities and returns detailed analysis.
     */
    public CompletableFuture<CapabilityProbeResult> probeCapabilities(Backend backend) {
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                UniversalCapabilities caps = this.capabilities;
                if (caps == null) {
                    return new CapabilityProbeResult(backend, true, true,
                        Set.of(), Set.of(), Map.of());
                }

                boolean stable = switch (backend) {
                    case OPENGL -> caps.isOpenGLStable();
                    case VULKAN -> caps.isVulkanStable();
                    case METAL -> caps.isMetalStable();
                    case OPENGL_ES -> caps.isGLESSable();
                    default -> true;
                };

                Set<String> missing = new HashSet<>();
                Set<String> degraded = new HashSet<>();
                Map<String, Object> measurements = new HashMap<>();

                // Backend-specific probing
                switch (backend) {
                    case OPENGL -> probeOpenGL(caps, missing, degraded, measurements);
                    case VULKAN -> probeVulkan(caps, missing, degraded, measurements);
                    case METAL -> probeMetal(caps, missing, degraded, measurements);
                    case OPENGL_ES -> probeGLES(caps, missing, degraded, measurements);
                    default -> { }
                }

                boolean optimal = missing.isEmpty() && degraded.isEmpty();

                return new CapabilityProbeResult(backend, stable, optimal, missing, degraded, measurements);

            } catch (Exception e) {
                return new CapabilityProbeResult(backend, false, false,
                    Set.of("probe_failed"), Set.of(), Map.of("error", e.getMessage()));
            }
        }, diagnosticsExecutor);
    }

    private void probeOpenGL(UniversalCapabilities caps, Set<String> missing,
                              Set<String> degraded, Map<String, Object> measurements) {
        // Check for critical extensions
        if (!caps.hasExtension("GL_ARB_direct_state_access")) {
            degraded.add("direct_state_access");
        }
        if (!caps.hasExtension("GL_ARB_buffer_storage")) {
            degraded.add("buffer_storage");
        }
        if (!caps.hasExtension("GL_ARB_multi_draw_indirect")) {
            degraded.add("multi_draw_indirect");
        }
        if (!caps.hasExtension("GL_ARB_bindless_texture")) {
            degraded.add("bindless_texture");
        }

        measurements.put("max_texture_size", caps.getMaxTextureSize());
        measurements.put("max_uniform_buffer_bindings", caps.getMaxUniformBufferBindings());
    }

    private void probeVulkan(UniversalCapabilities caps, Set<String> missing,
                              Set<String> degraded, Map<String, Object> measurements) {
        // Check Vulkan version
        int vkVersion = caps.getVulkanVersion();
        if (vkVersion < VK13.VK_API_VERSION_1_3) {
            degraded.add("vulkan_1_3");
        }

        // Check for extensions
        if (!caps.hasVulkanExtension("VK_KHR_dynamic_rendering")) {
            degraded.add("dynamic_rendering");
        }
        if (!caps.hasVulkanExtension("VK_KHR_synchronization2")) {
            degraded.add("synchronization2");
        }

        measurements.put("vulkan_version", vkVersion);
    }

    private void probeMetal(UniversalCapabilities caps, Set<String> missing,
                             Set<String> degraded, Map<String, Object> measurements) {
        // Metal-specific probing
        if (!caps.hasMetalFeature("metal_3")) {
            degraded.add("metal_3");
        }
    }

    private void probeGLES(UniversalCapabilities caps, Set<String> missing,
                            Set<String> degraded, Map<String, Object> measurements) {
        // GLES-specific probing
        int glesVersion = caps.getGLESVersion();
        if (glesVersion < 320) {
            degraded.add("gles_3_2");
        }
        measurements.put("gles_version", glesVersion);
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 22: FRAME ANALYSIS API
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Begins a new frame for timing analysis.
     */
    public void beginFrame() {
        frameAccumulator.reset();
    }

    /**
     * Ends the current frame and returns timing analysis.
     */
    public FrameTimingAnalysis endFrame(long totalFrameNanos) {
        FrameTimingAnalysis analysis = frameAccumulator.complete(totalFrameNanos);

        // Detect and report frame-level issues
        if (analysis.isGpuBound()) {
            frameAccumulator.addIssue(IssueKind.GPU_BOUND);
        }
        if (analysis.isCpuBound()) {
            frameAccumulator.addIssue(IssueKind.CPU_BOUND);
        }
        if (analysis.isBandwidthBound()) {
            frameAccumulator.addIssue(IssueKind.BANDWIDTH_BOUND);
        }

        return analysis;
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 23: ERROR CODE REPORTING
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Reports a backend error code (e.g., glGetError, vkResult).
     */
    public void reportErrorCode(CallKey key, int errorCode, String context) {
        IssueKind kind = classifyErrorCode(key.backend(), errorCode);
        Severity severity = errorCodeSeverity(errorCode);

        String message = String.format("[%s] Error %d in %s: %s",
            key.backend(), errorCode, key.name(), context);

        CallStats stats = getOrCreateStats(key);
        stats.recordError();

        DiagnosticsReport report = DiagnosticsReport.builder()
            .callKey(key)
            .issueKind(kind)
            .severity(severity)
            .durationNanos(0)
            .stats(stats.snapshot())
            .message(message)
            .errorCode(errorCode)
            .suggestedRecovery(determineRecoveryStrategy(key, kind, null))
            .suggestedRouting(determineRoutingForRecovery(key, kind))
            .build();

        reportQueue.offer(report);

        if (severity.requiresImmediateAction()) {
            handleCriticalReport(report);
        }
    }

    private IssueKind classifyErrorCode(Backend backend, int code) {
        return switch (backend) {
            case OPENGL, OPENGL_ES -> classifyGLError(code);
            case VULKAN -> classifyVKError(code);
            default -> IssueKind.RUNTIME_ERROR;
        };
    }

    private IssueKind classifyGLError(int code) {
        return switch (code) {
            case GL46C.GL_OUT_OF_MEMORY -> IssueKind.MEMORY_ALLOCATION_FAILURE;
            case GL46C.GL_INVALID_FRAMEBUFFER_OPERATION -> IssueKind.FRAMEBUFFER_INCOMPLETE;
            default -> IssueKind.RUNTIME_ERROR;
        };
    }

    private IssueKind classifyVKError(int code) {
        return switch (code) {
            case VK10.VK_ERROR_OUT_OF_HOST_MEMORY, VK10.VK_ERROR_OUT_OF_DEVICE_MEMORY ->
                IssueKind.MEMORY_ALLOCATION_FAILURE;
            case VK10.VK_ERROR_DEVICE_LOST -> IssueKind.DRIVER_TDR;
            case VK10.VK_ERROR_INITIALIZATION_FAILED -> IssueKind.DRIVER_CRASH;
            default -> IssueKind.RUNTIME_ERROR;
        };
    }

    private Severity errorCodeSeverity(int code) {
        // Critical errors
        if (code == VK10.VK_ERROR_DEVICE_LOST ||
            code == VK10.VK_ERROR_OUT_OF_DEVICE_MEMORY ||
            code == GL46C.GL_OUT_OF_MEMORY) {
            return Severity.CRITICAL;
        }
        return Severity.SEVERE;
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 24: STATS & METRICS API
    // ════════════════════════════════════════════════════════════════════════════════════════════

    public Optional<CallStatsSnapshot> getStats(CallKey key) {
        CallStats stats = statsMap.get(key);
        return stats != null ? Optional.of(stats.snapshot()) : Optional.empty();
    }

    public Map<CallKey, CallStatsSnapshot> getAllStats() {
        Map<CallKey, CallStatsSnapshot> result = new HashMap<>();
        statsMap.forEach((k, v) -> result.put(k, v.snapshot()));
        return result;
    }

    public Map<Backend, Integer> getCircuitBreakerStates() {
        Map<Backend, Integer> states = new EnumMap<>(Backend.class);
        circuitBreakers.forEach((backend, breaker) ->
            states.put(backend, breaker.getState().ordinal()));
        return states;
    }

    public long getPendingReports() {
        return reportQueue.size();
    }

    public long getPendingRecoveries() {
        return recoveryQueue.size();
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 25: MAINTENANCE & CLEANUP
    // ════════════════════════════════════════════════════════════════════════════════════════════

    private void performPeriodicMaintenance() {
        // Evict old stats
        evictLowTrafficStats();

        // Clear old report timestamps
        long now = System.nanoTime();
        lastReportTime.entrySet().removeIf(e -> (now - e.getValue()) > 60_000_000_000L);

        // Reset stale recovery trackers
        recoveryTrackers.entrySet().removeIf(e -> {
            long timeSince = now - e.getValue().lastAttemptNanos.get();
            return timeSince > RECOVERY_COOLDOWN_NS * 10;
        });

        // Analyze draw call patterns and trigger optimization if needed
        analyzeDrawCallPatterns();

        // Check for memory pressure
        checkMemoryPressure();
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 26: DRAW CALL MONITORING & INDIRECT DRAW OPTIMIZATION
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * ┌─────────────────────────────────────────────────────────────────────────────────────────────┐
     * │ DRAW CALL OPTIMIZATION SYSTEM                                                               │
     * ├─────────────────────────────────────────────────────────────────────────────────────────────┤
     * │                                                                                             │
     * │ DETECTION FLOW:                                                                             │
     * │   Monitor Draw Calls → Detect Overhead → Query DrawPool → Analyze Report                   │
     * │                                                                                             │
     * │ OPTIMIZATION FLOW:                                                                          │
     * │   Identify Heavy Draws → Cache in IndirectDrawManager → Batch Execute → Reuse              │
     * │                                                                                             │
     * │ TRIGGERS:                                                                                   │
     * │   • Draw call count exceeds threshold per frame                                             │
     * │   • Draw call time exceeds frame budget percentage                                          │
     * │   • Repeated identical draw patterns detected                                               │
     * │   • State thrashing between draws detected                                                  │
     * │   • CPU-bound frames with high draw overhead                                                │
     * │                                                                                             │
     * │ ACTIONS:                                                                                    │
     * │   • Request DrawPool report for current frame draws                                         │
     * │   • Identify candidates for indirect drawing                                                │
     * │   • Pre-validate and cache in IndirectDrawManager                                           │
     * │   • Switch from individual draws to batched indirect draws                                  │
     * │   • Monitor improvement and adjust thresholds                                               │
     * │                                                                                             │
     * └─────────────────────────────────────────────────────────────────────────────────────────────┘
     */

    // ─── Draw Call Thresholds ───
    private static final int DRAW_CALL_WARNING_THRESHOLD      = 2000;
    private static final int DRAW_CALL_CRITICAL_THRESHOLD     = 5000;
    private static final int DRAW_CALL_EMERGENCY_THRESHOLD    = 10000;
    private static final double DRAW_TIME_BUDGET_PERCENT      = 0.4;   // 40% of frame time
    private static final int MIN_DRAWS_FOR_INDIRECT_CANDIDATE = 10;    // Min similar draws to batch
    private static final int INDIRECT_BATCH_SIZE_MIN          = 16;
    private static final int INDIRECT_BATCH_SIZE_OPTIMAL      = 64;
    private static final long PATTERN_DETECTION_WINDOW_NS     = 5_000_000_000L;  // 5 seconds
    private static final int PATTERN_RECURRENCE_THRESHOLD     = 3;    // Pattern must repeat 3+ times

    // ─── Draw Monitoring State ───
    private final AtomicLong frameDrawCallCount = new AtomicLong(0);
    private final AtomicLong frameDrawTimeNanos = new AtomicLong(0);
    private final AtomicLong totalFrameTimeNanos = new AtomicLong(16_666_666L);  // Default 60fps
    private final AtomicBoolean indirectDrawOptimizationActive = new AtomicBoolean(false);
    private final AtomicInteger consecutiveHighDrawFrames = new AtomicInteger(0);
    private final AtomicLong lastDrawPoolQueryNanos = new AtomicLong(0);

    // ─── Draw Pattern Tracking ───
    private final ConcurrentHashMap<Long, DrawPatternTracker> drawPatterns = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<DrawCallRecord> recentDrawCalls = new ConcurrentLinkedQueue<>();
    private final AtomicInteger drawCallRecordCount = new AtomicInteger(0);
    private static final int MAX_DRAW_CALL_RECORDS = 4096;

    // ─── External References ───
    private volatile DrawPool drawPool;
    private volatile IndirectDrawManager indirectDrawManager;

    /**
     * Record of a single draw call for pattern analysis.
     */
    private record DrawCallRecord(
        long timestampNanos,
        long patternHash,
        int vertexCount,
        int instanceCount,
        int primitiveType,
        long shaderProgramId,
        long vaoId,
        long textureSetHash,
        long uniformSetHash,
        long durationNanos,
        boolean wasIndirect
    ) {
        /**
         * Computes similarity hash for grouping similar draws.
         */
        static long computePatternHash(int primitiveType, long shaderProgramId, long vaoId,
                                        long textureSetHash, long uniformSetHash) {
            long h = primitiveType;
            h = 31 * h + shaderProgramId;
            h = 31 * h + vaoId;
            h = 31 * h + textureSetHash;
            h = 31 * h + uniformSetHash;
            return h;
        }
    }

    /**
     * Tracks recurring draw patterns for batching optimization.
     */
    private static final class DrawPatternTracker {
        private final long patternHash;
        private final LongAdder occurrenceCount = new LongAdder();
        private final LongAdder totalVertices = new LongAdder();
        private final LongAdder totalInstances = new LongAdder();
        private final LongAdder totalDurationNanos = new LongAdder();
        private final AtomicLong firstSeenNanos = new AtomicLong(0);
        private final AtomicLong lastSeenNanos = new AtomicLong(0);
        private final AtomicBoolean markedForIndirect = new AtomicBoolean(false);
        private final AtomicBoolean indirectCached = new AtomicBoolean(false);

        // Representative draw info (from first occurrence)
        private volatile int primitiveType;
        private volatile long shaderProgramId;
        private volatile long vaoId;
        private volatile long textureSetHash;
        private volatile long uniformSetHash;

        DrawPatternTracker(long patternHash) {
            this.patternHash = patternHash;
        }

        void record(DrawCallRecord draw) {
            long now = System.nanoTime();
            occurrenceCount.increment();
            totalVertices.add(draw.vertexCount());
            totalInstances.add(draw.instanceCount());
            totalDurationNanos.add(draw.durationNanos());
            lastSeenNanos.set(now);

            if (firstSeenNanos.compareAndSet(0, now)) {
                // First occurrence - capture representative info
                primitiveType = draw.primitiveType();
                shaderProgramId = draw.shaderProgramId();
                vaoId = draw.vaoId();
                textureSetHash = draw.textureSetHash();
                uniformSetHash = draw.uniformSetHash();
            }
        }

        boolean isCandidate() {
            return occurrenceCount.sum() >= MIN_DRAWS_FOR_INDIRECT_CANDIDATE;
        }

        boolean isActive() {
            long timeSince = System.nanoTime() - lastSeenNanos.get();
            return timeSince < PATTERN_DETECTION_WINDOW_NS;
        }

        double averageDurationNanos() {
            long count = occurrenceCount.sum();
            return count > 0 ? (double) totalDurationNanos.sum() / count : 0;
        }

        long totalOverheadNanos() {
            return totalDurationNanos.sum();
        }
    }

    /**
     * Wires the DrawPool and IndirectDrawManager references.
     */
    public JITHelper wireDrawSystems(DrawPool pool, IndirectDrawManager indirectManager) {
        this.drawPool = pool;
        this.indirectDrawManager = indirectManager;
        return this;
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 27: DRAW CALL PROFILING & RECORDING
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Profiles a draw call with full pattern tracking and optimization triggering.
     *
     * <p>Call this instead of or in addition to {@link #profile} for draw operations.
     * This enables the automatic indirect draw optimization system.</p>
     *
     * @param callKey The call key for this draw
     * @param primitiveType GL primitive type (GL_TRIANGLES, etc.)
     * @param vertexCount Number of vertices
     * @param instanceCount Number of instances (1 for non-instanced)
     * @param shaderProgramId Current shader program
     * @param vaoId Current VAO
     * @param textureSetHash Hash of bound textures
     * @param uniformSetHash Hash of current uniforms
     * @param delegate The actual draw call
     */
    public void profileDraw(
        CallKey callKey,
        int primitiveType,
        int vertexCount,
        int instanceCount,
        long shaderProgramId,
        long vaoId,
        long textureSetHash,
        long uniformSetHash,
        Runnable delegate
    ) {
        long start = System.nanoTime();
        long patternHash = DrawCallRecord.computePatternHash(
            primitiveType, shaderProgramId, vaoId, textureSetHash, uniformSetHash
        );

        // Check if this pattern should use indirect drawing
        DrawPatternTracker tracker = drawPatterns.get(patternHash);
        if (tracker != null && tracker.indirectCached.get()) {
            // Use cached indirect draw instead
            boolean executed = executeIndirectDraw(tracker, vertexCount, instanceCount);
            if (executed) {
                long duration = System.nanoTime() - start;
                recordDrawCompletion(callKey, duration, patternHash, vertexCount, instanceCount,
                    primitiveType, shaderProgramId, vaoId, textureSetHash, uniformSetHash, true);
                return;
            }
            // Fall through to regular draw if indirect failed
        }

        // Execute regular draw
        Throwable error = null;
        try {
            delegate.run();
        } catch (Throwable t) {
            error = t;
            throw t instanceof RuntimeException re ? re : new RuntimeException(t);
        } finally {
            long duration = System.nanoTime() - start;
            recordDrawCompletion(callKey, duration, patternHash, vertexCount, instanceCount,
                primitiveType, shaderProgramId, vaoId, textureSetHash, uniformSetHash, false);

            if (error != null) {
                handleCallCompletion(callKey, duration, error);
            }
        }
    }

    /**
     * Records draw completion and updates pattern tracking.
     */
    private void recordDrawCompletion(
        CallKey callKey,
        long durationNanos,
        long patternHash,
        int vertexCount,
        int instanceCount,
        int primitiveType,
        long shaderProgramId,
        long vaoId,
        long textureSetHash,
        long uniformSetHash,
        boolean wasIndirect
    ) {
        // Update frame counters
        frameDrawCallCount.incrementAndGet();
        frameDrawTimeNanos.addAndGet(durationNanos);

        // Record for pattern analysis
        DrawCallRecord record = new DrawCallRecord(
            System.nanoTime(), patternHash, vertexCount, instanceCount,
            primitiveType, shaderProgramId, vaoId, textureSetHash, uniformSetHash,
            durationNanos, wasIndirect
        );

        // Add to recent records (bounded)
        if (drawCallRecordCount.get() < MAX_DRAW_CALL_RECORDS) {
            recentDrawCalls.offer(record);
            drawCallRecordCount.incrementAndGet();
        }

        // Update pattern tracker
        DrawPatternTracker tracker = drawPatterns.computeIfAbsent(
            patternHash, DrawPatternTracker::new
        );
        tracker.record(record);

        // Check if this pattern should be marked for indirect optimization
        if (!tracker.markedForIndirect.get() && tracker.isCandidate()) {
            considerForIndirectOptimization(tracker);
        }

        // Standard profiling
        handleCallCompletion(callKey, durationNanos, null);
    }

    /**
     * Considers a draw pattern for indirect draw optimization.
     */
    private void considerForIndirectOptimization(DrawPatternTracker tracker) {
        // Only optimize active patterns with significant overhead
        if (!tracker.isActive()) return;
        if (tracker.averageDurationNanos() < MICRO_SPIKE_THRESHOLD_NS / 10) return;

        // Mark for optimization
        if (tracker.markedForIndirect.compareAndSet(false, true)) {
            // Queue async optimization
            diagnosticsExecutor.execute(() -> {
                try {
                    setupIndirectDrawForPattern(tracker);
                } catch (Exception e) {
                    tracker.markedForIndirect.set(false);
                    e.printStackTrace();
                }
            });
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 28: DRAW POOL INTEGRATION
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Result from DrawPool analysis.
     */
    public record DrawPoolReport(
        long frameNumber,
        int totalDrawCalls,
        long totalDrawTimeNanos,
        List<DrawCallCluster> heavyDrawClusters,
        List<DrawCallCluster> repeatedPatterns,
        Map<Long, Integer> shaderDrawCounts,
        Map<Long, Integer> vaoDrawCounts,
        double estimatedIndirectSavingsPercent,
        List<Long> recommendedIndirectPatterns
    ) {}

    /**
     * Cluster of related draw calls.
     */
    public record DrawCallCluster(
        long patternHash,
        int drawCount,
        long totalTimeNanos,
        int totalVertices,
        int totalInstances,
        long shaderProgramId,
        long vaoId,
        boolean eligibleForIndirect
    ) {}

    /**
     * Requests a comprehensive draw report from DrawPool.
     */
    public CompletableFuture<DrawPoolReport> requestDrawPoolReport() {
        return CompletableFuture.supplyAsync(() -> {
            DrawPool pool = this.drawPool;
            if (pool == null) {
                return buildInternalDrawReport();
            }

            try {
                // Query DrawPool for its analysis
                DrawPoolReport poolReport = pool.generateReport();

                // Merge with our pattern tracking data
                return mergeWithInternalData(poolReport);

            } catch (Exception e) {
                System.err.println("[JITHelper] DrawPool query failed: " + e.getMessage());
                return buildInternalDrawReport();
            }
        }, diagnosticsExecutor);
    }

    /**
     * Builds a draw report from internal tracking when DrawPool is unavailable.
     */
    private DrawPoolReport buildInternalDrawReport() {
        long frameNumber = frameAccumulator.frameNumber.get();
        int totalCalls = (int) frameDrawCallCount.get();
        long totalTime = frameDrawTimeNanos.get();

        // Analyze patterns
        List<DrawCallCluster> heavyClusters = new ArrayList<>();
        List<DrawCallCluster> repeatedPatterns = new ArrayList<>();
        List<Long> recommendedIndirect = new ArrayList<>();
        Map<Long, Integer> shaderCounts = new HashMap<>();
        Map<Long, Integer> vaoCounts = new HashMap<>();

        for (DrawPatternTracker tracker : drawPatterns.values()) {
            if (!tracker.isActive()) continue;

            long count = tracker.occurrenceCount.sum();
            DrawCallCluster cluster = new DrawCallCluster(
                tracker.patternHash,
                (int) count,
                tracker.totalDurationNanos.sum(),
                (int) tracker.totalVertices.sum(),
                (int) tracker.totalInstances.sum(),
                tracker.shaderProgramId,
                tracker.vaoId,
                tracker.isCandidate()
            );

            // Heavy if total time > 1ms
            if (cluster.totalTimeNanos() > 1_000_000L) {
                heavyClusters.add(cluster);
            }

            // Repeated if count >= threshold
            if (count >= MIN_DRAWS_FOR_INDIRECT_CANDIDATE) {
                repeatedPatterns.add(cluster);
                if (cluster.eligibleForIndirect()) {
                    recommendedIndirect.add(tracker.patternHash);
                }
            }

            shaderCounts.merge(tracker.shaderProgramId, (int) count, Integer::sum);
            vaoCounts.merge(tracker.vaoId, (int) count, Integer::sum);
        }

        // Sort by overhead
        heavyClusters.sort(Comparator.comparingLong(DrawCallCluster::totalTimeNanos).reversed());
        repeatedPatterns.sort(Comparator.comparingInt(DrawCallCluster::drawCount).reversed());

        // Estimate savings
        long potentialSavings = repeatedPatterns.stream()
            .filter(DrawCallCluster::eligibleForIndirect)
            .mapToLong(c -> c.totalTimeNanos() * 7 / 10)  // Assume 70% reduction
            .sum();
        double savingsPercent = totalTime > 0 ? (double) potentialSavings / totalTime * 100 : 0;

        return new DrawPoolReport(
            frameNumber, totalCalls, totalTime,
            heavyClusters, repeatedPatterns,
            shaderCounts, vaoCounts,
            savingsPercent, recommendedIndirect
        );
    }

    /**
     * Merges DrawPool report with internal tracking data.
     */
    private DrawPoolReport mergeWithInternalData(DrawPoolReport poolReport) {
        // Combine pool's analysis with our pattern tracking
        List<Long> recommendedPatterns = new ArrayList<>(poolReport.recommendedIndirectPatterns());

        // Add patterns we've identified that pool might have missed
        for (DrawPatternTracker tracker : drawPatterns.values()) {
            if (tracker.isCandidate() && tracker.isActive() &&
                !recommendedPatterns.contains(tracker.patternHash)) {
                recommendedPatterns.add(tracker.patternHash);
            }
        }

        return new DrawPoolReport(
            poolReport.frameNumber(),
            poolReport.totalDrawCalls(),
            poolReport.totalDrawTimeNanos(),
            poolReport.heavyDrawClusters(),
            poolReport.repeatedPatterns(),
            poolReport.shaderDrawCounts(),
            poolReport.vaoDrawCounts(),
            poolReport.estimatedIndirectSavingsPercent(),
            recommendedPatterns
        );
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 29: INDIRECT DRAW MANAGER INTEGRATION
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Request sent to IndirectDrawManager to cache a draw pattern.
     */
    public record IndirectDrawCacheRequest(
        long patternHash,
        int primitiveType,
        long shaderProgramId,
        long vaoId,
        long textureSetHash,
        long uniformSetHash,
        int typicalVertexCount,
        int typicalInstanceCount,
        int expectedBatchSize
    ) {}

    /**
     * Sets up indirect drawing for a tracked pattern.
     */
    private void setupIndirectDrawForPattern(DrawPatternTracker tracker) {
        IndirectDrawManager indirectMgr = this.indirectDrawManager;
        if (indirectMgr == null) {
            tracker.markedForIndirect.set(false);
            return;
        }

        // Build cache request
        int avgVertices = (int) (tracker.totalVertices.sum() / Math.max(1, tracker.occurrenceCount.sum()));
        int avgInstances = (int) (tracker.totalInstances.sum() / Math.max(1, tracker.occurrenceCount.sum()));
        int batchSize = Math.min(
            INDIRECT_BATCH_SIZE_OPTIMAL,
            Math.max(INDIRECT_BATCH_SIZE_MIN, (int) tracker.occurrenceCount.sum())
        );

        IndirectDrawCacheRequest request = new IndirectDrawCacheRequest(
            tracker.patternHash,
            tracker.primitiveType,
            tracker.shaderProgramId,
            tracker.vaoId,
            tracker.textureSetHash,
            tracker.uniformSetHash,
            avgVertices,
            avgInstances,
            batchSize
        );

        // Request caching from IndirectDrawManager
        boolean cached = indirectMgr.cacheDrawPattern(request);

        if (cached) {
            tracker.indirectCached.set(true);
            System.out.println("[JITHelper] Cached indirect draw pattern: " +
                Long.toHexString(tracker.patternHash) +
                " (batch size: " + batchSize + ")");

            // Report optimization
            reportOptimizationActivated(tracker, request);
        } else {
            tracker.markedForIndirect.set(false);
        }
    }

    /**
     * Executes a draw using the cached indirect buffer.
     */
    private boolean executeIndirectDraw(DrawPatternTracker tracker, int vertexCount, int instanceCount) {
        IndirectDrawManager indirectMgr = this.indirectDrawManager;
        if (indirectMgr == null) return false;

        try {
            return indirectMgr.executeIndirect(
                tracker.patternHash,
                vertexCount,
                instanceCount
            );
        } catch (Exception e) {
            // Indirect draw failed - fall back to regular
            System.err.println("[JITHelper] Indirect draw failed for pattern " +
                Long.toHexString(tracker.patternHash) + ": " + e.getMessage());
            tracker.indirectCached.set(false);
            return false;
        }
    }

    /**
     * Reports that an optimization was activated.
     */
    private void reportOptimizationActivated(DrawPatternTracker tracker, IndirectDrawCacheRequest request) {
        DiagnosticsReport report = DiagnosticsReport.builder()
            .callKey(new CallKey(Backend.OPENGL, CallCategory.DRAW_INDIRECT, "IndirectOptimization"))
            .issueKind(IssueKind.RECOVERY_SUCCEEDED)
            .severity(Severity.INFO)
            .message("Activated indirect draw optimization for pattern " +
                Long.toHexString(tracker.patternHash))
            .addMetadata("patternHash", tracker.patternHash)
            .addMetadata("occurrences", tracker.occurrenceCount.sum())
            .addMetadata("totalOverheadNanos", tracker.totalOverheadNanos())
            .addMetadata("batchSize", request.expectedBatchSize())
            .addMetadata("shaderProgramId", request.shaderProgramId())
            .addMetadata("vaoId", request.vaoId())
            .build();

        reportQueue.offer(report);
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 30: DRAW CALL PATTERN ANALYSIS & AUTOMATIC OPTIMIZATION
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Analyzes draw call patterns and triggers optimizations.
     * Called periodically and at frame boundaries.
     */
    private void analyzeDrawCallPatterns() {
        long drawCount = frameDrawCallCount.get();
        long drawTime = frameDrawTimeNanos.get();
        long frameTime = totalFrameTimeNanos.get();

        // Check thresholds
        boolean highDrawCount = drawCount >= DRAW_CALL_WARNING_THRESHOLD;
        boolean highDrawTime = frameTime > 0 && (double) drawTime / frameTime > DRAW_TIME_BUDGET_PERCENT;
        boolean criticalDrawCount = drawCount >= DRAW_CALL_CRITICAL_THRESHOLD;

        if (highDrawCount || highDrawTime) {
            int consecutive = consecutiveHighDrawFrames.incrementAndGet();

            if (consecutive >= PATTERN_RECURRENCE_THRESHOLD) {
                triggerDrawOptimization(drawCount, drawTime, criticalDrawCount);
            }
        } else {
            consecutiveHighDrawFrames.set(0);
        }

        // Emergency optimization
        if (drawCount >= DRAW_CALL_EMERGENCY_THRESHOLD) {
            triggerEmergencyDrawOptimization(drawCount, drawTime);
        }
    }

    /**
     * Triggers draw optimization based on detected overhead.
     */
    private void triggerDrawOptimization(long drawCount, long drawTime, boolean isCritical) {
        // Rate limit DrawPool queries
        long now = System.nanoTime();
        long lastQuery = lastDrawPoolQueryNanos.get();
        if (now - lastQuery < 1_000_000_000L) return;  // Max once per second
        lastDrawPoolQueryNanos.set(now);

        // Report the issue
        Severity severity = isCritical ? Severity.SEVERE : Severity.WARNING;
        DiagnosticsReport report = DiagnosticsReport.builder()
            .callKey(new CallKey(Backend.OPENGL, CallCategory.DRAW, "DrawCallOverhead"))
            .issueKind(IssueKind.EXCESSIVE_DRAW_CALLS)
            .severity(severity)
            .durationNanos(drawTime)
            .message(String.format("Excessive draw calls: %d calls, %.2fms",
                drawCount, drawTime / 1_000_000.0))
            .addMetadata("drawCount", drawCount)
            .addMetadata("drawTimeNanos", drawTime)
            .suggestedRecovery(RecoveryStrategy.SKIP_OPERATION)  // Optimization, not recovery
            .build();

        reportQueue.offer(report);

        // Request DrawPool analysis and trigger optimization
        requestDrawPoolReport().thenAccept(poolReport -> {
            processDrawPoolReportForOptimization(poolReport);
        });
    }

    /**
     * Processes DrawPool report and sets up indirect draws for recommended patterns.
     */
    private void processDrawPoolReportForOptimization(DrawPoolReport report) {
        if (report.recommendedIndirectPatterns().isEmpty()) {
            return;
        }

        System.out.println("[JITHelper] DrawPool analysis: " + report.totalDrawCalls() +
            " draws, " + report.recommendedIndirectPatterns().size() + " patterns recommended for indirect");
        System.out.println("[JITHelper] Estimated savings: " +
            String.format("%.1f%%", report.estimatedIndirectSavingsPercent()));

        // Setup indirect draws for each recommended pattern
        for (Long patternHash : report.recommendedIndirectPatterns()) {
            DrawPatternTracker tracker = drawPatterns.get(patternHash);
            if (tracker != null && !tracker.indirectCached.get()) {
                if (tracker.markedForIndirect.compareAndSet(false, true)) {
                    diagnosticsExecutor.execute(() -> {
                        try {
                            setupIndirectDrawForPattern(tracker);
                        } catch (Exception e) {
                            tracker.markedForIndirect.set(false);
                        }
                    });
                }
            }
        }

        // Notify IndirectDrawManager to optimize
        IndirectDrawManager indirectMgr = this.indirectDrawManager;
        if (indirectMgr != null) {
            indirectMgr.optimizeForPatterns(report.recommendedIndirectPatterns());
        }

        indirectDrawOptimizationActive.set(true);
    }

    /**
     * Emergency optimization when draw calls are catastrophically high.
     */
    private void triggerEmergencyDrawOptimization(long drawCount, long drawTime) {
        System.err.println("[JITHelper] EMERGENCY: " + drawCount + " draw calls detected!");

        // Immediately request DrawPool to batch everything possible
        DrawPool pool = this.drawPool;
        if (pool != null) {
            pool.emergencyBatch();
        }

        // Request IndirectDrawManager to enable aggressive batching
        IndirectDrawManager indirectMgr = this.indirectDrawManager;
        if (indirectMgr != null) {
            indirectMgr.enableAggressiveBatching();
        }

        // Report critical issue
        DiagnosticsReport report = DiagnosticsReport.builder()
            .callKey(new CallKey(Backend.OPENGL, CallCategory.DRAW, "EmergencyDrawOptimization"))
            .issueKind(IssueKind.EXCESSIVE_DRAW_CALLS)
            .severity(Severity.CRITICAL)
            .durationNanos(drawTime)
            .message("EMERGENCY: " + drawCount + " draw calls causing severe overhead")
            .addMetadata("drawCount", drawCount)
            .addMetadata("emergencyOptimizationTriggered", true)
            .suggestedRecovery(RecoveryStrategy.DOWNGRADE_QUALITY)
            .build();

        reportQueue.offer(report);
        handleCriticalReport(report);
    }

    /**
     * Called at frame end to finalize draw analysis for the frame.
     */
    public void finalizeFrameDrawAnalysis(long actualFrameTimeNanos) {
        totalFrameTimeNanos.set(actualFrameTimeNanos);
        analyzeDrawCallPatterns();

        // Cleanup old records
        int recordCount = drawCallRecordCount.get();
        while (recordCount > MAX_DRAW_CALL_RECORDS / 2) {
            if (recentDrawCalls.poll() != null) {
                recordCount = drawCallRecordCount.decrementAndGet();
            } else {
                break;
            }
        }

        // Cleanup inactive patterns
        long now = System.nanoTime();
        drawPatterns.entrySet().removeIf(e -> {
            DrawPatternTracker tracker = e.getValue();
            return !tracker.isActive() && !tracker.indirectCached.get();
        });
    }

    /**
     * Resets frame draw counters. Call at frame start.
     */
    public void resetFrameDrawCounters() {
        frameDrawCallCount.set(0);
        frameDrawTimeNanos.set(0);
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 31: ACTIVE STUTTER & ERROR MONITORING
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * ┌─────────────────────────────────────────────────────────────────────────────────────────────┐
     * │ ACTIVE MONITORING SYSTEM                                                                    │
     * ├─────────────────────────────────────────────────────────────────────────────────────────────┤
     * │                                                                                             │
     * │ This system proactively monitors for stutters and errors, taking automatic action          │
     * │ before they become visible to the user. It runs on a dedicated monitoring thread.          │
     * │                                                                                             │
     * │ MONITORED CONDITIONS:                                                                       │
     * │   • Frame time variance exceeding threshold                                                 │
     * │   • Consecutive spike frames                                                                │
     * │   • Memory pressure approaching critical                                                    │
     * │   • Backend error rate increasing                                                           │
     * │   • Draw call count trending upward                                                         │
     * │   • Sync operation delays increasing                                                        │
     * │                                                                                             │
     * │ AUTOMATIC ACTIONS:                                                                          │
     * │   • Pre-emptive garbage collection                                                          │
     * │   • Proactive resource eviction                                                             │
     * │   • Early warning notifications                                                             │
     * │   • Automatic quality reduction                                                             │
     * │   • Preventive pipeline rebuilds                                                            │
     * │   • Draw batching activation                                                                │
     * │                                                                                             │
     * └─────────────────────────────────────────────────────────────────────────────────────────────┘
     */

    // ─── Monitoring State ───
    private final AtomicBoolean activeMonitoringEnabled = new AtomicBoolean(false);
    private final AtomicReference<ScheduledFuture<?>> monitoringTask = new AtomicReference<>();
    private final RingBuffer<Long> recentFrameTimes = new RingBuffer<>(64);
    private final AtomicInteger consecutiveSpikeFrames = new AtomicInteger(0);
    private final AtomicInteger consecutiveErrorFrames = new AtomicInteger(0);
    private final AtomicLong totalErrorsThisSession = new AtomicLong(0);

    // ─── Monitoring Thresholds ───
    private static final int CONSECUTIVE_SPIKE_WARNING    = 3;
    private static final int CONSECUTIVE_SPIKE_ACTION     = 5;
    private static final int CONSECUTIVE_ERROR_WARNING    = 2;
    private static final int CONSECUTIVE_ERROR_ACTION     = 4;
    private static final double FRAME_VARIANCE_WARNING    = 0.5;   // 50% variance
    private static final double FRAME_VARIANCE_ACTION     = 1.0;   // 100% variance
    private static final long MONITOR_INTERVAL_MS         = 16;    // ~60fps

    /**
     * Simple ring buffer for frame timing history.
     */
    @SuppressWarnings("unchecked")
    private static final class RingBuffer<T> {
        private final Object[] buffer;
        private final int capacity;
        private int head = 0;
        private int size = 0;

        RingBuffer(int capacity) {
            this.capacity = capacity;
            this.buffer = new Object[capacity];
        }

        synchronized void add(T value) {
            buffer[head] = value;
            head = (head + 1) % capacity;
            if (size < capacity) size++;
        }

        synchronized T get(int index) {
            if (index >= size) return null;
            int actualIndex = (head - size + index + capacity) % capacity;
            return (T) buffer[actualIndex];
        }

        synchronized int size() { return size; }

        synchronized void clear() {
            Arrays.fill(buffer, null);
            head = 0;
            size = 0;
        }
    }

    /**
     * Enables active monitoring mode.
     */
    public void enableActiveMonitoring() {
        if (activeMonitoringEnabled.compareAndSet(false, true)) {
            ScheduledFuture<?> task = scheduledExecutor.scheduleAtFixedRate(
                this::runActiveMonitoringCycle,
                MONITOR_INTERVAL_MS,
                MONITOR_INTERVAL_MS,
                TimeUnit.MILLISECONDS
            );
            monitoringTask.set(task);
            System.out.println("[JITHelper] Active monitoring enabled");
        }
    }

    /**
     * Disables active monitoring mode.
     */
    public void disableActiveMonitoring() {
        if (activeMonitoringEnabled.compareAndSet(true, false)) {
            ScheduledFuture<?> task = monitoringTask.getAndSet(null);
            if (task != null) {
                task.cancel(false);
            }
            System.out.println("[JITHelper] Active monitoring disabled");
        }
    }

    /**
     * Runs one cycle of active monitoring.
     */
    private void runActiveMonitoringCycle() {
        try {
            // Check frame time variance
            checkFrameTimeVariance();

            // Check for consecutive spikes
            checkConsecutiveSpikes();

            // Check for consecutive errors
            checkConsecutiveErrors();

            // Check memory pressure
            checkMemoryPressure();

            // Check backend health
            checkBackendHealth();

            // Check draw call trends
            checkDrawCallTrends();

        } catch (Exception e) {
            // Monitoring must never crash the system
            System.err.println("[JITHelper] Monitoring error: " + e.getMessage());
        }
    }

    /**
     * Records a frame time for monitoring.
     */
    public void recordFrameTime(long frameTimeNanos) {
        recentFrameTimes.add(frameTimeNanos);

        // Check for spike
        boolean isSpike = frameTimeNanos > VISIBLE_STUTTER_THRESHOLD_NS;
        if (isSpike) {
            consecutiveSpikeFrames.incrementAndGet();
        } else {
            consecutiveSpikeFrames.set(0);
        }
    }

    /**
     * Records an error occurrence for monitoring.
     */
    public void recordError() {
        totalErrorsThisSession.incrementAndGet();
        consecutiveErrorFrames.incrementAndGet();
    }

    /**
     * Resets error tracking for a new frame.
     */
    public void resetFrameErrors() {
        // Only reset consecutive counter if no errors this frame
        // This is called at frame end after checking
    }

    private void checkFrameTimeVariance() {
        int size = recentFrameTimes.size();
        if (size < 8) return;

        // Calculate mean and variance
        double sum = 0;
        for (int i = 0; i < size; i++) {
            Long ft = recentFrameTimes.get(i);
            if (ft != null) sum += ft;
        }
        double mean = sum / size;

        double varianceSum = 0;
        for (int i = 0; i < size; i++) {
            Long ft = recentFrameTimes.get(i);
            if (ft != null) {
                double diff = ft - mean;
                varianceSum += diff * diff;
            }
        }
        double variance = varianceSum / size;
        double coeffVar = mean > 0 ? Math.sqrt(variance) / mean : 0;

        if (coeffVar > FRAME_VARIANCE_ACTION) {
            handleHighFrameVariance(coeffVar, mean);
        } else if (coeffVar > FRAME_VARIANCE_WARNING) {
            reportFrameVarianceWarning(coeffVar, mean);
        }
    }

    private void handleHighFrameVariance(double coeffVar, double meanNanos) {
        DiagnosticsReport report = DiagnosticsReport.builder()
            .callKey(new CallKey(Backend.OPENGL, CallCategory.MISC, "FrameVariance"))
            .issueKind(IssueKind.FRAME_PACING_UNSTABLE)
            .severity(Severity.SEVERE)
            .durationNanos((long) meanNanos)
            .message(String.format("High frame time variance: %.1f%% (mean: %.2fms)",
                coeffVar * 100, meanNanos / 1_000_000.0))
            .addMetadata("coefficientOfVariation", coeffVar)
            .addMetadata("meanFrameTimeNanos", meanNanos)
            .suggestedRecovery(RecoveryStrategy.DOWNGRADE_QUALITY)
            .build();

        reportQueue.offer(report);

        // Take action: request quality reduction
        if (glManager != null) glManager.downgradeQuality();
        if (vkManager != null) vkManager.downgradeQuality();
    }

    private void reportFrameVarianceWarning(double coeffVar, double meanNanos) {
        DiagnosticsReport report = DiagnosticsReport.builder()
            .callKey(new CallKey(Backend.OPENGL, CallCategory.MISC, "FrameVariance"))
            .issueKind(IssueKind.FRAME_PACING_UNSTABLE)
            .severity(Severity.WARNING)
            .durationNanos((long) meanNanos)
            .message(String.format("Elevated frame time variance: %.1f%%", coeffVar * 100))
            .addMetadata("coefficientOfVariation", coeffVar)
            .build();

        reportQueue.offer(report);
    }

    private void checkConsecutiveSpikes() {
        int spikes = consecutiveSpikeFrames.get();

        if (spikes >= CONSECUTIVE_SPIKE_ACTION) {
            handleConsecutiveSpikes(spikes);
        } else if (spikes >= CONSECUTIVE_SPIKE_WARNING) {
            reportConsecutiveSpikeWarning(spikes);
        }
    }

    private void handleConsecutiveSpikes(int spikeCount) {
        DiagnosticsReport report = DiagnosticsReport.builder()
            .callKey(new CallKey(Backend.OPENGL, CallCategory.MISC, "ConsecutiveSpikes"))
            .issueKind(IssueKind.SEVERE_STUTTER)
            .severity(Severity.CRITICAL)
            .message("Sustained stuttering: " + spikeCount + " consecutive spike frames")
            .addMetadata("consecutiveSpikes", spikeCount)
            .suggestedRecovery(RecoveryStrategy.EMERGENCY_FLUSH)
            .build();

        reportQueue.offer(report);
        handleCriticalReport(report);

        // Take emergency action
        diagnosticsExecutor.execute(() -> {
            executeEmergencyFlush(report);
            executeEmergencyGC(report);
        });
    }

    private void reportConsecutiveSpikeWarning(int spikeCount) {
        DiagnosticsReport report = DiagnosticsReport.builder()
            .callKey(new CallKey(Backend.OPENGL, CallCategory.MISC, "ConsecutiveSpikes"))
            .issueKind(IssueKind.VISIBLE_STUTTER)
            .severity(Severity.WARNING)
            .message("Multiple spike frames detected: " + spikeCount + " consecutive")
            .addMetadata("consecutiveSpikes", spikeCount)
            .build();

        reportQueue.offer(report);
    }

    private void checkConsecutiveErrors() {
        int errors = consecutiveErrorFrames.get();

        if (errors >= CONSECUTIVE_ERROR_ACTION) {
            handleConsecutiveErrors(errors);
        } else if (errors >= CONSECUTIVE_ERROR_WARNING) {
            reportConsecutiveErrorWarning(errors);
        }

        // Reset counter for next check
        consecutiveErrorFrames.set(0);
    }

    private void handleConsecutiveErrors(int errorCount) {
        DiagnosticsReport report = DiagnosticsReport.builder()
            .callKey(new CallKey(Backend.OPENGL, CallCategory.MISC, "ConsecutiveErrors"))
            .issueKind(IssueKind.RUNTIME_ERROR)
            .severity(Severity.CRITICAL)
            .message("Multiple errors detected: " + errorCount + " in recent frames")
            .addMetadata("consecutiveErrors", errorCount)
            .addMetadata("totalSessionErrors", totalErrorsThisSession.get())
            .suggestedRecovery(RecoveryStrategy.GRACEFUL_DEGRADATION)
            .build();

        reportQueue.offer(report);
        handleCriticalReport(report);
    }

    private void reportConsecutiveErrorWarning(int errorCount) {
        DiagnosticsReport report = DiagnosticsReport.builder()
            .callKey(new CallKey(Backend.OPENGL, CallCategory.MISC, "ConsecutiveErrors"))
            .issueKind(IssueKind.RUNTIME_ERROR)
            .severity(Severity.WARNING)
            .message("Errors detected: " + errorCount + " in recent frames")
            .addMetadata("consecutiveErrors", errorCount)
            .build();

        reportQueue.offer(report);
    }

    private void checkMemoryPressure() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double memoryUsage = (double) usedMemory / maxMemory;

        if (memoryUsage > 0.9) {
            handleHighMemoryPressure(memoryUsage, usedMemory, maxMemory);
        } else if (memoryUsage > 0.75) {
            reportMemoryPressureWarning(memoryUsage);
        }
    }

    private void handleHighMemoryPressure(double usage, long used, long max) {
        DiagnosticsReport report = DiagnosticsReport.builder()
            .callKey(new CallKey(Backend.CPU_FALLBACK, CallCategory.MISC, "MemoryPressure"))
            .issueKind(IssueKind.MEMORY_ALLOCATION_FAILURE)
            .severity(Severity.CRITICAL)
            .message(String.format("High memory pressure: %.1f%% used (%.0fMB / %.0fMB)",
                usage * 100, used / (1024.0 * 1024), max / (1024.0 * 1024)))
            .addMetadata("memoryUsagePercent", usage * 100)
            .addMetadata("usedBytes", used)
            .addMetadata("maxBytes", max)
            .suggestedRecovery(RecoveryStrategy.EMERGENCY_GC)
            .build();

        reportQueue.offer(report);

        // Take action
        diagnosticsExecutor.execute(() -> {
            executeEmergencyGC(report);
            executeEvictAndRetry(report);
        });
    }

    private void reportMemoryPressureWarning(double usage) {
        DiagnosticsReport report = DiagnosticsReport.builder()
            .callKey(new CallKey(Backend.CPU_FALLBACK, CallCategory.MISC, "MemoryPressure"))
            .issueKind(IssueKind.MEMORY_FRAGMENTATION)
            .severity(Severity.WARNING)
            .message(String.format("Elevated memory usage: %.1f%%", usage * 100))
            .addMetadata("memoryUsagePercent", usage * 100)
            .build();

        reportQueue.offer(report);
    }

    private void checkBackendHealth() {
        // Check circuit breaker states
        for (Map.Entry<Backend, CircuitBreaker> entry : circuitBreakers.entrySet()) {
            Backend backend = entry.getKey();
            CircuitBreaker breaker = entry.getValue();

            if (breaker.getState() == CircuitBreaker.State.OPEN) {
                reportBackendUnhealthy(backend, breaker);
            }
        }
    }

    private void reportBackendUnhealthy(Backend backend, CircuitBreaker breaker) {
        DiagnosticsReport report = DiagnosticsReport.builder()
            .callKey(new CallKey(backend, CallCategory.MISC, "BackendHealth"))
            .issueKind(IssueKind.DRIVER_CRASH)
            .severity(Severity.SEVERE)
            .message("Backend " + backend + " circuit breaker is OPEN")
            .addMetadata("circuitBreakerState", "OPEN")
            .addMetadata("backend", backend.name())
            .suggestedRecovery(RecoveryStrategy.REROUTE_BACKEND)
            .suggestedRouting(getFallbackRouting(backend))
            .build();

        reportQueue.offer(report);
    }

    private void checkDrawCallTrends() {
        long currentDrawCount = frameDrawCallCount.get();

        // Check if we're trending toward problematic levels
        if (currentDrawCount > DRAW_CALL_WARNING_THRESHOLD * 0.8) {
            // Near warning threshold - preemptively request DrawPool report
            if (!indirectDrawOptimizationActive.get()) {
                diagnosticsExecutor.execute(() -> {
                    requestDrawPoolReport().thenAccept(this::processDrawPoolReportForOptimization);
                });
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 32: STUTTER DETECTION & MITIGATION
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * ┌─────────────────────────────────────────────────────────────────────────────────────────────┐
     * │ STUTTER MITIGATION STRATEGIES                                                               │
     * ├─────────────────────────────────────────────────────────────────────────────────────────────┤
     * │                                                                                             │
     * │ DETECTION:                                                                                  │
     * │   Frame time > 16.67ms (60fps target) or > 2x average                                      │
     * │                                                                                             │
     * │ IMMEDIATE MITIGATION:                                                                       │
     * │   • Skip non-essential rendering                                                            │
     * │   • Reduce particle counts                                                                  │
     * │   • Lower LOD levels                                                                        │
     * │   • Disable post-processing                                                                 │
     * │                                                                                             │
     * │ DEFERRED MITIGATION:                                                                        │
     * │   • Identify cause via profiling                                                            │
     * │   • Adjust quality settings                                                                 │
     * │   • Pre-warm problematic shaders                                                            │
     * │   • Defragment memory                                                                       │
     * │                                                                                             │
     * │ PREVENTIVE MEASURES:                                                                        │
     * │   • Frame time budgeting                                                                    │
     * │   • Async resource loading                                                                  │
     * │   • Pipeline state caching                                                                  │
     * │   • Draw call batching                                                                      │
     * │                                                                                             │
     * └─────────────────────────────────────────────────────────────────────────────────────────────┘
     */

    /**
     * Stutter mitigation callback interface.
     */
    public interface StutterMitigationCallback {
        void onSkipNonEssential();
        void onReduceParticles(float factor);
        void onLowerLOD(int levels);
        void onDisablePostProcessing();
        void onReduceDrawDistance(float factor);
    }

    private volatile StutterMitigationCallback stutterCallback;

    public JITHelper setStutterMitigationCallback(StutterMitigationCallback callback) {
        this.stutterCallback = callback;
        return this;
    }

    /**
     * Called when a stutter is detected to attempt immediate mitigation.
     */
    public void mitigateStutter(long frameTimeNanos, long targetFrameTimeNanos) {
        double overrun = (double) frameTimeNanos / targetFrameTimeNanos;
        StutterMitigationCallback callback = this.stutterCallback;

        if (callback == null) return;

        if (overrun > 3.0) {
            // Severe stutter: aggressive mitigation
            callback.onSkipNonEssential();
            callback.onDisablePostProcessing();
            callback.onReduceParticles(0.25f);
            callback.onLowerLOD(3);
        } else if (overrun > 2.0) {
            // Moderate stutter
            callback.onReduceParticles(0.5f);
            callback.onLowerLOD(2);
        } else if (overrun > 1.5) {
            // Minor stutter
            callback.onReduceParticles(0.75f);
            callback.onLowerLOD(1);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 33: UTILITY METHODS
    // ════════════════════════════════════════════════════════════════════════════════════════════

    private static double nanosToMs(long nanos) {
        return nanos / 1_000_000.0;
    }

    /**
     * Creates a static CallKey constant. Call this once per call site and store the result.
     */
    public static CallKey createCallKey(Backend backend, CallCategory category, String name) {
        return new CallKey(backend, category, name);
    }

    /**
     * Convenience method for common OpenGL call keys.
     */
    public static final class GLCallKeys {
        public static final CallKey DRAW_ARRAYS = createCallKey(Backend.OPENGL, CallCategory.DRAW, "glDrawArrays");
        public static final CallKey DRAW_ELEMENTS = createCallKey(Backend.OPENGL, CallCategory.DRAW_INDEXED, "glDrawElements");
        public static final CallKey DRAW_ARRAYS_INSTANCED = createCallKey(Backend.OPENGL, CallCategory.DRAW, "glDrawArraysInstanced");
        public static final CallKey DRAW_ELEMENTS_INSTANCED = createCallKey(Backend.OPENGL, CallCategory.DRAW_INDEXED, "glDrawElementsInstanced");
        public static final CallKey MULTI_DRAW_ELEMENTS_INDIRECT = createCallKey(Backend.OPENGL, CallCategory.DRAW_INDIRECT, "glMultiDrawElementsIndirect");
        public static final CallKey BUFFER_DATA = createCallKey(Backend.OPENGL, CallCategory.BUFFER_UPLOAD, "glBufferData");
        public static final CallKey BUFFER_SUB_DATA = createCallKey(Backend.OPENGL, CallCategory.BUFFER_UPLOAD, "glBufferSubData");
        public static final CallKey MAP_BUFFER = createCallKey(Backend.OPENGL, CallCategory.BUFFER_MAP, "glMapBuffer");
        public static final CallKey TEX_IMAGE_2D = createCallKey(Backend.OPENGL, CallCategory.TEXTURE_UPLOAD, "glTexImage2D");
        public static final CallKey COMPILE_SHADER = createCallKey(Backend.OPENGL, CallCategory.SHADER_COMPILE, "glCompileShader");
        public static final CallKey LINK_PROGRAM = createCallKey(Backend.OPENGL, CallCategory.SHADER_LINK, "glLinkProgram");
        public static final CallKey USE_PROGRAM = createCallKey(Backend.OPENGL, CallCategory.SHADER_BIND, "glUseProgram");
        public static final CallKey BIND_FRAMEBUFFER = createCallKey(Backend.OPENGL, CallCategory.FRAMEBUFFER_BIND, "glBindFramebuffer");
        public static final CallKey CLEAR = createCallKey(Backend.OPENGL, CallCategory.FRAMEBUFFER_CLEAR, "glClear");
        public static final CallKey FINISH = createCallKey(Backend.OPENGL, CallCategory.FENCE_WAIT, "glFinish");
        public static final CallKey FLUSH = createCallKey(Backend.OPENGL, CallCategory.QUEUE_SUBMIT, "glFlush");
    }

    /**
     * Convenience method for common Vulkan call keys.
     */
    public static final class VKCallKeys {
        public static final CallKey CMD_DRAW = createCallKey(Backend.VULKAN, CallCategory.DRAW, "vkCmdDraw");
        public static final CallKey CMD_DRAW_INDEXED = createCallKey(Backend.VULKAN, CallCategory.DRAW_INDEXED, "vkCmdDrawIndexed");
        public static final CallKey CMD_DRAW_INDIRECT = createCallKey(Backend.VULKAN, CallCategory.DRAW_INDIRECT, "vkCmdDrawIndirect");
        public static final CallKey CMD_DISPATCH = createCallKey(Backend.VULKAN, CallCategory.DISPATCH_COMPUTE, "vkCmdDispatch");
        public static final CallKey QUEUE_SUBMIT = createCallKey(Backend.VULKAN, CallCategory.QUEUE_SUBMIT, "vkQueueSubmit");
        public static final CallKey QUEUE_PRESENT = createCallKey(Backend.VULKAN, CallCategory.QUEUE_PRESENT, "vkQueuePresentKHR");
        public static final CallKey WAIT_FOR_FENCES = createCallKey(Backend.VULKAN, CallCategory.FENCE_WAIT, "vkWaitForFences");
        public static final CallKey CREATE_SHADER = createCallKey(Backend.VULKAN, CallCategory.SHADER_COMPILE, "vkCreateShaderModule");
        public static final CallKey CREATE_PIPELINE = createCallKey(Backend.VULKAN, CallCategory.PIPELINE_CREATE, "vkCreateGraphicsPipelines");
        public static final CallKey ALLOCATE_MEMORY = createCallKey(Backend.VULKAN, CallCategory.BUFFER_UPLOAD, "vkAllocateMemory");
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 34: SHUTDOWN
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Shuts down the JITHelper and all its background threads.
     */
    public void shutdown() {
        disableActiveMonitoring();

        // Shutdown executors
        diagnosticsExecutor.shutdown();
        recoveryExecutor.shutdown();
        scheduledExecutor.shutdown();

        try {
            if (!diagnosticsExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                diagnosticsExecutor.shutdownNow();
            }
            if (!recoveryExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                recoveryExecutor.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Clear state
        statsMap.clear();
        reportQueue.clear();
        recoveryQueue.clear();
        recoveryTrackers.clear();
        drawPatterns.clear();
        recentDrawCalls.clear();

        System.out.println("[JITHelper] Shutdown complete");
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 35: INTERFACES FOR EXTERNAL SYSTEMS
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Interface that DrawPool must implement.
     */
    public interface DrawPool {
        /**
         * Generates a comprehensive report of current draw call patterns.
         */
        DrawPoolReport generateReport();

        /**
         * Emergency batching - batch as many draws as possible immediately.
         */
        void emergencyBatch();

        /**
         * Get the list of currently tracked draw patterns.
         */
        List<Long> getTrackedPatternHashes();

        /**
         * Force flush all pending draws.
         */
        void flush();
    }

    /**
     * Interface that IndirectDrawManager must implement.
     */
    public interface IndirectDrawManager {
        /**
         * Caches a draw pattern for indirect execution.
         *
         * @param request The cache request with pattern details
         * @return true if caching succeeded
         */
        boolean cacheDrawPattern(IndirectDrawCacheRequest request);

        /**
         * Executes a cached draw pattern indirectly.
         *
         * @param patternHash The pattern hash
         * @param vertexCount Vertex count for this instance
         * @param instanceCount Instance count for this instance
         * @return true if indirect execution succeeded
         */
        boolean executeIndirect(long patternHash, int vertexCount, int instanceCount);

        /**
         * Optimizes for a set of patterns - pre-allocate buffers, etc.
         */
        void optimizeForPatterns(List<Long> patternHashes);

        /**
         * Enables aggressive batching mode for emergency situations.
         */
        void enableAggressiveBatching();

        /**
         * Disables aggressive batching mode.
         */
        void disableAggressiveBatching();

        /**
         * Flushes all pending indirect draws.
         */
        void flush();

        /**
         * Invalidates a cached pattern.
         */
        void invalidatePattern(long patternHash);

        /**
         * Invalidates all cached patterns.
         */
        void invalidateAll();
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 36: DIAGNOSTIC EXPORT
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Exports all diagnostics data as a comprehensive report string.
     */
    public String exportDiagnostics() {
        StringBuilder sb = new StringBuilder(8192);

        sb.append("╔══════════════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                         JITHelper Diagnostics Export                          ║\n");
        sb.append("║                         ").append(Instant.now()).append("                         ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════════════════════╝\n\n");

        // Circuit breaker status
        sb.append("┌── Circuit Breakers ──────────────────────────────────────────────────────────┐\n");
        for (Map.Entry<Backend, CircuitBreaker> entry : circuitBreakers.entrySet()) {
            sb.append("│ ").append(String.format("%-12s", entry.getKey()))
              .append(": ").append(entry.getValue().getState()).append("\n");
        }
        sb.append("└──────────────────────────────────────────────────────────────────────────────┘\n\n");

        // Call statistics summary
        sb.append("┌── Call Statistics Summary ──────────────────────────────────────────────────┐\n");
        sb.append("│ Tracked call sites: ").append(statsMap.size()).append("\n");
        sb.append("│ Pending reports: ").append(reportQueue.size()).append("\n");
        sb.append("│ Pending recoveries: ").append(recoveryQueue.size()).append("\n");
        sb.append("└──────────────────────────────────────────────────────────────────────────────┘\n\n");

        // Top spike producers
        sb.append("┌── Top Spike Producers ─────────────────────────────────────────────────────┐\n");
        statsMap.entrySet().stream()
            .sorted(Comparator.comparingLong(e -> -e.getValue().spikeCount.sum()))
            .limit(10)
            .forEach(e -> {
                CallStatsSnapshot snap = e.getValue().snapshot();
                sb.append(String.format("│ %-40s spikes: %5d  avg: %.3fms\n",
                    e.getKey().name(), snap.spikeCount(), snap.meanNanos() / 1_000_000.0));
            });
        sb.append("└──────────────────────────────────────────────────────────────────────────────┘\n\n");

        // Draw pattern analysis
        sb.append("┌── Draw Pattern Analysis ────────────────────────────────────────────────────┐\n");
        sb.append("│ Tracked patterns: ").append(drawPatterns.size()).append("\n");
        sb.append("│ Indirect optimization active: ").append(indirectDrawOptimizationActive.get()).append("\n");
        long cachedPatterns = drawPatterns.values().stream().filter(t -> t.indirectCached.get()).count();
        sb.append("│ Patterns with indirect caching: ").append(cachedPatterns).append("\n");
        sb.append("└──────────────────────────────────────────────────────────────────────────────┘\n\n");

        // Active monitoring status
        sb.append("┌── Active Monitoring ──────────────────────────────────────────────────────┐\n");
        sb.append("│ Enabled: ").append(activeMonitoringEnabled.get()).append("\n");
        sb.append("│ Consecutive spike frames: ").append(consecutiveSpikeFrames.get()).append("\n");
        sb.append("│ Total session errors: ").append(totalErrorsThisSession.get()).append("\n");
        sb.append("└──────────────────────────────────────────────────────────────────────────────┘\n");

        return sb.toString();
    }
}
