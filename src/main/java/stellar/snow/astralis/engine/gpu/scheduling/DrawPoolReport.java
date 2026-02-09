package stellar.snow.astralis.engine.gpu.scheduling;

// ═══════════════════════════════════════════════════════════════════════════════════════════════════
// ██████████████████████████████████████████████████████████████████████████████████████████████████
// ██                                                                                              ██
// ██    ██████╗ ██████╗  █████╗ ██╗    ██╗██████╗  ██████╗  ██████╗ ██╗                           ██
// ██    ██╔══██╗██╔══██╗██╔══██╗██║    ██║██╔══██╗██╔═══██╗██╔═══██╗██║                           ██
// ██    ██║  ██║██████╔╝███████║██║ █╗ ██║██████╔╝██║   ██║██║   ██║██║                           ██
// ██    ██║  ██║██╔══██╗██╔══██║██║███╗██║██╔═══╝ ██║   ██║██║   ██║██║                           ██
// ██    ██████╔╝██║  ██║██║  ██║╚███╔███╔╝██║     ╚██████╔╝╚██████╔╝███████╗                      ██
// ██    ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝ ╚══╝╚══╝ ╚═╝      ╚═════╝  ╚═════╝ ╚══════╝                      ██
// ██                                                                                              ██
// ██    ██████╗ ███████╗██████╗  ██████╗ ██████╗ ████████╗                                        ██
// ██    ██╔══██╗██╔════╝██╔══██╗██╔═══██╗██╔══██╗╚══██╔══╝                                        ██
// ██    ██████╔╝█████╗  ██████╔╝██║   ██║██████╔╝   ██║                                           ██
// ██    ██╔══██╗██╔══╝  ██╔═══╝ ██║   ██║██╔══██╗   ██║                                           ██
// ██    ██║  ██║███████╗██║     ╚██████╔╝██║  ██║   ██║                                           ██
// ██    ╚═╝  ╚═╝╚══════╝╚═╝      ╚═════╝ ╚═╝  ╚═╝   ╚═╝                                           ██
// ██                                                                                              ██
// ██    COMPREHENSIVE DRAW POOL ANALYSIS REPORT                                                   ██
// ██    Version: 4.0.0 | Immutable | Thread-Safe | Zero-Allocation Query                         ██
// ██                                                                                              ██
// ██████████████████████████████████████████████████████████████████████████████████████████████████
// ═══════════════════════════════════════════════════════════════════════════════════════════════════

// ─── Java 25 Core ───
import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.SoftReference;

// ─── Java 25 Concurrency ───
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.ConcurrentHashMap;

// ─── Java 25 Vector API ───
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

// ─── Collections & Utilities ───
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

// ─── NIO ───
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

// ─── Time ───
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// ─── Annotations ───
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════════════════════════════╗
 * ║                                      DRAW POOL REPORT                                              ║
 * ╠═══════════════════════════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                                                   ║
 * ║  Comprehensive, immutable analysis report generated by DrawPool.                                  ║
 * ║                                                                                                   ║
 * ║  REPORT SECTIONS:                                                                                 ║
 * ║  ═════════════════                                                                                ║
 * ║                                                                                                   ║
 * ║  ┌─────────────────────────────────────────────────────────────────────────────────────────────┐  ║
 * ║  │ 1. FRAME SUMMARY                                                                            │  ║
 * ║  │    • Frame number, timestamp, duration                                                      │  ║
 * ║  │    • Total draw count, batch count, state changes                                           │  ║
 * ║  │    • Vertex/index/instance totals                                                           │  ║
 * ║  └─────────────────────────────────────────────────────────────────────────────────────────────┘  ║
 * ║                                                                                                   ║
 * ║  ┌─────────────────────────────────────────────────────────────────────────────────────────────┐  ║
 * ║  │ 2. PERFORMANCE METRICS                                                                      │  ║
 * ║  │    • Draw call overhead analysis                                                            │  ║
 * ║  │    • State change cost estimation                                                           │  ║
 * ║  │    • Batching efficiency percentage                                                         │  ║
 * ║  │    • GPU utilization estimate                                                               │  ║
 * ║  └─────────────────────────────────────────────────────────────────────────────────────────────┘  ║
 * ║                                                                                                   ║
 * ║  ┌─────────────────────────────────────────────────────────────────────────────────────────────┐  ║
 * ║  │ 3. PATTERN ANALYSIS                                                                         │  ║
 * ║  │    • Recurring draw patterns detected                                                       │  ║
 * ║  │    • Static vs dynamic geometry ratio                                                       │  ║
 * ║  │    • Shader/VAO distribution                                                                │  ║
 * ║  └─────────────────────────────────────────────────────────────────────────────────────────────┘  ║
 * ║                                                                                                   ║
 * ║  ┌─────────────────────────────────────────────────────────────────────────────────────────────┐  ║
 * ║  │ 4. RECOMMENDATIONS                                                                          │  ║
 * ║  │    • Patterns eligible for indirect drawing                                                 │  ║
 * ║  │    • State sorting improvements                                                             │  ║
 * ║  │    • Memory optimization suggestions                                                        │  ║
 * ║  └─────────────────────────────────────────────────────────────────────────────────────────────┘  ║
 * ║                                                                                                   ║
 * ╚═══════════════════════════════════════════════════════════════════════════════════════════════════╝
 */
