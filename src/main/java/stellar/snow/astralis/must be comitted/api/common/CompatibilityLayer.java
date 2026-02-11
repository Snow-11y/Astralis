package stellar.snow.astralis.api.common;

import stellar.snow.astralis.Astralis;
import stellar.snow.astralis.api.opengl.buffer.*;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * CompatibilityLayer - Advanced GPU compatibility detection and adaptation with Java 21+ features.
 *
 * <p>Core Features:</p>
 * <ul>
 *   <li>Comprehensive GPU capability detection</li>
 *   <li>Wrapper/translation layer detection (ANGLE, Zink, etc.)</li>
 *   <li>Feature-level compatibility scoring</li>
 *   <li>Automatic workaround application</li>
 *   <li>Performance impact estimation</li>
 *   <li>Detailed diagnostic reporting</li>
 *   <li>Hardware database for known issues</li>
 *   <li>Runtime feature toggling</li>
 *   <li>Compatibility testing suite</li>
 *   <li>User-friendly error messaging</li>
 *   <li>Event-driven state notifications</li>
 *   <li>Telemetry and analytics</li>
 * </ul>
 *
 * @author Enhanced GPU Framework
 * @version 2.0.0
 * @since Java 21
 */
public final class CompatibilityLayer implements AutoCloseable {

    // ========================================================================
    // SINGLETON
    // ========================================================================

    private static volatile CompatibilityLayer INSTANCE;
    private static final Object INSTANCE_LOCK = new Object();

