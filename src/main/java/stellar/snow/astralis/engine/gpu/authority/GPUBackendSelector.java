// ═══════════════════════════════════════════════════════════════════════════════════════════════
// GPU BACKEND SELECTOR - COMPLETE OVERHAUL FOR MULTI-BACKEND GPU ABSTRACTION LAYER
// ═══════════════════════════════════════════════════════════════════════════════════════════════
// 
// This is a complete rewrite integrating with the 15-part GPU abstraction layer.
// Supports: OpenGL 4.6, OpenGL ES 3.2, Vulkan 1.4, Metal (FFI), D3D12 (FFI), WebGPU (future)
//
// ═══════════════════════════════════════════════════════════════════════════════════════════════

package stellar.snow.astralis.engine.gpu.authority;

import java.lang.foreign.*;
import java.lang.invoke.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * GPUBackendSelector — Advanced multi-backend GPU selection and lifecycle management.
 * 
 * <p>This selector integrates with the complete GPU abstraction layer supporting 6 graphics APIs.
 * It provides intelligent backend selection based on hardware capabilities, platform constraints,
 * feature requirements, and performance characteristics.</p>
 * 
 * <h2>Supported Backends</h2>
 * <ul>
 *   <li><b>Vulkan 1.4</b> — Maximum performance, full GPU-driven rendering</li>
 *   <li><b>Metal</b> — Native Apple GPU support via Panama FFI</li>
 *   <li><b>DirectX 12</b> — Native Windows GPU support via Panama FFI</li>
 *   <li><b>OpenGL 4.6</b> — Cross-platform compatibility with modern features</li>
 *   <li><b>OpenGL ES 3.2</b> — Mobile and embedded GPU support</li>
 *   <li><b>WebGPU</b> — Future web platform support</li>
 * </ul>
 * 
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Automatic backend detection with capability scoring</li>
 *   <li>Platform-aware selection (Windows→DX12/Vulkan, macOS→Metal, Linux→Vulkan/GL)</li>
 *   <li>Feature requirement validation with graceful degradation</li>
 *   <li>Async initialization with structured concurrency</li>
 *   <li>Hot-reload support for development workflows</li>
 *   <li>Comprehensive diagnostics and profiling</li>
 *   <li>Event-driven state machine with full observability</li>
 * </ul>
 * 
 * @author GPU Abstraction Layer
 * @version 3.0.0
 * @since Java 21
 */
public final class GPUBackendSelector implements AutoCloseable {

    private static final System.Logger LOGGER = System.getLogger(GPUBackendSelector.class.getName());

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // SINGLETON WITH DOUBLE-CHECKED LOCKING
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    private static volatile GPUBackendSelector INSTANCE;
    private static final Object INSTANCE_LOCK = new Object();