@Immutable
@ThreadSafe
public final class DrawPoolReport {

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 1: CONSTANTS
    // ════════════════════════════════════════════════════════════════════════════════════════════

    public static final int VERSION = 0x04_00_00;
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    // Performance thresholds
    private static final int EXCELLENT_BATCH_EFFICIENCY_PERCENT = 90;
    private static final int GOOD_BATCH_EFFICIENCY_PERCENT = 70;
    private static final int ACCEPTABLE_BATCH_EFFICIENCY_PERCENT = 50;
    
    private static final int LOW_DRAW_COUNT = 100;
    private static final int MODERATE_DRAW_COUNT = 500;
    private static final int HIGH_DRAW_COUNT = 2000;
    private static final int CRITICAL_DRAW_COUNT = 5000;
    
    private static final long LOW_ANALYSIS_TIME_NS = 50_000;        // 50µs
    private static final long MODERATE_ANALYSIS_TIME_NS = 200_000;  // 200µs
    private static final long HIGH_ANALYSIS_TIME_NS = 500_000;      // 500µs

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 2: CORE DATA (Immutable)
    // ════════════════════════════════════════════════════════════════════════════════════════════

    private final long frameNumber;
    private final Instant timestamp;
    private final long generationTimeNanos;
    
    // Draw statistics
    private final int totalDrawCalls;
    private final int batchedDrawCalls;
    private final int actualDrawCalls;
    private final int stateChanges;
    private final int duplicatesEliminated;
    
    // Geometry statistics
    private final long totalVertices;
    private final long totalIndices;
    private final long totalInstances;
    
    // Timing
    private final long analysisTimeNanos;
    private final long sortTimeNanos;
    private final long batchingTimeNanos;
    private final long executionTimeNanos;
    
    // Analysis results
    private final List<DrawCallCluster> heavyClusters;
    private final List<DrawCallCluster> repeatedPatterns;
    private final Map<Long, Integer> shaderDrawCounts;
    private final Map<Long, Integer> vaoDrawCounts;
    private final Map<Long, Integer> textureDrawCounts;
    
    // Recommendations
    private final double savingsPercentage;
    private final List<Long> recommendedForIndirect;
    private final List<Recommendation> recommendations;
    private final HealthStatus healthStatus;
    
    // Serialized cache
    private volatile SoftReference<String> cachedJsonRef;
    private volatile SoftReference<byte[]> cachedBinaryRef;

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 3: NESTED TYPES
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Health status classification.
     */
    public enum HealthStatus {
        EXCELLENT("🟢", "Excellent performance"),
        GOOD("🟡", "Good performance with minor opportunities"),
        ACCEPTABLE("🟠", "Acceptable but optimization recommended"),
        POOR("🔴", "Poor performance - optimization required"),
        CRITICAL("⛔", "Critical - immediate action required");

        private final String icon;
        private final String description;

        HealthStatus(String icon, String description) {
            this.icon = icon;
            this.description = description;
        }

        public String icon() { return icon; }
        public String description() { return description; }
    }

    /**
     * Severity level for recommendations.
     */
    public enum Severity {
        INFO(0, "ℹ️"),
        SUGGESTION(1, "💡"),
        WARNING(2, "⚠️"),
        CRITICAL(3, "🚨");

        private final int level;
        private final String icon;

        Severity(int level, String icon) {
            this.level = level;
            this.icon = icon;
        }

        public int level() { return level; }
        public String icon() { return icon; }
    }