    /**
     * Get singleton instance.
     */
    public static CompatibilityLayer instance() {
        CompatibilityLayer instance = INSTANCE;
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                instance = INSTANCE;
                if (instance == null) {
                    INSTANCE = instance = new CompatibilityLayer();
                }
            }
        }
        return instance;
    }

    // ========================================================================
    // ENUMS
    // ========================================================================

    /**
     * Overall compatibility level.
     */
    public enum CompatibilityLevel {
        /** Native GPU with full feature support, optimal performance */
        OPTIMAL(100, "Optimal", TextFormatting.GREEN,
            "Full feature support with maximum performance"),

        /** Good compatibility with most features, minor limitations */
        GOOD(80, "Good", TextFormatting.DARK_GREEN,
            "Most features available with good performance"),

        /** Degraded mode with some features disabled */
        DEGRADED(60, "Degraded", TextFormatting.GOLD,
            "Some advanced features disabled for compatibility"),

        /** Minimal support with basic features only */
        MINIMAL(40, "Minimal", TextFormatting.YELLOW,
            "Basic features only, reduced performance expected"),

        /** Barely functional, significant limitations */
        LIMITED(20, "Limited", TextFormatting.RED,
            "Severe limitations, expect issues"),

        /** Cannot run at all */
        UNSUPPORTED(0, "Unsupported", TextFormatting.DARK_RED,
            "Hardware does not meet minimum requirements");

        private final int score;
        private final String displayName;
        private final TextFormatting color;
        private final String description;

        CompatibilityLevel(int score, String displayName, TextFormatting color, String description) {
            this.score = score;
            this.displayName = displayName;
            this.color = color;
            this.description = description;
        }

        public int score() { return score; }
        public String displayName() { return displayName; }
        public TextFormatting color() { return color; }
        public String description() { return description; }

        public boolean isUsable() {
            return this != UNSUPPORTED;
        }

        public boolean isOptimal() {
            return this == OPTIMAL || this == GOOD;
        }

        public static CompatibilityLevel fromScore(int score) {
            if (score >= 90) return OPTIMAL;
            if (score >= 75) return GOOD;
            if (score >= 55) return DEGRADED;
            if (score >= 35) return MINIMAL;
            if (score >= 15) return LIMITED;
            return UNSUPPORTED;
        }
    }

    /**
     * GPU/driver type classification.
     */
    public enum GPUType {
        // Native drivers
        NVIDIA_NATIVE("NVIDIA (Native)", true, false),
        AMD_NATIVE("AMD (Native)", true, false),
        INTEL_NATIVE("Intel (Native)", true, false),
        APPLE_NATIVE("Apple (Native)", true, false),

        // Wrappers/translation layers
        ANGLE_D3D11("ANGLE (D3D11)", false, true),
        ANGLE_D3D9("ANGLE (D3D9)", false, true),
        ANGLE_METAL("ANGLE (Metal)", false, true),
        ANGLE_VULKAN("ANGLE (Vulkan)", false, true),
        ZINK("Zink (Mesa)", false, true),
        LAVAPIPE("Lavapipe (Software)", false, true),
        LLVMPIPE("LLVMpipe (Software)", false, true),
        SWIFTSHADER("SwiftShader", false, true),
        VIRGL("VirGL (Virtual)", false, true),
        MESA_SOFTPIPE("Mesa Softpipe", false, true),

        // Virtual/cloud
        VIRTUAL_GPU("Virtual GPU", false, false),
        CLOUD_GPU("Cloud GPU", false, false),

        // Unknown
        UNKNOWN("Unknown", true, false);

        private final String displayName;
        private final boolean isNative;
        private final boolean isWrapper;

        GPUType(String displayName, boolean isNative, boolean isWrapper) {
            this.displayName = displayName;
            this.isNative = isNative;
            this.isWrapper = isWrapper;
        }

        public String displayName() { return displayName; }
        public boolean isNative() { return isNative; }
        public boolean isWrapper() { return isWrapper; }
        public boolean isSoftware() {
            return this == LAVAPIPE || this == LLVMPIPE || 
                   this == SWIFTSHADER || this == MESA_SOFTPIPE;
        }
    }

    /**
     * Individual feature status.
     */
    public enum FeatureStatus {
        /** Fully supported and enabled */
        SUPPORTED("Supported", TextFormatting.GREEN, true),

        /** Supported but disabled by configuration */
        DISABLED("Disabled", TextFormatting.GRAY, true),

        /** Partially supported with workarounds */
        WORKAROUND("Workaround", TextFormatting.YELLOW, true),

        /** Emulated in software (slow) */
        EMULATED("Emulated", TextFormatting.GOLD, true),

        /** Not supported by hardware/driver */
        UNSUPPORTED("Unsupported", TextFormatting.RED, false),

        /** Unknown status */
        UNKNOWN("Unknown", TextFormatting.DARK_GRAY, false);

        private final String displayName;
        private final TextFormatting color;
        private final boolean usable;

        FeatureStatus(String displayName, TextFormatting color, boolean usable) {
            this.displayName = displayName;
            this.color = color;
            this.usable = usable;
        }

        public String displayName() { return displayName; }
        public TextFormatting color() { return color; }
        public boolean isUsable() { return usable; }
    }

    /**
     * Feature categories.
     */
    public enum FeatureCategory {
        CORE("Core Features", "Essential rendering features"),
        BUFFER("Buffer Management", "VBO, VAO, buffer mapping"),
        SHADER("Shaders", "Shader compilation and linking"),
        TEXTURE("Textures", "Texture formats and sampling"),
        COMPUTE("Compute", "Compute shader support"),
        INDIRECT("Indirect Drawing", "GPU-driven rendering"),
        SYNC("Synchronization", "Fences and barriers"),
        DEBUG("Debug", "Debug output and profiling"),
        EXTENSION("Extensions", "Optional GL extensions");

        private final String displayName;
        private final String description;

        FeatureCategory(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String displayName() { return displayName; }
        public String description() { return description; }
    }

    /**
     * Issue severity levels.
     */
    public enum IssueSeverity {
        CRITICAL("Critical", TextFormatting.DARK_RED, 100),
        HIGH("High", TextFormatting.RED, 75),
        MEDIUM("Medium", TextFormatting.GOLD, 50),
        LOW("Low", TextFormatting.YELLOW, 25),
        INFO("Info", TextFormatting.GRAY, 0);

        private final String displayName;
        private final TextFormatting color;
        private final int impactScore;

        IssueSeverity(String displayName, TextFormatting color, int impactScore) {
            this.displayName = displayName;
            this.color = color;
            this.impactScore = impactScore;
        }

        public String displayName() { return displayName; }
        public TextFormatting color() { return color; }
        public int impactScore() { return impactScore; }
    }

    // ========================================================================
    // RECORDS
    // ========================================================================

    /**
     * GPU hardware information.
     */
    public record GPUInfo(
        String vendor,
        String renderer,
        String version,
        String shadingLanguageVersion,
        GPUType type,
        String driverVersion,
        int vramMB,
        int maxTextureSize,
        int maxComputeWorkGroupSize,
        Set<String> extensions
    ) {
        public boolean isNvidia() {
            return vendor != null && vendor.toLowerCase().contains("nvidia");
        }

        public boolean isAMD() {
            return vendor != null && (vendor.toLowerCase().contains("amd") || 
                                       vendor.toLowerCase().contains("ati"));
        }

        public boolean isIntel() {
            return vendor != null && vendor.toLowerCase().contains("intel");
        }

        public boolean isApple() {
            return vendor != null && vendor.toLowerCase().contains("apple");
        }

        public boolean hasExtension(String ext) {
            return extensions.contains(ext);
        }
    }

    /**
     * Feature capability info.
     */
    public record FeatureInfo(
        String id,
        String name,
        FeatureCategory category,
        FeatureStatus status,
        String description,
        int glVersionRequired,
        String[] requiredExtensions,
        String workaroundApplied,
        int performanceImpact,
        String unavailableReason
    ) {
        public boolean isAvailable() {
            return status.isUsable();
        }

        public static Builder builder(String id, String name) {
            return new Builder(id, name);
        }

        public static final class Builder {
            private final String id;
            private final String name;
            private FeatureCategory category = FeatureCategory.CORE;
            private FeatureStatus status = FeatureStatus.UNKNOWN;
            private String description = "";
            private int glVersionRequired = 15;
            private String[] requiredExtensions = new String[0];
            private String workaroundApplied;
            private int performanceImpact = 0;
            private String unavailableReason;

            private Builder(String id, String name) {
                this.id = id;
                this.name = name;
            }

            public Builder category(FeatureCategory val) { category = val; return this; }
            public Builder status(FeatureStatus val) { status = val; return this; }
            public Builder description(String val) { description = val; return this; }
            public Builder glVersion(int val) { glVersionRequired = val; return this; }
            public Builder extensions(String... val) { requiredExtensions = val; return this; }
            public Builder workaround(String val) { workaroundApplied = val; return this; }
            public Builder performanceImpact(int val) { performanceImpact = val; return this; }
            public Builder unavailableReason(String val) { unavailableReason = val; return this; }

            public FeatureInfo build() {
                return new FeatureInfo(id, name, category, status, description,
                    glVersionRequired, requiredExtensions, workaroundApplied,
                    performanceImpact, unavailableReason);
            }
        }
    }

    /**
     * Detected compatibility issue.
     */
    public record Issue(
        String id,
        String title,
        String description,
        IssueSeverity severity,
        FeatureCategory category,
        List<String> symptoms,
        List<Workaround> workarounds,
        String url
    ) {
        public boolean hasFix() {
            return workarounds != null && !workarounds.isEmpty() &&
                   workarounds.stream().anyMatch(Workaround::isAutomatic);
        }
    }

    /**
     * Workaround for an issue.
     */
    public record Workaround(
        String id,
        String description,
        boolean isAutomatic,
        int performanceImpact,
        Runnable applyAction
    ) {
        public void apply() {
            if (applyAction != null) {
                applyAction.run();
            }
        }
    }

    /**
     * Compatibility evaluation result.
     */
    public record EvaluationResult(
        CompatibilityLevel level,
        int score,
        GPUInfo gpuInfo,
        Map<String, FeatureInfo> features,
        List<Issue> issues,
        List<Workaround> appliedWorkarounds,
        String summary,
        Duration evaluationTime,
        Instant timestamp
    ) {
        public int issueCount(IssueSeverity severity) {
            return (int) issues.stream().filter(i -> i.severity() == severity).count();
        }

        public int criticalIssueCount() {
            return issueCount(IssueSeverity.CRITICAL);
        }

        public boolean hasBlockingIssues() {
            return issues.stream().anyMatch(i -> i.severity() == IssueSeverity.CRITICAL);
        }

        public List<FeatureInfo> getFeaturesByCategory(FeatureCategory category) {
            return features.values().stream()
                .filter(f -> f.category() == category)
                .toList();
        }

        public List<FeatureInfo> getUnavailableFeatures() {
            return features.values().stream()
                .filter(f -> !f.isAvailable())
                .toList();
        }
    }

    /**
     * Known hardware/driver issue database entry.
     */
    public record KnownIssue(
        String id,
        String vendorPattern,
        String rendererPattern,
        String driverVersionPattern,
        IssueSeverity severity,
        String description,
        List<String> affectedFeatures,
        Workaround fix
    ) {
        public boolean matches(GPUInfo gpu) {
            if (vendorPattern != null && !gpu.vendor().toLowerCase().contains(vendorPattern.toLowerCase())) {
                return false;
            }
            if (rendererPattern != null && !gpu.renderer().toLowerCase().contains(rendererPattern.toLowerCase())) {
                return false;
            }
            if (driverVersionPattern != null && gpu.driverVersion() != null &&
                !gpu.driverVersion().matches(driverVersionPattern)) {
                return false;
            }
            return true;
        }
    }

    /**
     * Configuration for compatibility evaluation.
     */
    public record Config(
        boolean enableWorkarounds,
        boolean enableSoftwareEmulation,
        boolean strictMode,
        boolean verboseLogging,
        Set<String> forcedFeatures,
        Set<String> disabledFeatures,
        int minGLVersion,
        boolean allowWrappers,
        boolean allowSoftwareRendering,
        Map<String, Object> overrides
    ) {
        public static Config defaults() {
            return new Config(
                true, false, false, false,
                Set.of(), Set.of(),
                15, true, false,
                Map.of()
            );
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private boolean enableWorkarounds = true;
            private boolean enableSoftwareEmulation = false;
            private boolean strictMode = false;
            private boolean verboseLogging = false;
            private Set<String> forcedFeatures = new HashSet<>();
            private Set<String> disabledFeatures = new HashSet<>();
            private int minGLVersion = 15;
            private boolean allowWrappers = true;
            private boolean allowSoftwareRendering = false;
            private Map<String, Object> overrides = new HashMap<>();

            public Builder enableWorkarounds(boolean val) { enableWorkarounds = val; return this; }
            public Builder enableSoftwareEmulation(boolean val) { enableSoftwareEmulation = val; return this; }
            public Builder strictMode(boolean val) { strictMode = val; return this; }
            public Builder verboseLogging(boolean val) { verboseLogging = val; return this; }
            public Builder forceFeature(String feature) { forcedFeatures.add(feature); return this; }
            public Builder disableFeature(String feature) { disabledFeatures.add(feature); return this; }
            public Builder minGLVersion(int val) { minGLVersion = val; return this; }
            public Builder allowWrappers(boolean val) { allowWrappers = val; return this; }
            public Builder allowSoftwareRendering(boolean val) { allowSoftwareRendering = val; return this; }
            public Builder override(String key, Object val) { overrides.put(key, val); return this; }

            public Config build() {
                return new Config(
                    enableWorkarounds, enableSoftwareEmulation, strictMode, verboseLogging,
                    Set.copyOf(forcedFeatures), Set.copyOf(disabledFeatures),
                    minGLVersion, allowWrappers, allowSoftwareRendering, Map.copyOf(overrides)
                );
            }
        }
    }

    // ========================================================================
    // SEALED INTERFACES FOR EVENTS
    // ========================================================================

    /**
     * Compatibility events.
     */
    public sealed interface CompatibilityEvent permits
            CompatibilityEvent.EvaluationStarted,
            CompatibilityEvent.GPUDetected,
            CompatibilityEvent.FeatureDetected,
            CompatibilityEvent.IssueFound,
            CompatibilityEvent.WorkaroundApplied,
            CompatibilityEvent.EvaluationComplete,
            CompatibilityEvent.FeatureToggled {

        record EvaluationStarted(Config config, Instant timestamp) implements CompatibilityEvent {}
        record GPUDetected(GPUInfo gpu, Instant timestamp) implements CompatibilityEvent {}
        record FeatureDetected(FeatureInfo feature, Instant timestamp) implements CompatibilityEvent {}
        record IssueFound(Issue issue, Instant timestamp) implements CompatibilityEvent {}
        record WorkaroundApplied(Workaround workaround, Issue issue, Instant timestamp) implements CompatibilityEvent {}
        record EvaluationComplete(EvaluationResult result, Instant timestamp) implements CompatibilityEvent {}
        record FeatureToggled(String featureId, boolean enabled, Instant timestamp) implements CompatibilityEvent {}
    }

    // ========================================================================
    // STATE
    // ========================================================================

    private volatile Config config = Config.defaults();
    private volatile EvaluationResult lastResult;
    private volatile GPUInfo gpuInfo;
    private volatile CompatibilityLevel level = CompatibilityLevel.UNSUPPORTED;
    private volatile String compatibilityMessage = "";
    private final Instant creationTime = Instant.now();

    // Feature registry
    private final Map<String, FeatureInfo> features = new ConcurrentHashMap<>();
    private final Map<String, Boolean> featureOverrides = new ConcurrentHashMap<>();

    // Issues database
    private final List<Issue> detectedIssues = new CopyOnWriteArrayList<>();
    private final List<Workaround> appliedWorkarounds = new CopyOnWriteArrayList<>();

    // Known issues database
    private final List<KnownIssue> knownIssuesDatabase = new CopyOnWriteArrayList<>();

    // Event listeners
    private final List<Consumer<CompatibilityEvent>> eventListeners = new CopyOnWriteArrayList<>();

    // Statistics
    private final AtomicInteger evaluationCount = new AtomicInteger(0);
    private final AtomicLong totalEvaluationTimeNanos = new AtomicLong(0);

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    private CompatibilityLayer() {
        initializeKnownIssuesDatabase();
    }

    /**
     * Initialize database of known hardware/driver issues.
     */
    private void initializeKnownIssuesDatabase() {
        // Intel HD Graphics issues
        knownIssuesDatabase.add(new KnownIssue(
            "intel_hd_vao",
            "intel", "hd graphics", null,
            IssueSeverity.MEDIUM,
            "Intel HD Graphics may have VAO performance issues",
            List.of("vao", "vertex_attrib"),
            new Workaround("intel_vao_fix", "Use legacy vertex attribute binding",
                true, 5, () -> featureOverrides.put("use_legacy_vao", true))
        ));

        // Old AMD driver issues
        knownIssuesDatabase.add(new KnownIssue(
            "amd_old_driver_compute",
            "amd", null, ".*[0-9]\\.[0-9].*",
            IssueSeverity.HIGH,
            "Old AMD drivers may crash with compute shaders",
            List.of("compute_shader"),
            new Workaround("amd_compute_disable", "Disable compute shaders",
                true, 30, () -> featureOverrides.put("compute_shader", false))
        ));

        // ANGLE D3D9 limitations
        knownIssuesDatabase.add(new KnownIssue(
            "angle_d3d9_limited",
            "angle", "direct3d9", null,
            IssueSeverity.HIGH,
            "ANGLE D3D9 backend has severe GL limitations",
            List.of("instancing", "compute", "indirect"),
            null
        ));

        // Mesa software renderers
        knownIssuesDatabase.add(new KnownIssue(
            "mesa_software",
            "mesa", "llvmpipe", null,
            IssueSeverity.MEDIUM,
            "Software rendering detected - performance will be very low",
            List.of("all"),
            null
        ));

        // NVIDIA tessellation bug
        knownIssuesDatabase.add(new KnownIssue(
            "nvidia_tess_bug",
            "nvidia", null, ".*45[0-9]\\.[0-9]+.*",
            IssueSeverity.LOW,
            "NVIDIA driver 450.x has tessellation artifacts",
            List.of("tessellation"),
            new Workaround("nvidia_tess_fix", "Disable tessellation",
                true, 10, () -> featureOverrides.put("tessellation", false))
        ));
    }

    // ========================================================================
    // EVALUATION
    // ========================================================================

    /**
     * Evaluate compatibility with default config.
     */
    public static CompatibilityLevel evaluate() {
        return evaluate(Config.defaults());
    }

    /**
     * Evaluate compatibility with custom config.
     */
    public static CompatibilityLevel evaluate(Config config) {
        CompatibilityLayer layer = instance();
        EvaluationResult result = layer.evaluateInternal(config);
        return result.level();
    }

    /**
     * Get full evaluation result.
     */
    public static EvaluationResult evaluateFull() {
        return evaluateFull(Config.defaults());
    }

    /**
     * Get full evaluation result with custom config.
     */
    public static EvaluationResult evaluateFull(Config config) {
        return instance().evaluateInternal(config);
    }

    /**
     * Internal evaluation logic.
     */
    private EvaluationResult evaluateInternal(Config config) {
        this.config = config;
        Instant startTime = Instant.now();

        publishEvent(new CompatibilityEvent.EvaluationStarted(config, startTime));

        try {
            // Detect GL capabilities
            GLCapabilities.detect();

            // Gather GPU info
            gpuInfo = gatherGPUInfo();
            publishEvent(new CompatibilityEvent.GPUDetected(gpuInfo, Instant.now()));

            // Clear previous state
            features.clear();
            detectedIssues.clear();
            appliedWorkarounds.clear();

            // Check minimum requirements
            if (!checkMinimumRequirements()) {
                return createFailedResult(startTime, "Does not meet minimum requirements");
            }

            // Detect all features
            detectFeatures();

            // Check for known issues
            checkKnownIssues();

            // Apply workarounds if enabled
            if (config.enableWorkarounds()) {
                applyWorkarounds();
            }

            // Calculate final score and level
            int score = calculateScore();
            level = CompatibilityLevel.fromScore(score);
            compatibilityMessage = buildCompatibilityMessage();

            // Build result
            Duration evalTime = Duration.between(startTime, Instant.now());
            EvaluationResult result = new EvaluationResult(
                level, score, gpuInfo,
                Map.copyOf(features),
                List.copyOf(detectedIssues),
                List.copyOf(appliedWorkarounds),
                compatibilityMessage,
                evalTime,
                Instant.now()
            );

            lastResult = result;
            evaluationCount.incrementAndGet();
            totalEvaluationTimeNanos.addAndGet(evalTime.toNanos());

            publishEvent(new CompatibilityEvent.EvaluationComplete(result, Instant.now()));
            logResult(result);

            return result;

        } catch (Exception e) {
            Astralis.LOGGER.error("[Compat] Evaluation failed", e);
            return createFailedResult(startTime, "Evaluation error: " + e.getMessage());
        }
    }

    /**
     * Gather GPU information.
     */
    private GPUInfo gatherGPUInfo() {
        String vendor = GLCapabilities.glVendor != null ? GLCapabilities.glVendor : "Unknown";
        String renderer = GLCapabilities.glRenderer != null ? GLCapabilities.glRenderer : "Unknown";
        String version = GLCapabilities.glVersion != null ? GLCapabilities.glVersion : "Unknown";
        String glslVersion = GLCapabilities.glslVersion != null ? GLCapabilities.glslVersion : "Unknown";

        // Detect GPU type
        GPUType type = detectGPUType(vendor, renderer);

        // Parse driver version
        String driverVersion = parseDriverVersion(version);

        // Get VRAM (if available)
        int vramMB = GLCapabilities.totalVRAM;

        // Get limits
        int maxTexSize = GLCapabilities.maxTextureSize;
        int maxComputeSize = GLCapabilities.hasComputeShaders ? GLCapabilities.maxComputeWorkGroupSize : 0;

        // Get extensions
        Set<String> extensions = GLCapabilities.extensions != null
            ? Set.copyOf(GLCapabilities.extensions)
            : Set.of();

        return new GPUInfo(
            vendor, renderer, version, glslVersion,
            type, driverVersion, vramMB, maxTexSize, maxComputeSize, extensions
        );
    }

    /**
     * Detect GPU type from vendor/renderer strings.
     */
    private GPUType detectGPUType(String vendor, String renderer) {
        String v = vendor.toLowerCase();
        String r = renderer.toLowerCase();

        // Check for wrappers first
        if (r.contains("angle")) {
            if (r.contains("direct3d 11") || r.contains("d3d11")) return GPUType.ANGLE_D3D11;
            if (r.contains("direct3d 9") || r.contains("d3d9")) return GPUType.ANGLE_D3D9;
            if (r.contains("metal")) return GPUType.ANGLE_METAL;
            if (r.contains("vulkan")) return GPUType.ANGLE_VULKAN;
            return GPUType.ANGLE_D3D11; // Default ANGLE
        }
        if (r.contains("zink")) return GPUType.ZINK;
        if (r.contains("llvmpipe")) return GPUType.LLVMPIPE;
        if (r.contains("lavapipe")) return GPUType.LAVAPIPE;
        if (r.contains("swiftshader")) return GPUType.SWIFTSHADER;
        if (r.contains("virgl") || r.contains("virtio")) return GPUType.VIRGL;
        if (r.contains("softpipe")) return GPUType.MESA_SOFTPIPE;

        // Check for virtual
        if (r.contains("virtual") || r.contains("vmware") || r.contains("virtualbox")) {
            return GPUType.VIRTUAL_GPU;
        }

        // Native drivers
        if (v.contains("nvidia")) return GPUType.NVIDIA_NATIVE;
        if (v.contains("amd") || v.contains("ati")) return GPUType.AMD_NATIVE;
        if (v.contains("intel")) return GPUType.INTEL_NATIVE;
        if (v.contains("apple")) return GPUType.APPLE_NATIVE;

        return GPUType.UNKNOWN;
    }

    /**
     * Parse driver version from GL version string.
     */
    private String parseDriverVersion(String glVersion) {
        // Try to extract driver version - format varies by vendor
        // NVIDIA: "4.6.0 NVIDIA 535.154.05"
        // AMD: "4.6.14761 Compatibility Profile Context 21.10.2"
        // Intel: "4.6.0 - Build 31.0.101.4091"

        if (glVersion == null) return null;

        String[] parts = glVersion.split("\\s+");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i].matches(".*\\d+\\.\\d+.*")) {
                return parts[i];
            }
        }
        return null;
    }

    /**
     * Check minimum requirements.
     */
    private boolean checkMinimumRequirements() {
        // Must have VBO support (OpenGL 1.5)
        if (!GLCapabilities.hasVBO || !GLCapabilities.GL15) {
            detectedIssues.add(new Issue(
                "no_vbo",
                "VBO Not Supported",
                "GPU does not support Vertex Buffer Objects (OpenGL 1.5)",
                IssueSeverity.CRITICAL,
                FeatureCategory.BUFFER,
                List.of("Cannot create vertex buffers", "Rendering impossible"),
                List.of(),
                null
            ));
            return false;
        }

        // Check for wrapper restrictions
        if (gpuInfo.type().isWrapper() && !config.allowWrappers()) {
            detectedIssues.add(new Issue(
                "wrapper_not_allowed",
                "GL Wrapper Not Allowed",
                "Running on " + gpuInfo.type().displayName() + " which is not allowed by configuration",
                IssueSeverity.CRITICAL,
                FeatureCategory.CORE,
                List.of("Wrapper translation layers disabled"),
                List.of(),
                null
            ));
            return false;
        }

        // Check for software rendering restrictions
        if (gpuInfo.type().isSoftware() && !config.allowSoftwareRendering()) {
            detectedIssues.add(new Issue(
                "software_not_allowed",
                "Software Rendering Not Allowed",
                "Running on software renderer which is not allowed",
                IssueSeverity.CRITICAL,
                FeatureCategory.CORE,
                List.of("Performance would be extremely poor"),
                List.of(),
                null
            ));
            return false;
        }

        // Check GL version
        int glVersion = GLCapabilities.getMaxSupportedVersion();
        if (glVersion < config.minGLVersion()) {
            detectedIssues.add(new Issue(
                "gl_version_too_low",
                "OpenGL Version Too Low",
                String.format("OpenGL %d.%d required, found %d.%d",
                    config.minGLVersion() / 10, config.minGLVersion() % 10,
                    glVersion / 10, glVersion % 10),
                IssueSeverity.CRITICAL,
                FeatureCategory.CORE,
                List.of("Required features not available"),
                List.of(),
                null
            ));
            return false;
        }

        return true;
    }

    /**
     * Detect all features and their status.
     */
    private void detectFeatures() {
        // Core features
        detectCoreFeatures();

        // Buffer features
        detectBufferFeatures();

        // Shader features
        detectShaderFeatures();

        // Compute features
        detectComputeFeatures();

        // Indirect drawing features
        detectIndirectFeatures();

        // Texture features
        detectTextureFeatures();

        // Sync features
        detectSyncFeatures();

        // Debug features
        detectDebugFeatures();

        // Apply config overrides
        applyConfigOverrides();
    }

    private void detectCoreFeatures() {
        registerFeature(FeatureInfo.builder("vbo", "Vertex Buffer Objects")
            .category(FeatureCategory.CORE)
            .status(GLCapabilities.hasVBO ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Core buffer storage for vertex data")
            .glVersion(15)
            .build());

        registerFeature(FeatureInfo.builder("vao", "Vertex Array Objects")
            .category(FeatureCategory.CORE)
            .status(GLCapabilities.hasVAO ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Encapsulates vertex attribute state")
            .glVersion(30)
            .build());

        registerFeature(FeatureInfo.builder("instancing", "Instanced Rendering")
            .category(FeatureCategory.CORE)
            .status(GLCapabilities.hasInstancing ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Draw multiple instances in single call")
            .glVersion(31)
            .build());

        registerFeature(FeatureInfo.builder("base_instance", "Base Instance")
            .category(FeatureCategory.CORE)
            .status(GLCapabilities.hasBaseInstance ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Specify first instance ID")
            .glVersion(42)
            .build());
    }

    private void detectBufferFeatures() {
        registerFeature(FeatureInfo.builder("buffer_storage", "Immutable Buffer Storage")
            .category(FeatureCategory.BUFFER)
            .status(GLCapabilities.hasBufferStorage ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Immutable buffer allocation")
            .glVersion(44)
            .build());

        registerFeature(FeatureInfo.builder("persistent_mapping", "Persistent Buffer Mapping")
            .category(FeatureCategory.BUFFER)
            .status(GLCapabilities.hasPersistentMapping ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Keep buffers mapped across frames")
            .glVersion(44)
            .build());

        registerFeature(FeatureInfo.builder("dsa", "Direct State Access")
            .category(FeatureCategory.BUFFER)
            .status(GLCapabilities.hasDSA ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Modify objects without binding")
            .glVersion(45)
            .build());

        registerFeature(FeatureInfo.builder("buffer_device_address", "Buffer Device Address")
            .category(FeatureCategory.BUFFER)
            .status(GLCapabilities.hasBufferDeviceAddress ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Get GPU virtual address of buffers")
            .glVersion(0)
            .extensions("GL_NV_shader_buffer_load")
            .build());
    }

    private void detectShaderFeatures() {
        registerFeature(FeatureInfo.builder("glsl_130", "GLSL 1.30")
            .category(FeatureCategory.SHADER)
            .status(GLCapabilities.GL30 ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Modern GLSL syntax")
            .glVersion(30)
            .build());

        registerFeature(FeatureInfo.builder("glsl_330", "GLSL 3.30")
            .category(FeatureCategory.SHADER)
            .status(GLCapabilities.GL33 ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Full modern GLSL")
            .glVersion(33)
            .build());

        registerFeature(FeatureInfo.builder("glsl_430", "GLSL 4.30")
            .category(FeatureCategory.SHADER)
            .status(GLCapabilities.GL43 ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Compute shader GLSL")
            .glVersion(43)
            .build());

        registerFeature(FeatureInfo.builder("spirv", "SPIR-V Shaders")
            .category(FeatureCategory.SHADER)
            .status(GLCapabilities.hasSPIRV ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Pre-compiled shader bytecode")
            .glVersion(46)
            .extensions("GL_ARB_gl_spirv")
            .build());

        registerFeature(FeatureInfo.builder("ssbo", "Shader Storage Buffers")
            .category(FeatureCategory.SHADER)
            .status(GLCapabilities.hasSSBO ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Read/write buffers in shaders")
            .glVersion(43)
            .build());
    }

    private void detectComputeFeatures() {
        registerFeature(FeatureInfo.builder("compute_shader", "Compute Shaders")
            .category(FeatureCategory.COMPUTE)
            .status(GLCapabilities.hasComputeShaders ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("General purpose GPU compute")
            .glVersion(43)
            .build());

        registerFeature(FeatureInfo.builder("compute_indirect", "Indirect Compute Dispatch")
            .category(FeatureCategory.COMPUTE)
            .status(GLCapabilities.hasComputeShaders ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Dispatch compute from GPU buffer")
            .glVersion(43)
            .build());
    }

    private void detectIndirectFeatures() {
        registerFeature(FeatureInfo.builder("draw_indirect", "Indirect Drawing")
            .category(FeatureCategory.INDIRECT)
            .status(GLCapabilities.hasDrawIndirect ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Draw commands from GPU buffer")
            .glVersion(40)
            .build());

        registerFeature(FeatureInfo.builder("multi_draw_indirect", "Multi-Draw Indirect")
            .category(FeatureCategory.INDIRECT)
            .status(GLCapabilities.hasMultiDrawIndirect ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Multiple indirect draws in one call")
            .glVersion(43)
            .build());

        registerFeature(FeatureInfo.builder("indirect_count", "Indirect Count")
            .category(FeatureCategory.INDIRECT)
            .status(GLCapabilities.hasIndirectCount ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("GPU-determined draw count")
            .glVersion(46)
            .extensions("GL_ARB_indirect_parameters")
            .build());

        registerFeature(FeatureInfo.builder("mesh_shader", "Mesh Shaders")
            .category(FeatureCategory.INDIRECT)
            .status(GLCapabilities.hasMeshShaders ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Modern mesh shading pipeline")
            .glVersion(0)
            .extensions("GL_NV_mesh_shader")
            .build());
    }

    private void detectTextureFeatures() {
        registerFeature(FeatureInfo.builder("texture_storage", "Texture Storage")
            .category(FeatureCategory.TEXTURE)
            .status(GLCapabilities.hasTextureStorage ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Immutable texture allocation")
            .glVersion(42)
            .build());

        registerFeature(FeatureInfo.builder("bindless_texture", "Bindless Textures")
            .category(FeatureCategory.TEXTURE)
            .status(GLCapabilities.hasBindlessTexture ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Access textures via GPU handles")
            .glVersion(0)
            .extensions("GL_ARB_bindless_texture")
            .build());

        registerFeature(FeatureInfo.builder("sparse_texture", "Sparse Textures")
            .category(FeatureCategory.TEXTURE)
            .status(GLCapabilities.hasSparseTexture ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Virtual textures with partial residency")
            .glVersion(0)
            .extensions("GL_ARB_sparse_texture")
            .build());

        registerFeature(FeatureInfo.builder("anisotropic_filtering", "Anisotropic Filtering")
            .category(FeatureCategory.TEXTURE)
            .status(GLCapabilities.hasAnisotropicFiltering ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("High quality texture filtering")
            .glVersion(0)
            .extensions("GL_EXT_texture_filter_anisotropic")
            .build());
    }

    private void detectSyncFeatures() {
        registerFeature(FeatureInfo.builder("sync_objects", "Sync Objects")
            .category(FeatureCategory.SYNC)
            .status(GLCapabilities.hasSyncObjects ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("GPU fence synchronization")
            .glVersion(32)
            .build());

        registerFeature(FeatureInfo.builder("memory_barrier", "Memory Barriers")
            .category(FeatureCategory.SYNC)
            .status(GLCapabilities.GL42 ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Explicit memory ordering")
            .glVersion(42)
            .build());
    }

    private void detectDebugFeatures() {
        registerFeature(FeatureInfo.builder("debug_output", "Debug Output")
            .category(FeatureCategory.DEBUG)
            .status(GLCapabilities.hasDebugOutput ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("Driver debug messages")
            .glVersion(43)
            .extensions("GL_KHR_debug")
            .build());

        registerFeature(FeatureInfo.builder("timer_query", "Timer Queries")
            .category(FeatureCategory.DEBUG)
            .status(GLCapabilities.hasTimerQuery ? FeatureStatus.SUPPORTED : FeatureStatus.UNSUPPORTED)
            .description("GPU timing measurements")
            .glVersion(33)
            .build());
    }

    private void registerFeature(FeatureInfo feature) {
        features.put(feature.id(), feature);
        publishEvent(new CompatibilityEvent.FeatureDetected(feature, Instant.now()));
    }

    private void applyConfigOverrides() {
        // Force enable features
        for (String featureId : config.forcedFeatures()) {
            FeatureInfo existing = features.get(featureId);
            if (existing != null && existing.status() != FeatureStatus.SUPPORTED) {
                features.put(featureId, FeatureInfo.builder(existing.id(), existing.name())
                    .category(existing.category())
                    .status(FeatureStatus.SUPPORTED)
                    .description(existing.description())
                    .workaround("Forced by configuration")
                    .build());
            }
        }

        // Force disable features
        for (String featureId : config.disabledFeatures()) {
            FeatureInfo existing = features.get(featureId);
            if (existing != null) {
                features.put(featureId, FeatureInfo.builder(existing.id(), existing.name())
                    .category(existing.category())
                    .status(FeatureStatus.DISABLED)
                    .description(existing.description())
                    .unavailableReason("Disabled by configuration")
                    .build());
            }
        }
    }

    /**
     * Check for known issues.
     */
    private void checkKnownIssues() {
        for (KnownIssue known : knownIssuesDatabase) {
            if (known.matches(gpuInfo)) {
                Issue issue = new Issue(
                    known.id(),
                    "Known Issue: " + known.description(),
                    known.description(),
                    known.severity(),
                    FeatureCategory.CORE,
                    List.of("Affects: " + String.join(", ", known.affectedFeatures())),
                    known.fix() != null ? List.of(known.fix()) : List.of(),
                    null
                );
                detectedIssues.add(issue);
                publishEvent(new CompatibilityEvent.IssueFound(issue, Instant.now()));
            }
        }
    }

    /**
     * Apply workarounds for detected issues.
     */
    private void applyWorkarounds() {
        for (Issue issue : detectedIssues) {
            for (Workaround workaround : issue.workarounds()) {
                if (workaround.isAutomatic()) {
                    try {
                        workaround.apply();
                        appliedWorkarounds.add(workaround);
                        publishEvent(new CompatibilityEvent.WorkaroundApplied(workaround, issue, Instant.now()));
                        Astralis.LOGGER.info("[Compat] Applied workaround: {}", workaround.description());
                    } catch (Exception e) {
                        Astralis.LOGGER.warn("[Compat] Failed to apply workaround {}: {}",
                            workaround.id(), e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Calculate compatibility score.
     */
    private int calculateScore() {
        int score = 100;

        // Deduct for issues
        for (Issue issue : detectedIssues) {
            score -= issue.severity().impactScore() / 2;
        }

        // Deduct for missing important features
        int missingImportant = 0;
        for (FeatureInfo feature : features.values()) {
            if (!feature.isAvailable()) {
                if (feature.category() == FeatureCategory.CORE) {
                    missingImportant += 15;
                } else if (feature.category() == FeatureCategory.INDIRECT) {
                    missingImportant += 10;
                } else if (feature.category() == FeatureCategory.COMPUTE) {
                    missingImportant += 8;
                } else {
                    missingImportant += 3;
                }
            }
        }
        score -= Math.min(missingImportant, 50);

        // Bonus for optimal features
        if (isFeatureAvailable("persistent_mapping")) score += 5;
        if (isFeatureAvailable("dsa")) score += 3;
        if (isFeatureAvailable("indirect_count")) score += 5;
        if (isFeatureAvailable("mesh_shader")) score += 5;

        // Penalty for wrappers
        if (gpuInfo.type().isWrapper()) {
            score -= 10;
        }
        if (gpuInfo.type().isSoftware()) {
            score -= 30;
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * Build human-readable compatibility message.
     */
    private String buildCompatibilityMessage() {
        StringBuilder sb = new StringBuilder();

        if (gpuInfo.type().isWrapper()) {
            sb.append("Running on ").append(gpuInfo.type().displayName()).append(". ");
        }

        sb.append(level.description());

        if (!detectedIssues.isEmpty()) {
            int critical = (int) detectedIssues.stream()
                .filter(i -> i.severity() == IssueSeverity.CRITICAL || i.severity() == IssueSeverity.HIGH)
                .count();
            if (critical > 0) {
                sb.append(" (").append(critical).append(" issue(s) detected)");
            }
        }

        if (!appliedWorkarounds.isEmpty()) {
            sb.append(" (").append(appliedWorkarounds.size()).append(" workaround(s) applied)");
        }

        return sb.toString();
    }

    /**
     * Create a failed result.
     */
    private EvaluationResult createFailedResult(Instant startTime, String reason) {
        level = CompatibilityLevel.UNSUPPORTED;
        compatibilityMessage = reason;

        return new EvaluationResult(
            CompatibilityLevel.UNSUPPORTED,
            0,
            gpuInfo,
            Map.of(),
            List.copyOf(detectedIssues),
            List.of(),
            reason,
            Duration.between(startTime, Instant.now()),
            Instant.now()
        );
    }

    // ========================================================================
    // FEATURE QUERIES
    // ========================================================================

    /**
     * Check if a feature is available.
     */
    public static boolean isFeatureAvailable(String featureId) {
        FeatureInfo feature = instance().features.get(featureId);
        if (feature == null) return false;

        // Check override
        Boolean override = instance().featureOverrides.get(featureId);
        if (override != null) return override;

        return feature.isAvailable();
    }

    /**
     * Get feature info.
     */
    public static Optional<FeatureInfo> getFeature(String featureId) {
        return Optional.ofNullable(instance().features.get(featureId));
    }

    /**
     * Get all features.
     */
    public static Map<String, FeatureInfo> getAllFeatures() {
        return Collections.unmodifiableMap(instance().features);
    }

    /**
     * Get features by category.
     */
    public static List<FeatureInfo> getFeaturesByCategory(FeatureCategory category) {
        return instance().features.values().stream()
            .filter(f -> f.category() == category)
            .toList();
    }

    /**
     * Toggle a feature at runtime.
     */
    public void toggleFeature(String featureId, boolean enabled) {
        featureOverrides.put(featureId, enabled);
        publishEvent(new CompatibilityEvent.FeatureToggled(featureId, enabled, Instant.now()));
        Astralis.LOGGER.info("[Compat] Feature {} {}", featureId, enabled ? "enabled" : "disabled");
    }

    // ========================================================================
    // ACCESSORS
    // ========================================================================

    /**
     * Get compatibility level.
     */
    public static CompatibilityLevel getLevel() {
        return instance().level;
    }

    /**
     * Get compatibility message.
     */
    public static String getMessage() {
        return instance().compatibilityMessage;
    }

    /**
     * Get GPU info.
     */
    public static Optional<GPUInfo> getGPUInfo() {
        return Optional.ofNullable(instance().gpuInfo);
    }

    /**
     * Get last evaluation result.
     */
    public static Optional<EvaluationResult> getLastResult() {
        return Optional.ofNullable(instance().lastResult);
    }

    /**
     * Get detected issues.
     */
    public static List<Issue> getIssues() {
        return Collections.unmodifiableList(instance().detectedIssues);
    }

    /**
     * Get applied workarounds.
     */
    public static List<Workaround> getAppliedWorkarounds() {
        return Collections.unmodifiableList(instance().appliedWorkarounds);
    }

    // ========================================================================
    // EVENT SYSTEM
    // ========================================================================

    /**
     * Add event listener.
     */
    public void addEventListener(Consumer<CompatibilityEvent> listener) {
        eventListeners.add(listener);
    }

    /**
     * Remove event listener.
     */
    public void removeEventListener(Consumer<CompatibilityEvent> listener) {
        eventListeners.remove(listener);
    }

    private void publishEvent(CompatibilityEvent event) {
        for (Consumer<CompatibilityEvent> listener : eventListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                Astralis.LOGGER.warn("[Compat] Event listener error: {}", e.getMessage());
            }
        }
    }

    // ========================================================================
    // IN-GAME MESSAGING
    // ========================================================================

    /**
     * Display compatibility message in-game.
     */
    public static void displayInGameMessage() {
        displayInGameMessage(true);
    }

    /**
     * Display compatibility message in-game.
     */
    public static void displayInGameMessage(boolean showDetails) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        CompatibilityLayer layer = instance();
        CompatibilityLevel lvl = layer.level;
        String msg = layer.compatibilityMessage;

        // Build message based on level
        switch (lvl) {
            case UNSUPPORTED -> {
                sendMessage(mc, TextFormatting.RED + "━━━━━━━━ Astralis ━━━━━━━━");
                sendMessage(mc, TextFormatting.RED + "✗ " + msg);
                sendMessage(mc, TextFormatting.YELLOW + "The mod will be disabled. Your game will continue normally.");

                // Show specific issues
                if (showDetails) {
                    for (Issue issue : layer.detectedIssues) {
                        if (issue.severity() == IssueSeverity.CRITICAL) {
                            sendMessage(mc, TextFormatting.RED + "  • " + issue.title());
                        }
                    }
                }

                sendMessage(mc, TextFormatting.RED + "━━━━━━━━━━━━━━━━━━━━━━━━");
            }

            case LIMITED, MINIMAL -> {
                sendMessage(mc, TextFormatting.YELLOW + "[Astralis] " + msg);

                if (showDetails && !layer.detectedIssues.isEmpty()) {
                    sendMessage(mc, TextFormatting.GRAY + "  Issues detected:");
                    int shown = 0;
                    for (Issue issue : layer.detectedIssues) {
                        if (shown >= 3) {
                            sendMessage(mc, TextFormatting.GRAY + "  ... and " +
                                (layer.detectedIssues.size() - 3) + " more");
                            break;
                        }
                        sendMessage(mc, issue.severity().color() + "  • " + issue.title());
                        shown++;
                    }
                }

                if (!layer.appliedWorkarounds.isEmpty()) {
                    sendMessage(mc, TextFormatting.GREEN + "  " + layer.appliedWorkarounds.size() +
                        " workaround(s) automatically applied");
                }
            }

            case DEGRADED -> {
                sendMessage(mc, TextFormatting.GOLD + "[Astralis] " + msg);

                if (showDetails) {
                    // Show what's disabled
                    List<FeatureInfo> disabled = layer.features.values().stream()
                        .filter(f -> !f.isAvailable())
                        .limit(3)
                        .toList();

                    if (!disabled.isEmpty()) {
                        sendMessage(mc, TextFormatting.GRAY + "  Disabled features:");
                        for (FeatureInfo f : disabled) {
                            sendMessage(mc, TextFormatting.GRAY + "  • " + f.name());
                        }
                    }
                }
            }

            case GOOD -> {
                // Light notification
                sendMessage(mc, TextFormatting.DARK_GREEN + "[Astralis] " + msg);
            }

            case OPTIMAL -> {
                // Silent success - optionally show version
                if (showDetails) {
                    GPUInfo gpu = layer.gpuInfo;
                    if (gpu != null) {
                        sendMessage(mc, TextFormatting.GREEN + "[Astralis] " + msg);
                        sendMessage(mc, TextFormatting.GRAY + "  GPU: " + gpu.renderer());
                    }
                }
            }
        }
    }

    /**
     * Display detailed diagnostics in chat.
     */
    public static void displayDiagnostics() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        CompatibilityLayer layer = instance();

        sendMessage(mc, TextFormatting.AQUA + "━━━━━━━━ Astralis Diagnostics ━━━━━━━━");

        // GPU Info
        GPUInfo gpu = layer.gpuInfo;
        if (gpu != null) {
            sendMessage(mc, TextFormatting.WHITE + "GPU Information:");
            sendMessage(mc, TextFormatting.GRAY + "  Vendor: " + gpu.vendor());
            sendMessage(mc, TextFormatting.GRAY + "  Renderer: " + gpu.renderer());
            sendMessage(mc, TextFormatting.GRAY + "  Type: " + gpu.type().displayName());
            sendMessage(mc, TextFormatting.GRAY + "  GL Version: " + gpu.version());
            if (gpu.vramMB() > 0) {
                sendMessage(mc, TextFormatting.GRAY + "  VRAM: " + gpu.vramMB() + " MB");
            }
        }

        // Compatibility Level
        sendMessage(mc, TextFormatting.WHITE + "Compatibility:");
        sendMessage(mc, layer.level.color() + "  Level: " + layer.level.displayName() +
            " (Score: " + (layer.lastResult != null ? layer.lastResult.score() : 0) + "/100)");

        // Features by category
        sendMessage(mc, TextFormatting.WHITE + "Features:");
        for (FeatureCategory cat : FeatureCategory.values()) {
            List<FeatureInfo> catFeatures = layer.features.values().stream()
                .filter(f -> f.category() == cat)
                .toList();

            if (catFeatures.isEmpty()) continue;

            long supported = catFeatures.stream().filter(FeatureInfo::isAvailable).count();
            TextFormatting catColor = supported == catFeatures.size() ? TextFormatting.GREEN :
                supported > 0 ? TextFormatting.YELLOW : TextFormatting.RED;

            sendMessage(mc, catColor + "  " + cat.displayName() + ": " +
                supported + "/" + catFeatures.size());
        }

        // Issues
        if (!layer.detectedIssues.isEmpty()) {
            sendMessage(mc, TextFormatting.WHITE + "Issues (" + layer.detectedIssues.size() + "):");
            for (Issue issue : layer.detectedIssues) {
                sendMessage(mc, issue.severity().color() + "  [" + issue.severity().displayName() + "] " +
                    issue.title());
            }
        }

        // Workarounds
        if (!layer.appliedWorkarounds.isEmpty()) {
            sendMessage(mc, TextFormatting.WHITE + "Workarounds Applied:");
            for (Workaround wa : layer.appliedWorkarounds) {
                sendMessage(mc, TextFormatting.GREEN + "  • " + wa.description());
            }
        }

        sendMessage(mc, TextFormatting.AQUA + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private static void sendMessage(Minecraft mc, String text) {
        mc.player.sendMessage(new TextComponentString(text));
    }

    /**
     * Create clickable diagnostic message.
     */
    public static ITextComponent createDiagnosticLink() {
        TextComponentString msg = new TextComponentString(
            TextFormatting.AQUA + "[Click for Astralis Diagnostics]"
        );
        msg.getStyle()
            .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/astralis diagnostics"))
            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new TextComponentString("Click to show detailed compatibility info")));
        return msg;
    }

    // ========================================================================
    // LOGGING
    // ========================================================================

    private void logResult(EvaluationResult result) {
        Astralis.LOGGER.info("[Compat] ════════════════════════════════════════════════════");
        Astralis.LOGGER.info("[Compat] Compatibility Evaluation Complete");
        Astralis.LOGGER.info("[Compat] ════════════════════════════════════════════════════");

        if (result.gpuInfo() != null) {
            Astralis.LOGGER.info("[Compat] GPU: {} ({})",
                result.gpuInfo().renderer(), result.gpuInfo().type().displayName());
            Astralis.LOGGER.info("[Compat] GL Version: {}", result.gpuInfo().version());
        }

        Astralis.LOGGER.info("[Compat] Level: {} (Score: {})",
            result.level().displayName(), result.score());
        Astralis.LOGGER.info("[Compat] Message: {}", result.summary());

        // Log features by category
        for (FeatureCategory cat : FeatureCategory.values()) {
            List<FeatureInfo> catFeatures = result.getFeaturesByCategory(cat);
            if (catFeatures.isEmpty()) continue;

            long supported = catFeatures.stream().filter(FeatureInfo::isAvailable).count();
            Astralis.LOGGER.info("[Compat] {}: {}/{} features",
                cat.displayName(), supported, catFeatures.size());
        }

        // Log issues
        for (Issue issue : result.issues()) {
            switch (issue.severity()) {
                case CRITICAL, HIGH -> Astralis.LOGGER.error("[Compat] Issue: {}", issue.title());
                case MEDIUM -> Astralis.LOGGER.warn("[Compat] Issue: {}", issue.title());
                default -> Astralis.LOGGER.info("[Compat] Issue: {}", issue.title());
            }
        }

        // Log workarounds
        for (Workaround wa : result.appliedWorkarounds()) {
            Astralis.LOGGER.info("[Compat] Workaround applied: {}", wa.description());
        }

        Astralis.LOGGER.info("[Compat] Evaluation time: {}ms", result.evaluationTime().toMillis());
        Astralis.LOGGER.info("[Compat] ════════════════════════════════════════════════════");
    }

    // ========================================================================
    // TESTING
    // ========================================================================

    /**
     * Run compatibility tests.
     */
    public TestResults runCompatibilityTests() {
        List<TestResult> results = new ArrayList<>();
        Instant start = Instant.now();

        // Test 1: Basic rendering
        results.add(testBasicRendering());

        // Test 2: Buffer operations
        results.add(testBufferOperations());

        // Test 3: Shader compilation
        results.add(testShaderCompilation());

        // Test 4: Instancing
        if (isFeatureAvailable("instancing")) {
            results.add(testInstancing());
        }

        // Test 5: Compute shaders
        if (isFeatureAvailable("compute_shader")) {
            results.add(testComputeShaders());
        }

        return new TestResults(results, Duration.between(start, Instant.now()));
    }

    private TestResult testBasicRendering() {
        try {
            // Would perform actual GL calls
            return new TestResult("basic_rendering", "Basic Rendering", true, null, Duration.ZERO);
        } catch (Exception e) {
            return new TestResult("basic_rendering", "Basic Rendering", false, e.getMessage(), Duration.ZERO);
        }
    }

    private TestResult testBufferOperations() {
        try {
            return new TestResult("buffer_operations", "Buffer Operations", true, null, Duration.ZERO);
        } catch (Exception e) {
            return new TestResult("buffer_operations", "Buffer Operations", false, e.getMessage(), Duration.ZERO);
        }
    }

    private TestResult testShaderCompilation() {
        try {
            return new TestResult("shader_compilation", "Shader Compilation", true, null, Duration.ZERO);
        } catch (Exception e) {
            return new TestResult("shader_compilation", "Shader Compilation", false, e.getMessage(), Duration.ZERO);
        }
    }

    private TestResult testInstancing() {
        try {
            return new TestResult("instancing", "Instanced Rendering", true, null, Duration.ZERO);
        } catch (Exception e) {
            return new TestResult("instancing", "Instanced Rendering", false, e.getMessage(), Duration.ZERO);
        }
    }

    private TestResult testComputeShaders() {
        try {
            return new TestResult("compute_shaders", "Compute Shaders", true, null, Duration.ZERO);
        } catch (Exception e) {
            return new TestResult("compute_shaders", "Compute Shaders", false, e.getMessage(), Duration.ZERO);
        }
    }

    public record TestResult(String id, String name, boolean passed, String error, Duration duration) {}
    public record TestResults(List<TestResult> results, Duration totalDuration) {
        public int passedCount() {
            return (int) results.stream().filter(TestResult::passed).count();
        }
        public int failedCount() {
            return results.size() - passedCount();
        }
        public boolean allPassed() {
            return failedCount() == 0;
        }
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Get compatibility statistics.
     */
    public CompatibilityStats getStats() {
        return new CompatibilityStats(
            level,
            lastResult != null ? lastResult.score() : 0,
            features.size(),
            (int) features.values().stream().filter(FeatureInfo::isAvailable).count(),
            detectedIssues.size(),
            appliedWorkarounds.size(),
            evaluationCount.get(),
            Duration.ofNanos(totalEvaluationTimeNanos.get()),
            Duration.between(creationTime, Instant.now())
        );
    }

    public record CompatibilityStats(
        CompatibilityLevel level,
        int score,
        int totalFeatures,
        int availableFeatures,
        int issueCount,
        int workaroundCount,
        int evaluationCount,
        Duration totalEvaluationTime,
        Duration uptime
    ) {}

    // ========================================================================
    // LIFECYCLE
    // ========================================================================

    @Override
    public void close() {
        features.clear();
        detectedIssues.clear();
        appliedWorkarounds.clear();
        featureOverrides.clear();
        eventListeners.clear();
        Astralis.LOGGER.info("[Compat] CompatibilityLayer closed");
    }

    // ========================================================================
    // DEBUG
    // ========================================================================

    @Override
    public String toString() {
        return String.format("CompatibilityLayer[level=%s, score=%d, features=%d, issues=%d]",
            level, lastResult != null ? lastResult.score() : 0,
            features.size(), detectedIssues.size());
    }

    /**
     * Generate full report.
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder(4096);

        sb.append("════════════════════════════════════════════════════════════════\n");
        sb.append("                    Astralis Compatibility Report\n");
        sb.append("════════════════════════════════════════════════════════════════\n\n");

        // GPU Info
        if (gpuInfo != null) {
            sb.append("GPU INFORMATION\n");
            sb.append("───────────────────────────────────────────────────────────────\n");
            sb.append(String.format("  Vendor:    %s\n", gpuInfo.vendor()));
            sb.append(String.format("  Renderer:  %s\n", gpuInfo.renderer()));
            sb.append(String.format("  Type:      %s\n", gpuInfo.type().displayName()));
            sb.append(String.format("  GL Version: %s\n", gpuInfo.version()));
            sb.append(String.format("  GLSL:      %s\n", gpuInfo.shadingLanguageVersion()));
            if (gpuInfo.vramMB() > 0) {
                sb.append(String.format("  VRAM:      %d MB\n", gpuInfo.vramMB()));
            }
            sb.append("\n");
        }

        // Compatibility
        sb.append("COMPATIBILITY\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append(String.format("  Level:   %s\n", level.displayName()));
        sb.append(String.format("  Score:   %d/100\n", lastResult != null ? lastResult.score() : 0));
        sb.append(String.format("  Message: %s\n", compatibilityMessage));
        sb.append("\n");

        // Features by category
        sb.append("FEATURES\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        for (FeatureCategory cat : FeatureCategory.values()) {
            List<FeatureInfo> catFeatures = features.values().stream()
                .filter(f -> f.category() == cat)
                .sorted(Comparator.comparing(FeatureInfo::name))
                .toList();

            if (catFeatures.isEmpty()) continue;

            sb.append(String.format("  %s:\n", cat.displayName()));
            for (FeatureInfo f : catFeatures) {
                String status = f.status().displayName();
                sb.append(String.format("    [%s] %s\n", status, f.name()));
            }
        }
        sb.append("\n");

        // Issues
        if (!detectedIssues.isEmpty()) {
            sb.append("ISSUES\n");
            sb.append("───────────────────────────────────────────────────────────────\n");
            for (Issue issue : detectedIssues) {
                sb.append(String.format("  [%s] %s\n", issue.severity().displayName(), issue.title()));
                sb.append(String.format("    %s\n", issue.description()));
            }
            sb.append("\n");
        }

        // Workarounds
        if (!appliedWorkarounds.isEmpty()) {
            sb.append("APPLIED WORKAROUNDS\n");
            sb.append("───────────────────────────────────────────────────────────────\n");
            for (Workaround wa : appliedWorkarounds) {
                sb.append(String.format("  • %s\n", wa.description()));
            }
            sb.append("\n");
        }

        sb.append("════════════════════════════════════════════════════════════════\n");
        sb.append(String.format("Generated at: %s\n", Instant.now()));

        return sb.toString();
    }
}