    public static GPUBackendSelector instance() {
        GPUBackendSelector inst = INSTANCE;
        if (inst == null) {
            synchronized (INSTANCE_LOCK) {
                inst = INSTANCE;
                if (inst == null) {
                    INSTANCE = inst = new GPUBackendSelector();
                }
            }
        }
        return inst;
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // BACKEND TYPE ENUMERATION
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Supported GPU backend types with platform and capability metadata.
     */
    public enum BackendType {
        /** Vulkan 1.4 — Maximum performance, cross-platform */
        VULKAN_1_4("Vulkan 1.4", "vk", true, true, 
            Set.of(Platform.WINDOWS, Platform.LINUX, Platform.ANDROID)),
        
        /** Metal — Native Apple GPU support */
        METAL("Metal", "mtl", true, true,
            Set.of(Platform.MACOS, Platform.IOS)),
        
        /** DirectX 12 — Native Windows GPU support */
        DIRECTX_12("DirectX 12", "d3d12", true, true,
            Set.of(Platform.WINDOWS)),
        
        /** OpenGL 4.6 — Cross-platform compatibility */
        OPENGL_4_6("OpenGL 4.6", "gl46", false, true,
            Set.of(Platform.WINDOWS, Platform.LINUX, Platform.MACOS)),
        
        /** OpenGL ES 3.2 — Mobile and embedded */
        OPENGL_ES_3_2("OpenGL ES 3.2", "gles32", false, false,
            Set.of(Platform.ANDROID, Platform.IOS, Platform.LINUX, Platform.WEB)),
        
        /** WebGPU — Future web platform */
        WEBGPU("WebGPU", "wgpu", true, false,
            Set.of(Platform.WEB, Platform.WINDOWS, Platform.LINUX, Platform.MACOS)),
        
        /** Software rasterizer — Fallback */
        SOFTWARE("Software", "sw", false, false,
            Set.of(Platform.values())),
        
        /** Null backend — Testing only */
        NULL("Null", "null", false, false,
            Set.of(Platform.values()));

        private final String displayName;
        private final String shortCode;
        private final boolean modernApi;
        private final boolean gpuDrivenCapable;
        private final Set<Platform> supportedPlatforms;

        BackendType(String displayName, String shortCode, boolean modernApi,
                    boolean gpuDrivenCapable, Set<Platform> supportedPlatforms) {
            this.displayName = displayName;
            this.shortCode = shortCode;
            this.modernApi = modernApi;
            this.gpuDrivenCapable = gpuDrivenCapable;
            this.supportedPlatforms = supportedPlatforms;
        }

        public String displayName() { return displayName; }
        public String shortCode() { return shortCode; }
        public boolean isModernApi() { return modernApi; }
        public boolean isGpuDrivenCapable() { return gpuDrivenCapable; }
        public Set<Platform> supportedPlatforms() { return supportedPlatforms; }
        
        public boolean supportsPlatform(Platform platform) {
            return supportedPlatforms.contains(platform);
        }

        /** Get recommended backends for current platform, ordered by preference */
        public static List<BackendType> recommendedForPlatform() {
            Platform current = Platform.current();
            return Arrays.stream(values())
                .filter(t -> t.supportsPlatform(current))
                .filter(t -> t != SOFTWARE && t != NULL)
                .sorted(Comparator.comparingInt(BackendType::platformPriority).reversed())
                .toList();
        }

        private int platformPriority() {
            Platform current = Platform.current();
            return switch (this) {
                case VULKAN_1_4 -> current == Platform.LINUX ? 100 : 90;
                case METAL -> current == Platform.MACOS || current == Platform.IOS ? 100 : 0;
                case DIRECTX_12 -> current == Platform.WINDOWS ? 95 : 0;
                case OPENGL_4_6 -> 70;
                case OPENGL_ES_3_2 -> current == Platform.ANDROID ? 80 : 50;
                case WEBGPU -> current == Platform.WEB ? 100 : 60;
                case SOFTWARE -> 10;
                case NULL -> 0;
            };
        }
    }

    /**
     * Platform enumeration for cross-platform support.
     */
    public enum Platform {
        WINDOWS, LINUX, MACOS, IOS, ANDROID, WEB;

        private static Platform detected;

        public static Platform current() {
            if (detected == null) {
                detected = detect();
            }
            return detected;
        }

        private static Platform detect() {
            String os = System.getProperty("os.name", "").toLowerCase();
            String arch = System.getProperty("os.arch", "").toLowerCase();
            
            if (os.contains("win")) return WINDOWS;
            if (os.contains("mac") || os.contains("darwin")) {
                return arch.contains("aarch64") || arch.contains("arm") ? IOS : MACOS;
            }
            if (os.contains("linux")) {
                // Check for Android
                if (System.getProperty("java.vendor", "").toLowerCase().contains("android") ||
                    System.getProperty("java.vm.name", "").toLowerCase().contains("dalvik")) {
                    return ANDROID;
                }
                return LINUX;
            }
            return LINUX; // Default fallback
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // SELECTION STRATEGY
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Backend selection strategy.
     */
    public enum SelectionStrategy {
        /** Platform-optimal: Select best backend for current platform */
        PLATFORM_OPTIMAL,
        
        /** Highest capability score */
        HIGHEST_CAPABILITY,
        
        /** Best performance characteristics */
        BEST_PERFORMANCE,
        
        /** Lowest memory footprint */
        LOWEST_MEMORY,
        
        /** Lowest power consumption (mobile) */
        LOWEST_POWER,
        
        /** Maximum compatibility */
        MAXIMUM_COMPATIBILITY,
        
        /** First available from fallback chain */
        FIRST_AVAILABLE,
        
        /** Custom scoring function */
        CUSTOM
    }

    /**
     * Feature level requirements for backend selection.
     */
    public enum FeatureLevel {
        /** Basic rendering: draw calls, textures, shaders */
        BASIC(0, "Basic rendering primitives"),
        
        /** Compute shaders */
        COMPUTE(1, "Compute shader support"),
        
        /** Multi-draw indirect */
        INDIRECT_DRAW(2, "Multi-draw indirect commands"),
        
        /** Indirect count (GPU-driven draw count) */
        INDIRECT_COUNT(3, "GPU-driven draw count"),
        
        /** Bindless textures */
        BINDLESS_TEXTURES(4, "Bindless texture access"),
        
        /** Buffer device address */
        BUFFER_DEVICE_ADDRESS(5, "Buffer device address (bindless buffers)"),
        
        /** Mesh shaders */
        MESH_SHADERS(6, "Mesh shader pipeline"),
        
        /** Ray tracing */
        RAY_TRACING(7, "Hardware ray tracing"),
        
        /** Variable rate shading */
        VARIABLE_RATE_SHADING(8, "Variable rate shading"),
        
        /** Full GPU-driven rendering */
        GPU_DRIVEN(9, "Complete GPU-driven rendering pipeline"),
        
        /** Dynamic rendering (renderpass-less) */
        DYNAMIC_RENDERING(10, "Dynamic rendering without render passes"),
        
        /** Timeline semaphores */
        TIMELINE_SEMAPHORES(11, "Timeline semaphore synchronization");

        private final int level;
        private final String description;

        FeatureLevel(int level, String description) {
            this.level = level;
            this.description = description;
        }

        public int level() { return level; }
        public String description() { return description; }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // STATE MACHINE
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Selector state with valid transitions.
     */
    public enum State {
        UNINITIALIZED {
            @Override Set<State> validTransitions() { 
                return Set.of(PROBING, FAILED); 
            }
        },
        PROBING {
            @Override Set<State> validTransitions() { 
                return Set.of(SELECTING, FAILED); 
            }
        },
        SELECTING {
            @Override Set<State> validTransitions() { 
                return Set.of(INITIALIZING, FAILED); 
            }
        },
        INITIALIZING {
            @Override Set<State> validTransitions() { 
                return Set.of(INITIALIZED, FAILED, SELECTING); 
            }
        },
        INITIALIZED {
            @Override Set<State> validTransitions() { 
                return Set.of(HOT_RELOADING, SHUTTING_DOWN); 
            }
        },
        HOT_RELOADING {
            @Override Set<State> validTransitions() { 
                return Set.of(INITIALIZED, FAILED); 
            }
        },
        FAILED {
            @Override Set<State> validTransitions() { 
                return Set.of(PROBING, SHUTTING_DOWN); 
            }
        },
        SHUTTING_DOWN {
            @Override Set<State> validTransitions() { 
                return Set.of(SHUTDOWN); 
            }
        },
        SHUTDOWN {
            @Override Set<State> validTransitions() { 
                return Set.of(PROBING); 
            }
        };

        abstract Set<State> validTransitions();
        
        public boolean canTransitionTo(State target) {
            return validTransitions().contains(target);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // CONFIGURATION RECORD
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Immutable backend selection configuration.
     */
    public record Config(
        BackendType preferred,
        SelectionStrategy strategy,
        List<BackendType> fallbackChain,
        EnumSet<FeatureLevel> requiredFeatures,
        EnumSet<FeatureLevel> desiredFeatures,
        boolean enableValidation,
        boolean enableDebugMarkers,
        boolean enableProfiling,
        boolean enableGpuCapture,
        boolean allowSoftwareFallback,
        boolean asyncInitialization,
        boolean useVirtualThreads,
        Duration probeTimeout,
        Duration initTimeout,
        int maxInitRetries,
        int preferredSwapchainImages,
        boolean preferLowLatency,
        boolean preferLowPower,
        long maxMemoryBudgetMB,
        Map<String, Object> backendOptions
    ) {
        public Config {
            Objects.requireNonNull(strategy, "strategy");
            Objects.requireNonNull(fallbackChain, "fallbackChain");
            Objects.requireNonNull(requiredFeatures, "requiredFeatures");
            Objects.requireNonNull(desiredFeatures, "desiredFeatures");
            Objects.requireNonNull(probeTimeout, "probeTimeout");
            Objects.requireNonNull(initTimeout, "initTimeout");
            Objects.requireNonNull(backendOptions, "backendOptions");
            
            if (maxInitRetries < 1) throw new IllegalArgumentException("maxInitRetries must be >= 1");
            if (preferredSwapchainImages < 2) throw new IllegalArgumentException("preferredSwapchainImages must be >= 2");
        }

        public static Config defaults() {
            return builder().build();
        }

        public static Builder builder() {
            return new Builder();
        }

        /**
         * Configuration builder with fluent API.
         */
        public static final class Builder {
            private BackendType preferred = null;  // null = auto
            private SelectionStrategy strategy = SelectionStrategy.PLATFORM_OPTIMAL;
            private List<BackendType> fallbackChain = new ArrayList<>();
            private EnumSet<FeatureLevel> requiredFeatures = EnumSet.of(FeatureLevel.BASIC);
            private EnumSet<FeatureLevel> desiredFeatures = EnumSet.of(
                FeatureLevel.COMPUTE, FeatureLevel.INDIRECT_DRAW);
            private boolean enableValidation = false;
            private boolean enableDebugMarkers = false;
            private boolean enableProfiling = true;
            private boolean enableGpuCapture = false;
            private boolean allowSoftwareFallback = false;
            private boolean asyncInitialization = false;
            private boolean useVirtualThreads = true;
            private Duration probeTimeout = Duration.ofSeconds(5);
            private Duration initTimeout = Duration.ofSeconds(30);
            private int maxInitRetries = 3;
            private int preferredSwapchainImages = 3;
            private boolean preferLowLatency = true;
            private boolean preferLowPower = false;
            private long maxMemoryBudgetMB = 0;  // 0 = no limit
            private Map<String, Object> backendOptions = new HashMap<>();

            private Builder() {
                // Set platform-specific defaults
                Platform platform = Platform.current();
                fallbackChain.addAll(BackendType.recommendedForPlatform());
            }

            // Core settings
            public Builder preferred(BackendType type) { this.preferred = type; return this; }
            public Builder strategy(SelectionStrategy s) { this.strategy = s; return this; }
            public Builder fallbackChain(BackendType... types) {
                this.fallbackChain = new ArrayList<>(List.of(types));
                return this;
            }

            // Feature requirements
            public Builder require(FeatureLevel... levels) {
                this.requiredFeatures = EnumSet.copyOf(List.of(levels));
                return this;
            }
            public Builder desire(FeatureLevel... levels) {
                this.desiredFeatures = EnumSet.copyOf(List.of(levels));
                return this;
            }

            // Debug settings
            public Builder enableValidation(boolean v) { this.enableValidation = v; return this; }
            public Builder enableDebugMarkers(boolean v) { this.enableDebugMarkers = v; return this; }
            public Builder enableProfiling(boolean v) { this.enableProfiling = v; return this; }
            public Builder enableGpuCapture(boolean v) { this.enableGpuCapture = v; return this; }

            // Initialization settings
            public Builder allowSoftwareFallback(boolean v) { this.allowSoftwareFallback = v; return this; }
            public Builder asyncInitialization(boolean v) { this.asyncInitialization = v; return this; }
            public Builder useVirtualThreads(boolean v) { this.useVirtualThreads = v; return this; }
            public Builder probeTimeout(Duration d) { this.probeTimeout = d; return this; }
            public Builder initTimeout(Duration d) { this.initTimeout = d; return this; }
            public Builder maxInitRetries(int n) { this.maxInitRetries = n; return this; }

            // Performance settings
            public Builder preferredSwapchainImages(int n) { this.preferredSwapchainImages = n; return this; }
            public Builder preferLowLatency(boolean v) { this.preferLowLatency = v; return this; }
            public Builder preferLowPower(boolean v) { this.preferLowPower = v; return this; }
            public Builder maxMemoryBudgetMB(long mb) { this.maxMemoryBudgetMB = mb; return this; }

            // Backend-specific options
            public Builder option(String key, Object value) {
                this.backendOptions.put(key, value);
                return this;
            }
            public Builder options(Map<String, Object> opts) {
                this.backendOptions.putAll(opts);
                return this;
            }

            // ─── Preset Configurations ───

            /** Development configuration with full debugging */
            public Builder forDevelopment() {
                return enableValidation(true)
                    .enableDebugMarkers(true)
                    .enableProfiling(true)
                    .enableGpuCapture(true)
                    .maxInitRetries(1);
            }

            /** Production configuration with minimal overhead */
            public Builder forProduction() {
                return enableValidation(false)
                    .enableDebugMarkers(false)
                    .enableProfiling(false)
                    .enableGpuCapture(false)
                    .maxInitRetries(5);
            }

            /** GPU-driven rendering requirements */
            public Builder forGpuDriven() {
                return require(FeatureLevel.BASIC, FeatureLevel.COMPUTE, 
                               FeatureLevel.INDIRECT_DRAW, FeatureLevel.INDIRECT_COUNT)
                    .desire(FeatureLevel.BINDLESS_TEXTURES, FeatureLevel.BUFFER_DEVICE_ADDRESS,
                            FeatureLevel.MESH_SHADERS)
                    .strategy(SelectionStrategy.HIGHEST_CAPABILITY);
            }

            /** Ray tracing requirements */
            public Builder forRayTracing() {
                return require(FeatureLevel.RAY_TRACING)
                    .desire(FeatureLevel.MESH_SHADERS)
                    .strategy(SelectionStrategy.HIGHEST_CAPABILITY);
            }

            /** Mobile-optimized configuration */
            public Builder forMobile() {
                return preferLowPower(true)
                    .preferLowLatency(false)
                    .preferredSwapchainImages(2)
                    .maxMemoryBudgetMB(512)
                    .fallbackChain(BackendType.OPENGL_ES_3_2, BackendType.VULKAN_1_4);
            }

            /** VR/AR requirements (low latency) */
            public Builder forVR() {
                return preferLowLatency(true)
                    .preferredSwapchainImages(2)
                    .require(FeatureLevel.COMPUTE)
                    .desire(FeatureLevel.VARIABLE_RATE_SHADING);
            }

            /** Prefer Vulkan backend */
            public Builder preferVulkan() {
                return preferred(BackendType.VULKAN_1_4)
                    .fallbackChain(BackendType.VULKAN_1_4, BackendType.OPENGL_4_6);
            }

            /** Prefer Metal backend (macOS/iOS) */
            public Builder preferMetal() {
                return preferred(BackendType.METAL)
                    .fallbackChain(BackendType.METAL, BackendType.OPENGL_4_6);
            }

            /** Prefer DirectX 12 backend (Windows) */
            public Builder preferDirectX12() {
                return preferred(BackendType.DIRECTX_12)
                    .fallbackChain(BackendType.DIRECTX_12, BackendType.VULKAN_1_4, BackendType.OPENGL_4_6);
            }

            /** Prefer OpenGL backend (maximum compatibility) */
            public Builder preferOpenGL() {
                return preferred(BackendType.OPENGL_4_6)
                    .fallbackChain(BackendType.OPENGL_4_6, BackendType.OPENGL_ES_3_2);
            }

            public Config build() {
                // Ensure fallback chain has entries
                if (fallbackChain.isEmpty()) {
                    fallbackChain.addAll(BackendType.recommendedForPlatform());
                }
                
                return new Config(
                    preferred, strategy, List.copyOf(fallbackChain),
                    EnumSet.copyOf(requiredFeatures), EnumSet.copyOf(desiredFeatures),
                    enableValidation, enableDebugMarkers, enableProfiling, enableGpuCapture,
                    allowSoftwareFallback, asyncInitialization, useVirtualThreads,
                    probeTimeout, initTimeout, maxInitRetries, preferredSwapchainImages,
                    preferLowLatency, preferLowPower, maxMemoryBudgetMB,
                    Map.copyOf(backendOptions)
                );
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // CAPABILITY SCORING
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Detailed capability score for backend comparison.
     */
    public record CapabilityScore(
        BackendType type,
        int totalScore,
        int featureScore,
        int performanceScore,
        int stabilityScore,
        int platformScore,
        Map<FeatureLevel, Boolean> featureSupport,
        Map<String, Integer> detailedScores,
        boolean meetsRequirements,
        List<FeatureLevel> missingRequired,
        List<FeatureLevel> missingDesired
    ) implements Comparable<CapabilityScore> {
        
        @Override
        public int compareTo(CapabilityScore other) {
            // Primary: meets requirements
            if (this.meetsRequirements != other.meetsRequirements) {
                return this.meetsRequirements ? 1 : -1;
            }
            // Secondary: total score
            return Integer.compare(this.totalScore, other.totalScore);
        }

        public boolean supports(FeatureLevel level) {
            return featureSupport.getOrDefault(level, false);
        }

        public Set<FeatureLevel> supportedFeatures() {
            return featureSupport.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FeatureLevel.class)));
        }
    }

    /**
     * Backend probe result before full initialization.
     */
    public record ProbeResult(
        BackendType type,
        boolean available,
        String deviceName,
        String driverVersion,
        String apiVersion,
        CapabilityScore score,
        Duration probeTime,
        long dedicatedMemoryMB,
        long sharedMemoryMB,
        String unavailableReason,
        Map<String, Object> deviceProperties
    ) {
        public static ProbeResult unavailable(BackendType type, String reason, Duration time) {
            return new ProbeResult(type, false, null, null, null, null, time, 0, 0, reason, Map.of());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // INITIALIZATION RESULT
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Complete initialization result with diagnostics.
     */
    public record InitResult(
        boolean success,
        GPUBackend backend,
        BackendType type,
        String deviceName,
        String apiVersion,
        Duration totalInitTime,
        Duration probeTime,
        Duration backendInitTime,
        CapabilityScore score,
        List<FeatureLevel> supportedFeatures,
        List<FeatureLevel> missingFeatures,
        List<BackendType> triedBackends,
        List<String> warnings,
        List<String> diagnostics,
        Throwable error
    ) {
        public static InitResult success(
                GPUBackend backend, BackendType type, String deviceName, String apiVersion,
                Duration totalTime, Duration probeTime, Duration initTime, CapabilityScore score,
                List<BackendType> tried, List<String> warnings) {
            return new InitResult(
                true, backend, type, deviceName, apiVersion,
                totalTime, probeTime, initTime, score,
                new ArrayList<>(score.supportedFeatures()),
                score.missingRequired(),
                tried, warnings, List.of(), null
            );
        }

        public static InitResult failure(
                Throwable error, List<BackendType> tried, List<String> warnings,
                List<String> diagnostics, Duration totalTime) {
            return new InitResult(
                false, null, null, null, null,
                totalTime, Duration.ZERO, Duration.ZERO, null,
                List.of(), List.of(), tried, warnings, diagnostics, error
            );
        }

        public boolean hasWarnings() { return !warnings.isEmpty(); }
        public boolean hasDiagnostics() { return !diagnostics.isEmpty(); }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // EVENT SYSTEM (SEALED INTERFACE HIERARCHY)
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Sealed event hierarchy for initialization lifecycle.
     */
    public sealed interface InitEvent {
        Instant timestamp();

        record Starting(Config config, Instant timestamp) implements InitEvent {}
        record ProbingBackend(BackendType type, Instant timestamp) implements InitEvent {}
        record ProbeComplete(ProbeResult result, Instant timestamp) implements InitEvent {}
        record AllProbesComplete(List<ProbeResult> results, Instant timestamp) implements InitEvent {}
        record BackendSelected(BackendType type, CapabilityScore score, String reason, Instant timestamp) implements InitEvent {}
        record InitializingBackend(BackendType type, int attempt, int maxAttempts, Instant timestamp) implements InitEvent {}
        record BackendInitialized(GPUBackend backend, Duration initTime, Instant timestamp) implements InitEvent {}
        record BackendFailed(BackendType type, Throwable error, int attempt, Instant timestamp) implements InitEvent {}
        record FallbackTriggered(BackendType from, BackendType to, String reason, Instant timestamp) implements InitEvent {}
        record Complete(InitResult result, Instant timestamp) implements InitEvent {}
        record HotReloadStarted(BackendType from, BackendType to, Instant timestamp) implements InitEvent {}
        record HotReloadComplete(InitResult result, Instant timestamp) implements InitEvent {}
        record ShutdownStarted(BackendType type, Duration uptime, Instant timestamp) implements InitEvent {}
        record ShutdownComplete(Instant timestamp) implements InitEvent {}
        record StateChanged(State from, State to, Instant timestamp) implements InitEvent {}
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // INTERNAL STATE
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    // Core state (volatile for visibility)
    private volatile Config config = Config.defaults();
    private volatile GPUBackend activeBackend;
    private volatile State state = State.UNINITIALIZED;
    private volatile InitResult lastResult;
    
    // Timing
    private final Instant creationTime = Instant.now();
    private volatile Instant initStartTime;
    private volatile Instant initEndTime;
    
    // Probed backends cache
    private final ConcurrentHashMap<BackendType, ProbeResult> probeCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BackendType, GPUBackend> backendInstances = new ConcurrentHashMap<>();
    
    // Event listeners (thread-safe list)
    private final CopyOnWriteArrayList<Consumer<InitEvent>> eventListeners = new CopyOnWriteArrayList<>();
    
    // Statistics
    private final AtomicInteger totalInitAttempts = new AtomicInteger();
    private final AtomicInteger failedInitAttempts = new AtomicInteger();
    private final AtomicInteger hotReloadCount = new AtomicInteger();
    
    // State lock for transitions
    private final ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();
    
    // Async initialization tracking
    private volatile CompletableFuture<InitResult> asyncInitFuture;
    
    // Custom scoring (optional)
    private volatile Function<GPUCapabilities, Integer> customScoringFunction;
    
    // Backend factories
    private final Map<BackendType, Supplier<GPUBackend>> backendFactories;

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    private GPUBackendSelector() {
        // Initialize backend factories
        this.backendFactories = createBackendFactories();
        
        LOGGER.log(System.Logger.Level.DEBUG, "GPUBackendSelector created on platform: {0}", 
            Platform.current());
    }

    private Map<BackendType, Supplier<GPUBackend>> createBackendFactories() {
        Map<BackendType, Supplier<GPUBackend>> factories = new EnumMap<>(BackendType.class);
        
        // Register available backend factories based on platform
        Platform platform = Platform.current();
        
        if (BackendType.VULKAN_1_4.supportsPlatform(platform)) {
            factories.put(BackendType.VULKAN_1_4, this::createVulkanBackend);
        }
        
        if (BackendType.METAL.supportsPlatform(platform)) {
            factories.put(BackendType.METAL, this::createMetalBackend);
        }
        
        if (BackendType.DIRECTX_12.supportsPlatform(platform)) {
            factories.put(BackendType.DIRECTX_12, this::createD3D12Backend);
        }
        
        if (BackendType.OPENGL_4_6.supportsPlatform(platform)) {
            factories.put(BackendType.OPENGL_4_6, this::createOpenGL46Backend);
        }
        
        if (BackendType.OPENGL_ES_3_2.supportsPlatform(platform)) {
            factories.put(BackendType.OPENGL_ES_3_2, this::createOpenGLES32Backend);
        }
        
        // Always available
        factories.put(BackendType.SOFTWARE, this::createSoftwareBackend);
        factories.put(BackendType.NULL, this::createNullBackend);
        
        return Collections.unmodifiableMap(factories);
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // PUBLIC INITIALIZATION API
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Initialize with default configuration.
     */
    public static GPUBackend initialize() {
        return initialize(Config.defaults());
    }

    /**
     * Initialize with specific backend preference.
     */
    public static GPUBackend initialize(BackendType preferred) {
        return initialize(Config.builder().preferred(preferred).build());
    }

    /**
     * Initialize with full configuration.
     */
    public static GPUBackend initialize(Config config) {
        GPUBackendSelector selector = instance();
        InitResult result = selector.initializeInternal(config);
        
        if (!result.success()) {
            throw new BackendInitializationException(
                "Failed to initialize GPU backend: " + 
                (result.error() != null ? result.error().getMessage() : "Unknown error"),
                result.error()
            );
        }
        
        return result.backend();
    }

    /**
     * Initialize asynchronously with default configuration.
     */
    public static CompletableFuture<GPUBackend> initializeAsync() {
        return initializeAsync(Config.defaults());
    }

    /**
     * Initialize asynchronously with full configuration.
     */
    public static CompletableFuture<GPUBackend> initializeAsync(Config config) {
        GPUBackendSelector selector = instance();
        
        Executor executor = config.useVirtualThreads() 
            ? Executors.newVirtualThreadPerTaskExecutor()
            : ForkJoinPool.commonPool();
        
        CompletableFuture<GPUBackend> future = CompletableFuture
            .supplyAsync(() -> selector.initializeInternal(config), executor)
            .thenApply(result -> {
                if (!result.success()) {
                    throw new CompletionException(new BackendInitializationException(
                        "Async initialization failed", result.error()));
                }
                return result.backend();
            });
        
        // Apply timeout if configured
        if (config.initTimeout().toMillis() > 0) {
            future = future.orTimeout(config.initTimeout().toMillis(), TimeUnit.MILLISECONDS);
        }
        
        return future;
    }

    /**
     * Initialize with structured concurrency (Java 21+).
     */
    public static GPUBackend initializeStructured(Config config) throws Exception {
        GPUBackendSelector selector = instance();
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // Probe all backends in parallel
            List<StructuredTaskScope.Subtask<ProbeResult>> probeTasks = new ArrayList<>();
            
            for (BackendType type : config.fallbackChain()) {
                probeTasks.add(scope.fork(() -> selector.probeBackend(type, config)));
            }
            
            scope.join();
            scope.throwIfFailed();
            
            // Collect results
            List<ProbeResult> probeResults = probeTasks.stream()
                .map(StructuredTaskScope.Subtask::get)
                .filter(ProbeResult::available)
                .toList();
            
            // Select and initialize
            return selector.selectAndInitialize(probeResults, config);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // INTERNAL INITIALIZATION LOGIC
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    private InitResult initializeInternal(Config config) {
        stateLock.writeLock().lock();
        try {
            // Check if already initialized
            if (state == State.INITIALIZED && activeBackend != null) {
                LOGGER.log(System.Logger.Level.WARNING, 
                    "Backend already initialized, returning existing instance");
                return lastResult;
            }
            
            this.config = config;
            this.initStartTime = Instant.now();
            
            transitionState(State.PROBING);
            publishEvent(new InitEvent.Starting(config, Instant.now()));
            
            List<BackendType> triedBackends = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            List<String> diagnostics = new ArrayList<>();
            
            try {
                // Phase 1: Probe available backends
                Instant probeStart = Instant.now();
                List<ProbeResult> probeResults = probeAllBackends(config);
                Duration probeTime = Duration.between(probeStart, Instant.now());
                
                publishEvent(new InitEvent.AllProbesComplete(probeResults, Instant.now()));
                
                // Add diagnostic info
                diagnostics.add("Platform: " + Platform.current());
                diagnostics.add("Probed " + probeResults.size() + " backends in " + probeTime.toMillis() + "ms");
                
                for (ProbeResult probe : probeResults) {
                    if (probe.available()) {
                        diagnostics.add(String.format("  %s: %s (%s) - Score: %d",
                            probe.type().displayName(),
                            probe.deviceName(),
                            probe.apiVersion(),
                            probe.score() != null ? probe.score().totalScore() : 0));
                    } else {
                        diagnostics.add(String.format("  %s: Unavailable - %s",
                            probe.type().displayName(),
                            probe.unavailableReason()));
                    }
                }
                
                // Phase 2: Select backend
                transitionState(State.SELECTING);
                BackendType selectedType = selectBackend(probeResults, config, warnings);
                
                if (selectedType == null) {
                    throw new BackendInitializationException(
                        "No suitable backend found. Required features: " + config.requiredFeatures());
                }
                
                ProbeResult selectedProbe = probeCache.get(selectedType);
                publishEvent(new InitEvent.BackendSelected(
                    selectedType, selectedProbe.score(), 
                    "Selected by " + config.strategy(), Instant.now()));
                
                // Phase 3: Initialize selected backend
                transitionState(State.INITIALIZING);
                InitResult result = initializeBackendWithRetries(
                    selectedType, config, triedBackends, warnings);
                
                if (result.success()) {
                    this.activeBackend = result.backend();
                    this.lastResult = result;
                    this.initEndTime = Instant.now();
                    transitionState(State.INITIALIZED);
                    
                    publishEvent(new InitEvent.BackendInitialized(
                        result.backend(), result.backendInitTime(), Instant.now()));
                    publishEvent(new InitEvent.Complete(result, Instant.now()));
                    
                    logSuccessfulInit(result);
                    return result;
                }
                
                // All attempts failed
                this.initEndTime = Instant.now();
                transitionState(State.FAILED);
                
                InitResult failResult = InitResult.failure(
                    result.error(), triedBackends, warnings, diagnostics,
                    Duration.between(initStartTime, initEndTime));
                this.lastResult = failResult;
                
                publishEvent(new InitEvent.Complete(failResult, Instant.now()));
                return failResult;
                
            } catch (Exception e) {
                this.initEndTime = Instant.now();
                transitionState(State.FAILED);
                
                LOGGER.log(System.Logger.Level.ERROR, "Backend initialization failed", e);
                
                InitResult failResult = InitResult.failure(
                    e, triedBackends, warnings, diagnostics,
                    Duration.between(initStartTime, initEndTime));
                this.lastResult = failResult;
                
                publishEvent(new InitEvent.Complete(failResult, Instant.now()));
                return failResult;
            }
            
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // BACKEND PROBING
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    private List<ProbeResult> probeAllBackends(Config config) {
        List<ProbeResult> results = new ArrayList<>();
        
        // Determine which backends to probe
        Set<BackendType> toProbe = new LinkedHashSet<>();
        if (config.preferred() != null) {
            toProbe.add(config.preferred());
        }
        toProbe.addAll(config.fallbackChain());
        
        // Add platform-recommended backends
        toProbe.addAll(BackendType.recommendedForPlatform());
        
        // Probe in parallel if using virtual threads
        if (config.useVirtualThreads()) {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<ProbeResult>> futures = new ArrayList<>();
                
                for (BackendType type : toProbe) {
                    futures.add(executor.submit(() -> probeBackend(type, config)));
                }
                
                for (Future<ProbeResult> future : futures) {
                    try {
                        ProbeResult result = future.get(config.probeTimeout().toMillis(), TimeUnit.MILLISECONDS);
                        results.add(result);
                        probeCache.put(result.type(), result);
                    } catch (TimeoutException e) {
                        LOGGER.log(System.Logger.Level.WARNING, "Backend probe timed out");
                    } catch (Exception e) {
                        LOGGER.log(System.Logger.Level.DEBUG, "Backend probe failed: {0}", e.getMessage());
                    }
                }
            }
        } else {
            // Sequential probing
            for (BackendType type : toProbe) {
                ProbeResult result = probeBackend(type, config);
                results.add(result);
                probeCache.put(result.type(), result);
            }
        }
        
        return results;
    }

    private ProbeResult probeBackend(BackendType type, Config config) {
        Instant start = Instant.now();
        publishEvent(new InitEvent.ProbingBackend(type, Instant.now()));
        
        try {
            // Check platform support
            if (!type.supportsPlatform(Platform.current())) {
                return ProbeResult.unavailable(type, 
                    "Not supported on " + Platform.current(),
                    Duration.between(start, Instant.now()));
            }
            
            // Check if factory exists
            Supplier<GPUBackend> factory = backendFactories.get(type);
            if (factory == null) {
                return ProbeResult.unavailable(type,
                    "No factory registered",
                    Duration.between(start, Instant.now()));
            }
            
            // Create backend instance
            GPUBackend backend = factory.get();
            if (backend == null) {
                return ProbeResult.unavailable(type,
                    "Factory returned null",
                    Duration.between(start, Instant.now()));
            }
            
            // Initialize backend
            boolean initialized = backend.initialize(
                config.enableValidation(),
                config.enableDebugMarkers()
            );
            
            if (!initialized || !backend.isValid()) {
                return ProbeResult.unavailable(type,
                    "Initialization failed",
                    Duration.between(start, Instant.now()));
            }
            
            // Score capabilities
            GPUCapabilities caps = backend.getCapabilities();
            CapabilityScore score = scoreCapabilities(type, caps, config);
            
            // Cache backend instance
            backendInstances.put(type, backend);
            
            Duration probeTime = Duration.between(start, Instant.now());
            
            ProbeResult result = new ProbeResult(
                type, true,
                caps.deviceName(),
                caps.driverVersion(),
                caps.apiVersion(),
                score, probeTime,
                caps.dedicatedMemoryMB(),
                caps.sharedMemoryMB(),
                null,
                caps.deviceProperties()
            );
            
            publishEvent(new InitEvent.ProbeComplete(result, Instant.now()));
            return result;
            
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.DEBUG, "Probe failed for {0}: {1}", type, e.getMessage());
            
            ProbeResult result = ProbeResult.unavailable(type,
                e.getMessage(),
                Duration.between(start, Instant.now()));
            
            publishEvent(new InitEvent.ProbeComplete(result, Instant.now()));
            return result;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // CAPABILITY SCORING
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    private CapabilityScore scoreCapabilities(BackendType type, GPUCapabilities caps, Config config) {
        Map<FeatureLevel, Boolean> featureSupport = new EnumMap<>(FeatureLevel.class);
        Map<String, Integer> detailedScores = new LinkedHashMap<>();
        
        int featureScore = 0;
        int performanceScore = 0;
        int stabilityScore = 0;
        int platformScore = 0;
        
        // Feature support detection
        featureSupport.put(FeatureLevel.BASIC, true);
        
        featureSupport.put(FeatureLevel.COMPUTE, caps.computeShaders());
        if (caps.computeShaders()) featureScore += 100;
        
        featureSupport.put(FeatureLevel.INDIRECT_DRAW, caps.multiDrawIndirect());
        if (caps.multiDrawIndirect()) featureScore += 80;
        
        featureSupport.put(FeatureLevel.INDIRECT_COUNT, caps.indirectCount());
        if (caps.indirectCount()) featureScore += 100;
        
        featureSupport.put(FeatureLevel.BINDLESS_TEXTURES, caps.bindlessTextures());
        if (caps.bindlessTextures()) featureScore += 60;
        
        featureSupport.put(FeatureLevel.BUFFER_DEVICE_ADDRESS, caps.bufferDeviceAddress());
        if (caps.bufferDeviceAddress()) featureScore += 80;
        
        featureSupport.put(FeatureLevel.MESH_SHADERS, caps.meshShaders());
        if (caps.meshShaders()) featureScore += 150;
        
        featureSupport.put(FeatureLevel.RAY_TRACING, caps.rayTracing());
        if (caps.rayTracing()) featureScore += 200;
        
        featureSupport.put(FeatureLevel.VARIABLE_RATE_SHADING, caps.variableRateShading());
        if (caps.variableRateShading()) featureScore += 50;
        
        featureSupport.put(FeatureLevel.GPU_DRIVEN, caps.supportsGpuDriven());
        if (caps.supportsGpuDriven()) featureScore += 200;
        
        featureSupport.put(FeatureLevel.DYNAMIC_RENDERING, caps.dynamicRendering());
        if (caps.dynamicRendering()) featureScore += 40;
        
        featureSupport.put(FeatureLevel.TIMELINE_SEMAPHORES, caps.timelineSemaphores());
        if (caps.timelineSemaphores()) featureScore += 30;
        
        detailedScores.put("features", featureScore);
        
        // Performance scoring
        if (type.isModernApi()) performanceScore += 100;
        if (caps.persistentMapping()) performanceScore += 50;
        performanceScore += Math.min(caps.maxComputeWorkGroupSize() / 10, 100);
        performanceScore += Math.min(caps.maxDrawIndirectCount() / 1000, 50);
        detailedScores.put("performance", performanceScore);
        
        // Stability scoring (based on driver maturity)
        stabilityScore = switch (type) {
            case OPENGL_4_6 -> 100;  // Very mature
            case VULKAN_1_4 -> 90;   // Mature
            case DIRECTX_12 -> 95;   // Mature on Windows
            case METAL -> 95;        // Mature on Apple
            case OPENGL_ES_3_2 -> 85;
            default -> 50;
        };
        detailedScores.put("stability", stabilityScore);
        
        // Platform-specific scoring
        Platform platform = Platform.current();
        platformScore = type.platformPriority();
        detailedScores.put("platform", platformScore);
        
        // Apply custom scoring if configured
        if (customScoringFunction != null) {
            int customScore = customScoringFunction.apply(caps);
            detailedScores.put("custom", customScore);
            featureScore += customScore;
        }
        
        int totalScore = featureScore + performanceScore + stabilityScore + platformScore;
        
        // Check requirements
        List<FeatureLevel> missingRequired = config.requiredFeatures().stream()
            .filter(f -> !featureSupport.getOrDefault(f, false))
            .toList();
        
        List<FeatureLevel> missingDesired = config.desiredFeatures().stream()
            .filter(f -> !featureSupport.getOrDefault(f, false))
            .toList();
        
        boolean meetsRequirements = missingRequired.isEmpty();
        
        return new CapabilityScore(
            type, totalScore, featureScore, performanceScore, stabilityScore, platformScore,
            featureSupport, detailedScores, meetsRequirements, missingRequired, missingDesired
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // BACKEND SELECTION
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    private BackendType selectBackend(List<ProbeResult> probes, Config config, List<String> warnings) {
        // Filter to available backends
        List<ProbeResult> available = probes.stream()
            .filter(ProbeResult::available)
            .filter(p -> p.score() != null)
            .toList();
        
        if (available.isEmpty()) {
            warnings.add("No backends available");
            return null;
        }
        
        // Filter by requirements
        List<ProbeResult> meetsRequirements = available.stream()
            .filter(p -> p.score().meetsRequirements())
            .toList();
        
        if (meetsRequirements.isEmpty()) {
            if (config.allowSoftwareFallback()) {
                warnings.add("No backend meets requirements, using best available");
                meetsRequirements = available;
            } else {
                warnings.add("No backend meets feature requirements");
                return null;
            }
        }
        
        // Check for preferred backend
        if (config.preferred() != null) {
            Optional<ProbeResult> preferred = meetsRequirements.stream()
                .filter(p -> p.type() == config.preferred())
                .findFirst();
            
            if (preferred.isPresent()) {
                return preferred.get().type();
            }
            
            warnings.add("Preferred backend " + config.preferred() + " not available");
        }
        
        // Apply selection strategy
        return switch (config.strategy()) {
            case PLATFORM_OPTIMAL -> selectPlatformOptimal(meetsRequirements);
            case HIGHEST_CAPABILITY -> selectHighestCapability(meetsRequirements);
            case BEST_PERFORMANCE -> selectBestPerformance(meetsRequirements);
            case LOWEST_MEMORY -> selectLowestMemory(meetsRequirements);
            case LOWEST_POWER -> selectLowestPower(meetsRequirements, config);
            case MAXIMUM_COMPATIBILITY -> selectMaximumCompatibility(meetsRequirements);
            case FIRST_AVAILABLE -> selectFirstAvailable(meetsRequirements, config);
            case CUSTOM -> selectCustom(meetsRequirements);
        };
    }

    private BackendType selectPlatformOptimal(List<ProbeResult> available) {
        return available.stream()
            .max(Comparator.comparingInt(p -> p.type().platformPriority()))
            .map(ProbeResult::type)
            .orElse(null);
    }

    private BackendType selectHighestCapability(List<ProbeResult> available) {
        return available.stream()
            .max(Comparator.comparing(ProbeResult::score))
            .map(ProbeResult::type)
            .orElse(null);
    }

    private BackendType selectBestPerformance(List<ProbeResult> available) {
        return available.stream()
            .max(Comparator.comparingInt(p -> p.score().performanceScore()))
            .map(ProbeResult::type)
            .orElse(null);
    }

    private BackendType selectLowestMemory(List<ProbeResult> available) {
        // Prefer OpenGL for lower memory overhead
        return available.stream()
            .min(Comparator.comparingLong(ProbeResult::dedicatedMemoryMB))
            .map(ProbeResult::type)
            .orElse(null);
    }

    private BackendType selectLowestPower(List<ProbeResult> available, Config config) {
        // Prefer integrated graphics and ES for power efficiency
        return available.stream()
            .filter(p -> p.type() == BackendType.OPENGL_ES_3_2 || 
                        !p.deviceName().toLowerCase().contains("nvidia") &&
                        !p.deviceName().toLowerCase().contains("radeon"))
            .findFirst()
            .or(() -> available.stream().findFirst())
            .map(ProbeResult::type)
            .orElse(null);
    }

    private BackendType selectMaximumCompatibility(List<ProbeResult> available) {
        // Prefer OpenGL for compatibility
        return available.stream()
            .filter(p -> p.type() == BackendType.OPENGL_4_6 || p.type() == BackendType.OPENGL_ES_3_2)
            .findFirst()
            .or(() -> available.stream()
                .max(Comparator.comparingInt(p -> p.score().stabilityScore())))
            .map(ProbeResult::type)
            .orElse(null);
    }

    private BackendType selectFirstAvailable(List<ProbeResult> available, Config config) {
        for (BackendType type : config.fallbackChain()) {
            Optional<ProbeResult> result = available.stream()
                .filter(p -> p.type() == type)
                .findFirst();
            if (result.isPresent()) {
                return result.get().type();
            }
        }
        return available.isEmpty() ? null : available.get(0).type();
    }

    private BackendType selectCustom(List<ProbeResult> available) {
        if (customScoringFunction != null) {
            return available.stream()
                .max(Comparator.comparingInt(p -> {
                    GPUBackend backend = backendInstances.get(p.type());
                    return backend != null ? customScoringFunction.apply(backend.getCapabilities()) : 0;
                }))
                .map(ProbeResult::type)
                .orElse(null);
        }
        return selectHighestCapability(available);
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // BACKEND INITIALIZATION WITH RETRIES
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    private InitResult initializeBackendWithRetries(
            BackendType type, Config config,
            List<BackendType> triedBackends, List<String> warnings) {
        
        triedBackends.add(type);
        Throwable lastError = null;
        
        for (int attempt = 1; attempt <= config.maxInitRetries(); attempt++) {
            totalInitAttempts.incrementAndGet();
            publishEvent(new InitEvent.InitializingBackend(type, attempt, config.maxInitRetries(), Instant.now()));
            
            Instant attemptStart = Instant.now();
            
            try {
                // Get cached or create new backend
                GPUBackend backend = backendInstances.get(type);
                
                if (backend == null || !backend.isValid()) {
                    Supplier<GPUBackend> factory = backendFactories.get(type);
                    if (factory == null) {
                        throw new BackendInitializationException("No factory for " + type);
                    }
                    
                    backend = factory.get();
                    if (backend == null) {
                        throw new BackendInitializationException("Factory returned null for " + type);
                    }
                    
                    boolean initialized = backend.initialize(
                        config.enableValidation(),
                        config.enableDebugMarkers()
                    );
                    
                    if (!initialized) {
                        throw new BackendInitializationException("Backend initialization returned false");
                    }
                }
                
                if (!backend.isValid()) {
                    throw new BackendInitializationException("Backend not valid after initialization");
                }
                
                // Success!
                Duration initTime = Duration.between(attemptStart, Instant.now());
                Duration totalTime = Duration.between(initStartTime, Instant.now());
                ProbeResult probe = probeCache.get(type);
                
                return InitResult.success(
                    backend, type,
                    backend.getCapabilities().deviceName(),
                    backend.getCapabilities().apiVersion(),
                    totalTime,
                    probe != null ? probe.probeTime() : Duration.ZERO,
                    initTime,
                    probe != null ? probe.score() : null,
                    triedBackends, warnings
                );
                
            } catch (Exception e) {
                lastError = e;
                failedInitAttempts.incrementAndGet();
                
                warnings.add(String.format("Attempt %d for %s failed: %s", attempt, type, e.getMessage()));
                publishEvent(new InitEvent.BackendFailed(type, e, attempt, Instant.now()));
                
                LOGGER.log(System.Logger.Level.WARNING, 
                    "Backend {0} initialization attempt {1} failed: {2}", 
                    type, attempt, e.getMessage());
                
                // Wait before retry
                if (attempt < config.maxInitRetries()) {
                    try {
                        Thread.sleep(100L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // All retries failed, try fallback
        return tryFallbackBackends(type, config, triedBackends, warnings, lastError);
    }

    private InitResult tryFallbackBackends(
            BackendType failedType, Config config,
            List<BackendType> triedBackends, List<String> warnings, Throwable lastError) {
        
        for (BackendType fallbackType : config.fallbackChain()) {
            if (triedBackends.contains(fallbackType)) continue;
            
            ProbeResult probe = probeCache.get(fallbackType);
            if (probe == null || !probe.available()) continue;
            if (!probe.score().meetsRequirements() && !config.allowSoftwareFallback()) continue;
            
            publishEvent(new InitEvent.FallbackTriggered(
                failedType, fallbackType, "Previous backend failed", Instant.now()));
            
            LOGGER.log(System.Logger.Level.INFO, "Falling back from {0} to {1}", failedType, fallbackType);
            
            InitResult result = initializeBackendWithRetries(fallbackType, config, triedBackends, warnings);
            if (result.success()) {
                return result;
            }
        }
        
        // No fallback succeeded
        Duration totalTime = Duration.between(initStartTime, Instant.now());
        return InitResult.failure(lastError, triedBackends, warnings, List.of(), totalTime);
    }

    private GPUBackend selectAndInitialize(List<ProbeResult> probeResults, Config config) {
        List<String> warnings = new ArrayList<>();
        BackendType selected = selectBackend(probeResults, config, warnings);
        
        if (selected == null) {
            throw new BackendInitializationException("No suitable backend found");
        }
        
        GPUBackend backend = backendInstances.get(selected);
        if (backend == null || !backend.isValid()) {
            throw new BackendInitializationException("Selected backend not available");
        }
        
        this.activeBackend = backend;
        this.state = State.INITIALIZED;
        
        return backend;
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // BACKEND FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    private GPUBackend createVulkanBackend() {
        // Integrates with VulkanBackend from Part 11/12
        try {
            return new VulkanBackend();
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.DEBUG, "Failed to create Vulkan backend: {0}", e.getMessage());
            return null;
        }
    }

    private GPUBackend createMetalBackend() {
        // Integrates with MetalBackend from Part 14
        if (Platform.current() != Platform.MACOS && Platform.current() != Platform.IOS) {
            return null;
        }
        try {
            return new MetalBackend();
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.DEBUG, "Failed to create Metal backend: {0}", e.getMessage());
            return null;
        }
    }

    private GPUBackend createD3D12Backend() {
        // Integrates with D3D12Backend from Part 15
        if (Platform.current() != Platform.WINDOWS) {
            return null;
        }
        try {
            return new D3D12Backend();
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.DEBUG, "Failed to create D3D12 backend: {0}", e.getMessage());
            return null;
        }
    }

    private GPUBackend createOpenGL46Backend() {
        // Integrates with OpenGLBackend from Part 10
        try {
            return new OpenGLBackend();
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.DEBUG, "Failed to create OpenGL 4.6 backend: {0}", e.getMessage());
            return null;
        }
    }

    private GPUBackend createOpenGLES32Backend() {
        // Integrates with OpenGLESBackend from Part 13
        try {
            return new OpenGLESBackend();
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.DEBUG, "Failed to create OpenGL ES 3.2 backend: {0}", e.getMessage());
            return null;
        }
    }

    private GPUBackend createSoftwareBackend() {
        // Software rasterizer for fallback
        return new NullBackend(); // Placeholder
    }

    private GPUBackend createNullBackend() {
        return new NullBackend();
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // STATE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    private void transitionState(State newState) {
        State oldState = this.state;
        if (!oldState.canTransitionTo(newState)) {
            throw new IllegalStateException("Invalid state transition: " + oldState + " -> " + newState);
        }
        this.state = newState;
        publishEvent(new InitEvent.StateChanged(oldState, newState, Instant.now()));
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // EVENT SYSTEM
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    public void addEventListener(Consumer<InitEvent> listener) {
        eventListeners.add(Objects.requireNonNull(listener));
    }

    public void removeEventListener(Consumer<InitEvent> listener) {
        eventListeners.remove(listener);
    }

    private void publishEvent(InitEvent event) {
        for (Consumer<InitEvent> listener : eventListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                LOGGER.log(System.Logger.Level.WARNING, "Event listener error: {0}", e.getMessage());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // PUBLIC ACCESSORS
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    /** Get active backend (throws if not initialized) */
    public static GPUBackend get() {
        GPUBackendSelector selector = instance();
        if (selector.activeBackend == null) {
            throw new IllegalStateException("GPU backend not initialized. Call initialize() first.");
        }
        return selector.activeBackend;
    }

    /** Get active backend as Optional */
    public static Optional<GPUBackend> getOptional() {
        return Optional.ofNullable(instance().activeBackend);
    }

    /** Check if initialized */
    public static boolean isInitialized() {
        GPUBackendSelector selector = instance();
        return selector.state == State.INITIALIZED && selector.activeBackend != null;
    }

    /** Get active backend type */
    public static Optional<BackendType> getActiveType() {
        GPUBackendSelector selector = instance();
        if (selector.activeBackend == null) return Optional.empty();
        
        // Map GPUBackend.Type to BackendType
        return Optional.ofNullable(switch (selector.activeBackend.getType()) {
            case VULKAN -> BackendType.VULKAN_1_4;
            case METAL -> BackendType.METAL;
            case D3D12 -> BackendType.DIRECTX_12;
            case OPENGL -> BackendType.OPENGL_4_6;
            case OPENGL_ES -> BackendType.OPENGL_ES_3_2;
            default -> null;
        });
    }

    /** Get current state */
    public State getState() { return state; }

    /** Get last initialization result */
    public Optional<InitResult> getLastResult() { return Optional.ofNullable(lastResult); }

    /** Get probe results */
    public Map<BackendType, ProbeResult> getProbeResults() {
        return Collections.unmodifiableMap(probeCache);
    }

    /** Get uptime */
    public Duration getUptime() {
        return initEndTime != null ? Duration.between(initEndTime, Instant.now()) : Duration.ZERO;
    }

    /** Get statistics */
    public SelectorStats getStats() {
        return new SelectorStats(
            state,
            activeBackend != null ? getActiveType().orElse(null) : null,
            totalInitAttempts.get(),
            failedInitAttempts.get(),
            hotReloadCount.get(),
            initStartTime != null && initEndTime != null
                ? Duration.between(initStartTime, initEndTime) : Duration.ZERO,
            getUptime(),
            probeCache.size(),
            backendInstances.size()
        );
    }

    public record SelectorStats(
        State state,
        BackendType activeType,
        int totalInitAttempts,
        int failedInitAttempts,
        int hotReloadCount,
        Duration initTime,
        Duration uptime,
        int probedBackends,
        int availableBackends
    ) {}

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // HOT RELOAD
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    /** Hot reload to different backend (development) */
    public InitResult hotReload(BackendType newType) {
        stateLock.writeLock().lock();
        try {
            if (state != State.INITIALIZED) {
                throw new IllegalStateException("Cannot hot-reload: not initialized");
            }
            
            BackendType oldType = getActiveType().orElse(null);
            LOGGER.log(System.Logger.Level.INFO, "Hot-reloading from {0} to {1}", oldType, newType);
            
            publishEvent(new InitEvent.HotReloadStarted(oldType, newType, Instant.now()));
            
            transitionState(State.HOT_RELOADING);
            hotReloadCount.incrementAndGet();
            
            // Shutdown old backend
            if (activeBackend != null) {
                try {
                    activeBackend.shutdown();
                } catch (Exception e) {
                    LOGGER.log(System.Logger.Level.WARNING, "Error shutting down old backend: {0}", e.getMessage());
                }
            }
            activeBackend = null;
            
            // Clear caches
            probeCache.clear();
            backendInstances.clear();
            
            // Reinitialize
            Config newConfig = Config.builder()
                .preferred(newType)
                .enableValidation(config.enableValidation())
                .enableDebugMarkers(config.enableDebugMarkers())
                .enableProfiling(config.enableProfiling())
                .build();
            
            InitResult result = initializeInternal(newConfig);
            
            publishEvent(new InitEvent.HotReloadComplete(result, Instant.now()));
            
            return result;
            
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // SHUTDOWN
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    public void shutdown() {
        stateLock.writeLock().lock();
        try {
            if (state == State.SHUTDOWN || state == State.SHUTTING_DOWN) return;
            
            BackendType type = getActiveType().orElse(null);
            Duration uptime = getUptime();
            
            publishEvent(new InitEvent.ShutdownStarted(type, uptime, Instant.now()));
            transitionState(State.SHUTTING_DOWN);
            
            LOGGER.log(System.Logger.Level.INFO, "Shutting down GPU backend selector...");
            
            // Shutdown active backend
            if (activeBackend != null) {
                try {
                    activeBackend.shutdown();
                } catch (Exception e) {
                    LOGGER.log(System.Logger.Level.WARNING, "Error during backend shutdown: {0}", e.getMessage());
                }
                activeBackend = null;
            }
            
            // Cleanup all cached backends
            for (GPUBackend backend : backendInstances.values()) {
                try {
                    backend.shutdown();
                } catch (Exception ignored) {}
            }
            backendInstances.clear();
            probeCache.clear();
            
            transitionState(State.SHUTDOWN);
            publishEvent(new InitEvent.ShutdownComplete(Instant.now()));
            
            LOGGER.log(System.Logger.Level.INFO, "GPU backend selector shutdown complete. Uptime: {0}", uptime);
            
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    @Override
    public void close() {
        shutdown();
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // CUSTOM SCORING
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    public void setCustomScoringFunction(Function<GPUCapabilities, Integer> function) {
        this.customScoringFunction = function;
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // CONVENIENCE METHODS
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    /** Check if GPU-driven rendering is supported */
    public static boolean supportsGpuDriven() {
        return isInitialized() && get().getCapabilities().supportsGpuDriven();
    }

    /** Check if compute shaders are supported */
    public static boolean supportsCompute() {
        return isInitialized() && get().getCapabilities().computeShaders();
    }

    /** Check if mesh shaders are supported */
    public static boolean supportsMeshShaders() {
        return isInitialized() && get().getCapabilities().meshShaders();
    }

    /** Check if ray tracing is supported */
    public static boolean supportsRayTracing() {
        return isInitialized() && get().getCapabilities().rayTracing();
    }

    /** Execute action with backend */
    public static void withBackend(Consumer<GPUBackend> action) {
        if (isInitialized()) {
            action.accept(get());
        }
    }

    /** Execute function with backend */
    public static <T> Optional<T> withBackend(Function<GPUBackend, T> function) {
        return isInitialized() ? Optional.ofNullable(function.apply(get())) : Optional.empty();
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // LOGGING
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    private void logSuccessfulInit(InitResult result) {
        LOGGER.log(System.Logger.Level.INFO, "═══════════════════════════════════════════════════════════");
        LOGGER.log(System.Logger.Level.INFO, "GPU Backend Initialized Successfully");
        LOGGER.log(System.Logger.Level.INFO, "═══════════════════════════════════════════════════════════");
        LOGGER.log(System.Logger.Level.INFO, "Backend:     {0}", result.type().displayName());
        LOGGER.log(System.Logger.Level.INFO, "Device:      {0}", result.deviceName());
        LOGGER.log(System.Logger.Level.INFO, "API Version: {0}", result.apiVersion());
        LOGGER.log(System.Logger.Level.INFO, "Init Time:   {0}ms", result.totalInitTime().toMillis());
        LOGGER.log(System.Logger.Level.INFO, "Score:       {0}", result.score() != null ? result.score().totalScore() : "N/A");
        LOGGER.log(System.Logger.Level.INFO, "Features:    {0}", result.supportedFeatures());
        
        if (result.hasWarnings()) {
            for (String warning : result.warnings()) {
                LOGGER.log(System.Logger.Level.WARNING, "Warning: {0}", warning);
            }
        }
        
        LOGGER.log(System.Logger.Level.INFO, "═══════════════════════════════════════════════════════════");
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    // EXCEPTIONS
    // ═══════════════════════════════════════════════════════════════════════════════════════════

    public static class BackendInitializationException extends RuntimeException {
        public BackendInitializationException(String message) {
            super(message);
        }
        public BackendInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════════════════════
// NULL BACKEND IMPLEMENTATION (FOR TESTING)
// ═══════════════════════════════════════════════════════════════════════════════════════════════

/**
 * Null backend implementation for testing and fallback scenarios.
 */
final class NullBackend implements GPUBackend {
    
    private boolean initialized = false;
    
    @Override
    public boolean initialize(boolean enableValidation, boolean enableDebugMarkers) {
        initialized = true;
        return true;
    }
    
    @Override
    public void shutdown() {
        initialized = false;
    }
    
    @Override
    public boolean isValid() { return initialized; }
    
    @Override
    public Type getType() { return Type.NULL; }
    
    @Override
    public String getVersionString() { return "Null 1.0"; }
    
    @Override
    public GPUCapabilities getCapabilities() {
        return new GPUCapabilities(
            "Null Device", "N/A", "1.0", "Null",
            0, 0, Map.of(),
            false, false, false, false, false, false, false, false, false, false, false, false,
            1024, 1024, 1, 1, 1, 1, 0, 0, 0, 16384, 65536
        );
    }
    
    // All other methods return no-ops or default values
    @Override public BufferHandle createBuffer(BufferDesc desc) { return new BufferHandle(0); }
    @Override public void destroyBuffer(BufferHandle handle) {}
    @Override public void uploadBuffer(BufferHandle handle, long offset, MemorySegment data) {}
    @Override public TextureHandle createTexture(TextureDesc desc) { return new TextureHandle(0); }
    @Override public void destroyTexture(TextureHandle handle) {}
    @Override public void uploadTexture(TextureHandle handle, int mipLevel, int arrayLayer, MemorySegment data) {}
    @Override public ShaderHandle createShader(ShaderDesc desc) { return new ShaderHandle(0); }
    @Override public void destroyShader(ShaderHandle handle) {}
    @Override public PipelineHandle createGraphicsPipeline(GraphicsPipelineDesc desc) { return new PipelineHandle(0); }
    @Override public PipelineHandle createComputePipeline(ComputePipelineDesc desc) { return new PipelineHandle(0); }
    @Override public void destroyPipeline(PipelineHandle handle) {}
    @Override public void beginFrame() {}
    @Override public void endFrame() {}
    @Override public void present() {}
    @Override public void waitIdle() {}
    @Override public void cmdBindPipeline(PipelineHandle pipeline) {}
    @Override public void cmdBindVertexBuffer(BufferHandle buffer, int binding, long offset) {}
    @Override public void cmdBindIndexBuffer(BufferHandle buffer, long offset, IndexType type) {}
    @Override public void cmdDraw(int vertexCount, int instanceCount, int firstVertex, int firstInstance) {}
    @Override public void cmdDrawIndexed(int indexCount, int instanceCount, int firstIndex, int vertexOffset, int firstInstance) {}
    @Override public void cmdDispatch(int groupCountX, int groupCountY, int groupCountZ) {}
    @Override public void cmdSetViewport(float x, float y, float width, float height, float minDepth, float maxDepth) {}
    @Override public void cmdSetScissor(int x, int y, int width, int height) {}
    @Override public void cmdPushConstants(int offset, MemorySegment data) {}
    @Override public void cmdBindTexture(int binding, TextureHandle texture) {}
    @Override public void cmdBindBuffer(int binding, BufferHandle buffer, long offset, long size) {}
    @Override public void cmdPipelineBarrier(PipelineStage srcStage, PipelineStage dstStage) {}
}