    /**
     * Recommendation category.
     */
    public enum RecommendationCategory {
        BATCHING,
        STATE_SORTING,
        INDIRECT_DRAWING,
        MEMORY,
        DEDUPLICATION,
        GEOMETRY,
        SHADER,
        TEXTURE,
        GENERAL
    }

    /**
     * Individual recommendation.
     */
    @Immutable
    public record Recommendation(
        Severity severity,
        RecommendationCategory category,
        String title,
        String description,
        String actionItem,
        OptionalDouble estimatedSavings,
        List<Long> affectedPatterns
    ) {
        public Recommendation {
            Objects.requireNonNull(severity);
            Objects.requireNonNull(category);
            Objects.requireNonNull(title);
            Objects.requireNonNull(description);
            Objects.requireNonNull(actionItem);
            Objects.requireNonNull(estimatedSavings);
            affectedPatterns = affectedPatterns != null 
                ? List.copyOf(affectedPatterns) 
                : List.of();
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private Severity severity = Severity.INFO;
            private RecommendationCategory category = RecommendationCategory.GENERAL;
            private String title = "";
            private String description = "";
            private String actionItem = "";
            private OptionalDouble estimatedSavings = OptionalDouble.empty();
            private List<Long> affectedPatterns = List.of();

            public Builder severity(Severity s) { this.severity = s; return this; }
            public Builder category(RecommendationCategory c) { this.category = c; return this; }
            public Builder title(String t) { this.title = t; return this; }
            public Builder description(String d) { this.description = d; return this; }
            public Builder actionItem(String a) { this.actionItem = a; return this; }
            public Builder estimatedSavings(double s) { 
                this.estimatedSavings = OptionalDouble.of(s); 
                return this; 
            }
            public Builder affectedPatterns(List<Long> p) { 
                this.affectedPatterns = p; 
                return this; 
            }

            public Recommendation build() {
                return new Recommendation(
                    severity, category, title, description, 
                    actionItem, estimatedSavings, affectedPatterns
                );
            }
        }
    }

    /**
     * Performance tier classification.
     */
    @Immutable
    public record PerformanceTier(
        String name,
        int drawCountThreshold,
        int stateChangeThreshold,
        double minBatchEfficiency
    ) {
        public static final PerformanceTier ULTRA = 
            new PerformanceTier("Ultra", 100, 20, 0.95);
        public static final PerformanceTier HIGH = 
            new PerformanceTier("High", 500, 100, 0.85);
        public static final PerformanceTier MEDIUM = 
            new PerformanceTier("Medium", 2000, 500, 0.70);
        public static final PerformanceTier LOW = 
            new PerformanceTier("Low", 5000, 1000, 0.50);
        public static final PerformanceTier MINIMUM = 
            new PerformanceTier("Minimum", Integer.MAX_VALUE, Integer.MAX_VALUE, 0.0);
    }

    /**
     * Breakdown of where time is spent.
     */
    @Immutable
    public record TimeBreakdown(
        long analysisNanos,
        long sortNanos,
        long batchingNanos,
        long executionNanos,
        long totalNanos
    ) {
        public double analysisPercent() { 
            return totalNanos > 0 ? (double) analysisNanos / totalNanos * 100 : 0; 
        }
        public double sortPercent() { 
            return totalNanos > 0 ? (double) sortNanos / totalNanos * 100 : 0; 
        }
        public double batchingPercent() { 
            return totalNanos > 0 ? (double) batchingNanos / totalNanos * 100 : 0; 
        }
        public double executionPercent() { 
            return totalNanos > 0 ? (double) executionNanos / totalNanos * 100 : 0; 
        }

        public String formatMicroseconds() {
            return String.format(
                "Analysis: %.1fµs | Sort: %.1fµs | Batch: %.1fµs | Execute: %.1fµs | Total: %.1fµs",
                analysisNanos / 1000.0,
                sortNanos / 1000.0,
                batchingNanos / 1000.0,
                executionNanos / 1000.0,
                totalNanos / 1000.0
            );
        }
    }

    /**
     * Summary statistics for quick overview.
     */
    @Immutable
    public record Summary(
        int totalDraws,
        int actualDraws,
        int stateChanges,
        double batchEfficiency,
        double deduplicationRate,
        HealthStatus health,
        String quickDiagnosis
    ) {
        public int drawsSaved() { return totalDraws - actualDraws; }
        public boolean needsAttention() { 
            return health == HealthStatus.POOR || health == HealthStatus.CRITICAL; 
        }
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 4: CONSTRUCTORS & BUILDERS
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Primary constructor - creates immutable report.
     */
    public DrawPoolReport(
        long frameNumber,
        int totalDrawCalls,
        long analysisTimeNanos,
        List<DrawCallCluster> heavyClusters,
        List<DrawCallCluster> repeatedPatterns,
        Map<Long, Integer> shaderDrawCounts,
        Map<Long, Integer> vaoDrawCounts,
        double savingsPercentage,
        List<Long> recommendedForIndirect
    ) {
        this.frameNumber = frameNumber;
        this.timestamp = Instant.now();
        this.generationTimeNanos = System.nanoTime();
        
        this.totalDrawCalls = totalDrawCalls;
        this.batchedDrawCalls = 0;
        this.actualDrawCalls = totalDrawCalls;
        this.stateChanges = 0;
        this.duplicatesEliminated = 0;
        
        this.totalVertices = 0;
        this.totalIndices = 0;
        this.totalInstances = 0;
        
        this.analysisTimeNanos = analysisTimeNanos;
        this.sortTimeNanos = 0;
        this.batchingTimeNanos = 0;
        this.executionTimeNanos = 0;
        
        this.heavyClusters = heavyClusters != null ? List.copyOf(heavyClusters) : List.of();
        this.repeatedPatterns = repeatedPatterns != null ? List.copyOf(repeatedPatterns) : List.of();
        this.shaderDrawCounts = shaderDrawCounts != null ? Map.copyOf(shaderDrawCounts) : Map.of();
        this.vaoDrawCounts = vaoDrawCounts != null ? Map.copyOf(vaoDrawCounts) : Map.of();
        this.textureDrawCounts = Map.of();
        
        this.savingsPercentage = savingsPercentage;
        this.recommendedForIndirect = recommendedForIndirect != null 
            ? List.copyOf(recommendedForIndirect) 
            : List.of();
        
        this.recommendations = generateRecommendations();
        this.healthStatus = calculateHealthStatus();
    }

    /**
     * Full constructor via builder.
     */
    private DrawPoolReport(Builder builder) {
        this.frameNumber = builder.frameNumber;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.generationTimeNanos = System.nanoTime();
        
        this.totalDrawCalls = builder.totalDrawCalls;
        this.batchedDrawCalls = builder.batchedDrawCalls;
        this.actualDrawCalls = builder.actualDrawCalls;
        this.stateChanges = builder.stateChanges;
        this.duplicatesEliminated = builder.duplicatesEliminated;
        
        this.totalVertices = builder.totalVertices;
        this.totalIndices = builder.totalIndices;
        this.totalInstances = builder.totalInstances;
        
        this.analysisTimeNanos = builder.analysisTimeNanos;
        this.sortTimeNanos = builder.sortTimeNanos;
        this.batchingTimeNanos = builder.batchingTimeNanos;
        this.executionTimeNanos = builder.executionTimeNanos;
        
        this.heavyClusters = List.copyOf(builder.heavyClusters);
        this.repeatedPatterns = List.copyOf(builder.repeatedPatterns);
        this.shaderDrawCounts = Map.copyOf(builder.shaderDrawCounts);
        this.vaoDrawCounts = Map.copyOf(builder.vaoDrawCounts);
        this.textureDrawCounts = Map.copyOf(builder.textureDrawCounts);
        
        this.savingsPercentage = builder.savingsPercentage;
        this.recommendedForIndirect = List.copyOf(builder.recommendedForIndirect);
        
        this.recommendations = generateRecommendations();
        this.healthStatus = calculateHealthStatus();
    }

    /**
     * Builder for comprehensive report construction.
     */
    public static final class Builder {
        private long frameNumber;
        private Instant timestamp;
        private int totalDrawCalls;
        private int batchedDrawCalls;
        private int actualDrawCalls;
        private int stateChanges;
        private int duplicatesEliminated;
        private long totalVertices;
        private long totalIndices;
        private long totalInstances;
        private long analysisTimeNanos;
        private long sortTimeNanos;
        private long batchingTimeNanos;
        private long executionTimeNanos;
        private List<DrawCallCluster> heavyClusters = new ArrayList<>();
        private List<DrawCallCluster> repeatedPatterns = new ArrayList<>();
        private Map<Long, Integer> shaderDrawCounts = new HashMap<>();
        private Map<Long, Integer> vaoDrawCounts = new HashMap<>();
        private Map<Long, Integer> textureDrawCounts = new HashMap<>();
        private double savingsPercentage;
        private List<Long> recommendedForIndirect = new ArrayList<>();

        public Builder frameNumber(long n) { this.frameNumber = n; return this; }
        public Builder timestamp(Instant t) { this.timestamp = t; return this; }
        public Builder totalDrawCalls(int n) { this.totalDrawCalls = n; return this; }
        public Builder batchedDrawCalls(int n) { this.batchedDrawCalls = n; return this; }
        public Builder actualDrawCalls(int n) { this.actualDrawCalls = n; return this; }
        public Builder stateChanges(int n) { this.stateChanges = n; return this; }
        public Builder duplicatesEliminated(int n) { this.duplicatesEliminated = n; return this; }
        public Builder totalVertices(long n) { this.totalVertices = n; return this; }
        public Builder totalIndices(long n) { this.totalIndices = n; return this; }
        public Builder totalInstances(long n) { this.totalInstances = n; return this; }
        public Builder analysisTimeNanos(long n) { this.analysisTimeNanos = n; return this; }
        public Builder sortTimeNanos(long n) { this.sortTimeNanos = n; return this; }
        public Builder batchingTimeNanos(long n) { this.batchingTimeNanos = n; return this; }
        public Builder executionTimeNanos(long n) { this.executionTimeNanos = n; return this; }
        public Builder heavyClusters(List<DrawCallCluster> c) { 
            this.heavyClusters = new ArrayList<>(c); 
            return this; 
        }
        public Builder repeatedPatterns(List<DrawCallCluster> p) { 
            this.repeatedPatterns = new ArrayList<>(p); 
            return this; 
        }
        public Builder shaderDrawCounts(Map<Long, Integer> m) { 
            this.shaderDrawCounts = new HashMap<>(m); 
            return this; 
        }
        public Builder vaoDrawCounts(Map<Long, Integer> m) { 
            this.vaoDrawCounts = new HashMap<>(m); 
            return this; 
        }
        public Builder textureDrawCounts(Map<Long, Integer> m) { 
            this.textureDrawCounts = new HashMap<>(m); 
            return this; 
        }
        public Builder savingsPercentage(double p) { this.savingsPercentage = p; return this; }
        public Builder recommendedForIndirect(List<Long> r) { 
            this.recommendedForIndirect = new ArrayList<>(r); 
            return this; 
        }

        public Builder addHeavyCluster(DrawCallCluster c) { 
            this.heavyClusters.add(c); 
            return this; 
        }
        public Builder addRepeatedPattern(DrawCallCluster p) { 
            this.repeatedPatterns.add(p); 
            return this; 
        }

        public DrawPoolReport build() {
            return new DrawPoolReport(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an empty report for error cases.
     */
    public static DrawPoolReport empty() {
        return new DrawPoolReport(0, 0, 0, List.of(), List.of(), Map.of(), Map.of(), 0, List.of());
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 5: ACCESSORS
    // ════════════════════════════════════════════════════════════════════════════════════════════

    public long frameNumber() { return frameNumber; }
    public Instant timestamp() { return timestamp; }
    public int totalDrawCalls() { return totalDrawCalls; }
    public int batchedDrawCalls() { return batchedDrawCalls; }
    public int actualDrawCalls() { return actualDrawCalls; }
    public int stateChanges() { return stateChanges; }
    public int duplicatesEliminated() { return duplicatesEliminated; }
    public long totalVertices() { return totalVertices; }
    public long totalIndices() { return totalIndices; }
    public long totalInstances() { return totalInstances; }
    public long analysisTimeNanos() { return analysisTimeNanos; }
    public double savingsPercentage() { return savingsPercentage; }
    public List<Long> recommendedForIndirect() { return recommendedForIndirect; }
    public List<DrawCallCluster> heavyClusters() { return heavyClusters; }
    public List<DrawCallCluster> repeatedPatterns() { return repeatedPatterns; }
    public Map<Long, Integer> shaderDrawCounts() { return shaderDrawCounts; }
    public Map<Long, Integer> vaoDrawCounts() { return vaoDrawCounts; }
    public Map<Long, Integer> textureDrawCounts() { return textureDrawCounts; }
    public List<Recommendation> recommendations() { return recommendations; }
    public HealthStatus healthStatus() { return healthStatus; }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 6: COMPUTED METRICS
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Batch efficiency as a percentage (0-100).
     */
    public double batchEfficiency() {
        if (totalDrawCalls == 0) return 100.0;
        return (1.0 - (double) actualDrawCalls / totalDrawCalls) * 100.0;
    }

    /**
     * Deduplication rate as a percentage.
     */
    public double deduplicationRate() {
        int total = totalDrawCalls + duplicatesEliminated;
        if (total == 0) return 0.0;
        return (double) duplicatesEliminated / total * 100.0;
    }

    /**
     * State changes per draw call ratio.
     */
    public double stateChangesPerDraw() {
        if (actualDrawCalls == 0) return 0.0;
        return (double) stateChanges / actualDrawCalls;
    }

    /**
     * Average vertices per draw.
     */
    public double averageVerticesPerDraw() {
        if (actualDrawCalls == 0) return 0.0;
        return (double) totalVertices / actualDrawCalls;
    }

    /**
     * Estimated CPU overhead in nanoseconds.
     */
    public long estimatedCpuOverheadNanos() {
        // Rough estimation: 5µs per draw call + 2µs per state change
        return actualDrawCalls * 5_000L + stateChanges * 2_000L;
    }

    /**
     * Time breakdown analysis.
     */
    public TimeBreakdown timeBreakdown() {
        long total = analysisTimeNanos + sortTimeNanos + batchingTimeNanos + executionTimeNanos;
        return new TimeBreakdown(
            analysisTimeNanos, sortTimeNanos, batchingTimeNanos, executionTimeNanos, total
        );
    }

    /**
     * Quick summary for dashboards.
     */
    public Summary summary() {
        String diagnosis = switch (healthStatus) {
            case EXCELLENT -> "Optimal draw batching achieved";
            case GOOD -> "Minor optimization opportunities available";
            case ACCEPTABLE -> "Consider enabling additional optimizations";
            case POOR -> "Significant optimization needed - high draw count";
            case CRITICAL -> "CRITICAL: Excessive draw calls causing performance issues";
        };

        return new Summary(
            totalDrawCalls,
            actualDrawCalls,
            stateChanges,
            batchEfficiency(),
            deduplicationRate(),
            healthStatus,
            diagnosis
        );
    }

    /**
     * Determines performance tier.
     */
    public PerformanceTier performanceTier() {
        double efficiency = batchEfficiency() / 100.0;
        
        if (actualDrawCalls <= PerformanceTier.ULTRA.drawCountThreshold() 
            && stateChanges <= PerformanceTier.ULTRA.stateChangeThreshold()
            && efficiency >= PerformanceTier.ULTRA.minBatchEfficiency()) {
            return PerformanceTier.ULTRA;
        }
        if (actualDrawCalls <= PerformanceTier.HIGH.drawCountThreshold() 
            && efficiency >= PerformanceTier.HIGH.minBatchEfficiency()) {
            return PerformanceTier.HIGH;
        }
        if (actualDrawCalls <= PerformanceTier.MEDIUM.drawCountThreshold() 
            && efficiency >= PerformanceTier.MEDIUM.minBatchEfficiency()) {
            return PerformanceTier.MEDIUM;
        }
        if (actualDrawCalls <= PerformanceTier.LOW.drawCountThreshold()) {
            return PerformanceTier.LOW;
        }
        return PerformanceTier.MINIMUM;
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 7: ANALYSIS
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Calculates health status based on metrics.
     */
    private HealthStatus calculateHealthStatus() {
        double efficiency = batchEfficiency();
        
        if (actualDrawCalls >= CRITICAL_DRAW_COUNT) {
            return HealthStatus.CRITICAL;
        }
        if (actualDrawCalls >= HIGH_DRAW_COUNT || efficiency < ACCEPTABLE_BATCH_EFFICIENCY_PERCENT) {
            return HealthStatus.POOR;
        }
        if (actualDrawCalls >= MODERATE_DRAW_COUNT || efficiency < GOOD_BATCH_EFFICIENCY_PERCENT) {
            return HealthStatus.ACCEPTABLE;
        }
        if (efficiency >= EXCELLENT_BATCH_EFFICIENCY_PERCENT && actualDrawCalls < LOW_DRAW_COUNT) {
            return HealthStatus.EXCELLENT;
        }
        return HealthStatus.GOOD;
    }

    /**
     * Generates recommendations based on analysis.
     */
    private List<Recommendation> generateRecommendations() {
        List<Recommendation> recs = new ArrayList<>();

        // Check for indirect draw opportunities
        if (!recommendedForIndirect.isEmpty()) {
            recs.add(Recommendation.builder()
                .severity(Severity.SUGGESTION)
                .category(RecommendationCategory.INDIRECT_DRAWING)
                .title("Enable Indirect Drawing")
                .description(String.format(
                    "%d patterns detected that would benefit from indirect drawing",
                    recommendedForIndirect.size()
                ))
                .actionItem("Convert repeated patterns to glMultiDrawIndirect calls")
                .estimatedSavings(Math.min(40.0, recommendedForIndirect.size() * 2.0))
                .affectedPatterns(recommendedForIndirect)
                .build());
        }

        // Check draw count
        if (actualDrawCalls >= CRITICAL_DRAW_COUNT) {
            recs.add(Recommendation.builder()
                .severity(Severity.CRITICAL)
                .category(RecommendationCategory.BATCHING)
                .title("Critical Draw Count")
                .description(String.format(
                    "Draw count (%d) exceeds critical threshold (%d)",
                    actualDrawCalls, CRITICAL_DRAW_COUNT
                ))
                .actionItem("Enable aggressive batching and consider geometry instancing")
                .estimatedSavings(50.0)
                .build());
        } else if (actualDrawCalls >= HIGH_DRAW_COUNT) {
            recs.add(Recommendation.builder()
                .severity(Severity.WARNING)
                .category(RecommendationCategory.BATCHING)
                .title("High Draw Count")
                .description(String.format(
                    "Draw count (%d) is high - consider additional batching",
                    actualDrawCalls
                ))
                .actionItem("Review draw submission patterns for merge opportunities")
                .estimatedSavings(25.0)
                .build());
        }

        // Check state changes
        if (stateChanges > actualDrawCalls * 0.5) {
            recs.add(Recommendation.builder()
                .severity(Severity.WARNING)
                .category(RecommendationCategory.STATE_SORTING)
                .title("Excessive State Changes")
                .description(String.format(
                    "State changes (%d) exceed 50%% of draw calls",
                    stateChanges
                ))
                .actionItem("Enable state-based sorting to minimize pipeline flushes")
                .estimatedSavings(20.0)
                .build());
        }

        // Check shader distribution
        if (shaderDrawCounts.size() > 20) {
            recs.add(Recommendation.builder()
                .severity(Severity.SUGGESTION)
                .category(RecommendationCategory.SHADER)
                .title("High Shader Diversity")
                .description(String.format(
                    "%d different shaders used - consider consolidation",
                    shaderDrawCounts.size()
                ))
                .actionItem("Evaluate uber-shader approach for similar materials")
                .estimatedSavings(15.0)
                .build());
        }

        // Check analysis time
        if (analysisTimeNanos > HIGH_ANALYSIS_TIME_NS) {
            recs.add(Recommendation.builder()
                .severity(Severity.INFO)
                .category(RecommendationCategory.GENERAL)
                .title("High Analysis Overhead")
                .description(String.format(
                    "Analysis taking %.1fµs - consider reducing draw complexity",
                    analysisTimeNanos / 1000.0
                ))
                .actionItem("Profile draw submission to identify bottlenecks")
                .build());
        }

        // Sort by severity
        recs.sort(Comparator.comparingInt(r -> -r.severity().level()));

        return List.copyOf(recs);
    }

    /**
     * Gets top N problematic patterns.
     */
    public List<DrawCallCluster> topProblematicPatterns(int n) {
        return heavyClusters.stream()
            .sorted(Comparator.comparingLong(DrawCallCluster::totalTimeNanos).reversed())
            .limit(n)
            .toList();
    }

    /**
     * Gets shaders sorted by draw count.
     */
    public List<Map.Entry<Long, Integer>> shadersByDrawCount() {
        return shaderDrawCounts.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .toList();
    }

    /**
     * Checks if specific optimization is recommended.
     */
    public boolean isOptimizationRecommended(RecommendationCategory category) {
        return recommendations.stream()
            .anyMatch(r -> r.category() == category && r.severity().level() >= Severity.SUGGESTION.level());
    }

    // ════════════════════════════════════════════════════════════════════════════════════════════
    // ██ SECTION 8: SERIALIZATION
    // ════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * Exports report as JSON string.
     */
    public String toJson() {
        SoftReference<String> ref = cachedJsonRef;
        String cached = ref != null ? ref.get() : null;
        if (cached != null) return cached;

        StringBuilder json = new StringBuilder(4096);
        json.append("{\n");
        json.append("  \"version\": ").append(VERSION).append(",\n");
        json.append("  \"frameNumber\": ").append(frameNumber).append(",\n");
        json.append("  \"timestamp\": \"").append(timestamp).append("\",\n");
        json.append("  \"summary\": {\n");
        json.append("    \"totalDrawCalls\": ").append(totalDrawCalls).append(",\n");
        json.append("    \"actualDrawCalls\": ").append(actualDrawCalls).append(",\n");
        json.append("    \"stateChanges\": ").append(stateChanges).append(",\n");
        json.append("    \"duplicatesEliminated\": ").append(duplicatesEliminated).append(",\n");
        json.append("    \"batchEfficiency\": ").append(String.format("%.2f", batchEfficiency())).append(",\n");
        json.append("    \"healthStatus\": \"").append(healthStatus.name()).append("\"\n");
        json.append("  },\n");
        json.append("  \"geometry\": {\n");
        json.append("    \"totalVertices\": ").append(totalVertices).append(",\n");
        json.append("    \"totalIndices\": ").append(totalIndices).append(",\n");
        json.append("    \"totalInstances\": ").append(totalInstances).append("\n");
        json.append("  },\n");
        json.append("  \"timing\": {\n");
        json.append("    \"analysisNanos\": ").append(analysisTimeNanos).append(",\n");
        json.append("    \"sortNanos\": ").append(sortTimeNanos).append(",\n");
        json.append("    \"batchingNanos\": ").append(batchingTimeNanos).append(",\n");
        json.append("    \"executionNanos\": ").append(executionTimeNanos).append("\n");
        json.append("  },\n");
        json.append("  \"savingsPercentage\": ").append(String.format("%.2f", savingsPercentage)).append(",\n");
        json.append("  \"recommendationCount\": ").append(recommendations.size()).append("\n");
        json.append("}");

        String result = json.toString();
        cachedJsonRef = new SoftReference<>(result);
        return result;
    }

    /**
     * Human-readable formatted report.
     */
    public String toFormattedString() {
        StringBuilder sb = new StringBuilder(8192);

        sb.append("╔══════════════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                          DRAW POOL REPORT                                    ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ Frame: %-10d │ Health: %s %-10s                          ║\n", 
            frameNumber, healthStatus.icon(), healthStatus.name()));
        sb.append(String.format("║ Time:  %s                                   ║\n",
            LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault()).format(TIMESTAMP_FORMAT)));
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        
        sb.append("║ DRAW STATISTICS                                                              ║\n");
        sb.append(String.format("║   Total Submitted:    %,10d                                        ║\n", totalDrawCalls));
        sb.append(String.format("║   Actually Executed:  %,10d                                        ║\n", actualDrawCalls));
        sb.append(String.format("║   Batched:            %,10d                                        ║\n", batchedDrawCalls));
        sb.append(String.format("║   Duplicates Removed: %,10d                                        ║\n", duplicatesEliminated));
        sb.append(String.format("║   State Changes:      %,10d                                        ║\n", stateChanges));
        sb.append(String.format("║   Batch Efficiency:   %10.1f%%                                       ║\n", batchEfficiency()));
        
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ GEOMETRY                                                                     ║\n");
        sb.append(String.format("║   Vertices:  %,15d                                              ║\n", totalVertices));
        sb.append(String.format("║   Indices:   %,15d                                              ║\n", totalIndices));
        sb.append(String.format("║   Instances: %,15d                                              ║\n", totalInstances));

        if (!recommendations.isEmpty()) {
            sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
            sb.append("║ RECOMMENDATIONS                                                              ║\n");
            for (Recommendation rec : recommendations.stream().limit(5).toList()) {
                sb.append(String.format("║   %s [%s] %s\n", 
                    rec.severity().icon(), 
                    rec.category().name(),
                    truncate(rec.title(), 50)));
            }
        }

        sb.append("╚══════════════════════════════════════════════════════════════════════════════╝\n");

        return sb.toString();
    }

    private static String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }

    @Override
    public String toString() {
        return String.format(
            "DrawPoolReport[frame=%d, draws=%d→%d, efficiency=%.1f%%, health=%s]",
            frameNumber, totalDrawCalls, actualDrawCalls, batchEfficiency(), healthStatus.name()
        );
    }
}
