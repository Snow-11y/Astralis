package stellar.snow.astralis.engine.render.meshlet;

import stellar.snow.astralis.engine.gpu.authority.GPUBackend;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * MeshletProfiler - Comprehensive performance profiling and analysis.
 * 
 * <p>Tracks:</p>
 * <ul>
 *   <li>Per-frame timing (CPU and GPU)</li>
 *   <li>Culling efficiency by type</li>
 *   <li>Memory usage and bandwidth</li>
 *   <li>LOD transition quality</li>
 *   <li>Bottleneck identification</li>
 *   <li>Historical trends and statistics</li>
 * </ul>
 */
public final class MeshletProfiler {
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════
    
    public static final class Config {
        /** Enable GPU timestamp queries */
        public boolean enableGPUTimestamps = true;
        
        /** Enable CPU profiling */
        public boolean enableCPUProfiling = true;
        
        /** Enable memory tracking */
        public boolean enableMemoryTracking = true;
        
        /** History frame count */
        public int historyFrames = 300; // 5 sec @ 60fps
        
        /** Enable bottleneck detection */
        public boolean enableBottleneckDetection = true;
        
        /** Percentile calculations */
        public float[] percentiles = {50, 90, 95, 99};
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // FRAME PROFILE
    // ═══════════════════════════════════════════════════════════════════════
    
    public static final class FrameProfile {
        public final int frameIndex;
        public final long timestamp;
        
        // CPU timing (nanoseconds)
        public long cpuTotal;
        public long cpuStreaming;
        public long cpuLODSelection;
        public long cpuOcclusionCulling;
        public long cpuVRS;
        public long cpuSubmission;
        
        // GPU timing (nanoseconds)
        public long gpuTotal;
        public long gpuTaskShader;
        public long gpuMeshShader;
        public long gpuFragment;
        
        // Culling stats
        public int totalMeshlets;
        public int visibleMeshlets;
        public int culledByFrustum;
        public int culledByCone;
        public int culledByOcclusion;
        public int culledByLOD;
        
        // Memory
        public long gpuMemoryUsed;
        public long cpuMemoryUsed;
        public long bandwidthUsed;
        
        // LOD stats
        public int[] meshletsPerLOD;
        public float avgLODLevel;
        public int lodTransitions;
        
        // VRS stats
        public float vrsReduction;
        
        public FrameProfile(int frameIndex) {
            this.frameIndex = frameIndex;
            this.timestamp = System.nanoTime();
        }
        
        public float getCPUTimeMs() {
            return cpuTotal / 1_000_000.0f;
        }
        
        public float getGPUTimeMs() {
            return gpuTotal / 1_000_000.0f;
        }
        
        public float getCullingEfficiency() {
            return totalMeshlets > 0 
                ? 1.0f - (float) visibleMeshlets / totalMeshlets
                : 0.0f;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // AGGREGATED STATISTICS
    // ═══════════════════════════════════════════════════════════════════════
    
    public static final class AggregatedStats {
        // Timing statistics
        public TimingStats cpuTiming = new TimingStats();
        public TimingStats gpuTiming = new TimingStats();
        
        // Culling statistics
        public float avgCullingEfficiency;
        public long totalMeshletsProcessed;
        public long totalMeshletsRendered;
        
        // Memory statistics
        public long peakGPUMemory;
        public long avgGPUMemory;
        public long peakBandwidth;
        public long avgBandwidth;
        
        // LOD statistics
        public float avgLODLevel;
        public int[] lodDistribution;
        public int totalLODTransitions;
        
        // Bottlenecks
        public String primaryBottleneck;
        public float bottleneckPercentage;
        
        // Frame statistics
        public int totalFrames;
        public float avgFPS;
        public float minFPS;
        public float maxFPS;
    }
    
    public static final class TimingStats {
        public float min;
        public float max;
        public float avg;
        public float median;
        public float p90;
        public float p95;
        public float p99;
        public float stdDev;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════
    
    private final Config config;
    private final GPUBackend backend;
    
    private final Queue<FrameProfile> history;
    private FrameProfile currentFrame;
    private final AtomicInteger frameCounter;
    
    // Timing zones
    private final Map<String, Long> zoneStarts;
    private final Map<String, AtomicLong> zoneTotals;
    
    // GPU queries
    private final Queue<Long> availableQueries;
    private final Map<String, Long> activeQueries;
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════
    
    public MeshletProfiler(GPUBackend backend, Config config) {
        this.backend = backend;
        this.config = config;
        this.history = new ConcurrentLinkedQueue<>();
        this.frameCounter = new AtomicInteger(0);
        this.zoneStarts = new ConcurrentHashMap<>();
        this.zoneTotals = new ConcurrentHashMap<>();
        this.availableQueries = new ConcurrentLinkedQueue<>();
        this.activeQueries = new ConcurrentHashMap<>();
        
        // Pre-allocate GPU queries
        if (config.enableGPUTimestamps) {
            for (int i = 0; i < 32; i++) {
                availableQueries.offer(backend.createTimestampQuery());
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // FRAME TRACKING
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Begins a new frame profile.
     */
    public void beginFrame() {
        int frame = frameCounter.getAndIncrement();
        currentFrame = new FrameProfile(frame);
        
        if (config.enableCPUProfiling) {
            beginZone("frame_total");
        }
    }
    
    /**
     * Ends current frame and stores profile.
     */
    public void endFrame() {
        if (currentFrame == null) return;
        
        if (config.enableCPUProfiling) {
            currentFrame.cpuTotal = endZone("frame_total");
        }
        
        // Add to history
        history.offer(currentFrame);
        
        // Limit history size
        while (history.size() > config.historyFrames) {
            history.poll();
        }
        
        currentFrame = null;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CPU PROFILING
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Begins a CPU timing zone.
     */
    public void beginZone(String name) {
        if (!config.enableCPUProfiling) return;
        zoneStarts.put(name, System.nanoTime());
    }
    
    /**
     * Ends a CPU timing zone and returns elapsed nanoseconds.
     */
    public long endZone(String name) {
        if (!config.enableCPUProfiling) return 0;
        
        Long start = zoneStarts.remove(name);
        if (start == null) return 0;
        
        long elapsed = System.nanoTime() - start;
        zoneTotals.computeIfAbsent(name, k -> new AtomicLong()).addAndGet(elapsed);
        
        return elapsed;
    }
    
    /**
     * Records a zone time directly.
     */
    public void recordZone(String name, long nanos) {
        if (currentFrame == null) return;
        
        switch (name) {
            case "streaming" -> currentFrame.cpuStreaming = nanos;
            case "lod_selection" -> currentFrame.cpuLODSelection = nanos;
            case "occlusion_culling" -> currentFrame.cpuOcclusionCulling = nanos;
            case "vrs" -> currentFrame.cpuVRS = nanos;
            case "submission" -> currentFrame.cpuSubmission = nanos;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // GPU PROFILING
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Begins GPU timestamp query.
     */
    public void beginGPUZone(String name) {
        if (!config.enableGPUTimestamps) return;
        
        Long query = availableQueries.poll();
        if (query != null) {
            backend.writeTimestamp(query);
            activeQueries.put(name, query);
        }
    }
    
    /**
     * Ends GPU timestamp query.
     */
    public void endGPUZone(String name) {
        if (!config.enableGPUTimestamps) return;
        
        Long startQuery = activeQueries.remove(name);
        if (startQuery == null) return;
        
        Long endQuery = availableQueries.poll();
        if (endQuery != null) {
            backend.writeTimestamp(endQuery);
            
            // Read results asynchronously
            CompletableFuture.runAsync(() -> {
                long start = backend.getTimestampResult(startQuery);
                long end = backend.getTimestampResult(endQuery);
                long elapsed = end - start;
                
                if (currentFrame != null) {
                    recordGPUZone(name, elapsed);
                }
                
                // Return queries to pool
                availableQueries.offer(startQuery);
                availableQueries.offer(endQuery);
            });
        }
    }
    
    private void recordGPUZone(String name, long nanos) {
        if (currentFrame == null) return;
        
        switch (name) {
            case "task_shader" -> currentFrame.gpuTaskShader = nanos;
            case "mesh_shader" -> currentFrame.gpuMeshShader = nanos;
            case "fragment" -> currentFrame.gpuFragment = nanos;
        }
        
        currentFrame.gpuTotal += nanos;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // STATISTICS RECORDING
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Records culling statistics for current frame.
     */
    public void recordCullingStats(
        int total,
        int visible,
        int frustum,
        int cone,
        int occlusion,
        int lod
    ) {
        if (currentFrame == null) return;
        
        currentFrame.totalMeshlets = total;
        currentFrame.visibleMeshlets = visible;
        currentFrame.culledByFrustum = frustum;
        currentFrame.culledByCone = cone;
        currentFrame.culledByOcclusion = occlusion;
        currentFrame.culledByLOD = lod;
    }
    
    /**
     * Records memory usage for current frame.
     */
    public void recordMemoryUsage(long gpuBytes, long cpuBytes, long bandwidthBytes) {
        if (currentFrame == null) return;
        
        currentFrame.gpuMemoryUsed = gpuBytes;
        currentFrame.cpuMemoryUsed = cpuBytes;
        currentFrame.bandwidthUsed = bandwidthBytes;
    }
    
    /**
     * Records LOD statistics for current frame.
     */
    public void recordLODStats(int[] meshletsPerLOD, int transitions) {
        if (currentFrame == null) return;
        
        currentFrame.meshletsPerLOD = meshletsPerLOD.clone();
        currentFrame.lodTransitions = transitions;
        
        // Compute average LOD
        int total = 0;
        int weighted = 0;
        for (int lod = 0; lod < meshletsPerLOD.length; lod++) {
            total += meshletsPerLOD[lod];
            weighted += meshletsPerLOD[lod] * lod;
        }
        currentFrame.avgLODLevel = total > 0 ? (float) weighted / total : 0;
    }
    
    /**
     * Records VRS statistics for current frame.
     */
    public void recordVRSStats(float reduction) {
        if (currentFrame == null) return;
        currentFrame.vrsReduction = reduction;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // ANALYSIS
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Computes aggregated statistics from history.
     */
    public AggregatedStats analyze() {
        AggregatedStats stats = new AggregatedStats();
        
        if (history.isEmpty()) {
            return stats;
        }
        
        List<FrameProfile> frames = new ArrayList<>(history);
        stats.totalFrames = frames.size();
        
        // Timing statistics
        stats.cpuTiming = analyzeTimings(frames, f -> f.getCPUTimeMs());
        stats.gpuTiming = analyzeTimings(frames, f -> f.getGPUTimeMs());
        
        // FPS statistics
        float[] frameTimes = frames.stream()
            .mapToDouble(FrameProfile::getCPUTimeMs)
            .toArray();
        stats.avgFPS = 1000.0f / stats.cpuTiming.avg;
        stats.minFPS = 1000.0f / stats.cpuTiming.max;
        stats.maxFPS = 1000.0f / stats.cpuTiming.min;
        
        // Culling statistics
        stats.totalMeshletsProcessed = frames.stream()
            .mapToLong(f -> f.totalMeshlets)
            .sum();
        stats.totalMeshletsRendered = frames.stream()
            .mapToLong(f -> f.visibleMeshlets)
            .sum();
        stats.avgCullingEfficiency = frames.stream()
            .mapToDouble(FrameProfile::getCullingEfficiency)
            .average()
            .orElse(0.0);
        
        // Memory statistics
        stats.peakGPUMemory = frames.stream()
            .mapToLong(f -> f.gpuMemoryUsed)
            .max()
            .orElse(0);
        stats.avgGPUMemory = (long) frames.stream()
            .mapToLong(f -> f.gpuMemoryUsed)
            .average()
            .orElse(0);
        stats.peakBandwidth = frames.stream()
            .mapToLong(f -> f.bandwidthUsed)
            .max()
            .orElse(0);
        stats.avgBandwidth = (long) frames.stream()
            .mapToLong(f -> f.bandwidthUsed)
            .average()
            .orElse(0);
        
        // LOD statistics
        stats.avgLODLevel = (float) frames.stream()
            .mapToDouble(f -> f.avgLODLevel)
            .average()
            .orElse(0.0);
        stats.totalLODTransitions = frames.stream()
            .mapToInt(f -> f.lodTransitions)
            .sum();
        
        // Bottleneck detection
        if (config.enableBottleneckDetection) {
            detectBottleneck(stats, frames);
        }
        
        return stats;
    }
    
    private TimingStats analyzeTimings(
        List<FrameProfile> frames,
        java.util.function.ToDoubleFunction<FrameProfile> extractor
    ) {
        TimingStats stats = new TimingStats();
        
        double[] values = frames.stream()
            .mapToDouble(extractor)
            .sorted()
            .toArray();
        
        if (values.length == 0) {
            return stats;
        }
        
        stats.min = (float) values[0];
        stats.max = (float) values[values.length - 1];
        stats.avg = (float) Arrays.stream(values).average().orElse(0);
        stats.median = (float) percentile(values, 50);
        stats.p90 = (float) percentile(values, 90);
        stats.p95 = (float) percentile(values, 95);
        stats.p99 = (float) percentile(values, 99);
        
        // Standard deviation
        double variance = Arrays.stream(values)
            .map(v -> Math.pow(v - stats.avg, 2))
            .average()
            .orElse(0);
        stats.stdDev = (float) Math.sqrt(variance);
        
        return stats;
    }
    
    private double percentile(double[] sorted, float p) {
        int index = (int) Math.ceil(sorted.length * p / 100.0) - 1;
        index = Math.clamp(index, 0, sorted.length - 1);
        return sorted[index];
    }
    
    private void detectBottleneck(AggregatedStats stats, List<FrameProfile> frames) {
        // Compute average time for each stage
        double avgTask = frames.stream()
            .mapToLong(f -> f.gpuTaskShader)
            .average()
            .orElse(0) / 1_000_000.0;
        
        double avgMesh = frames.stream()
            .mapToLong(f -> f.gpuMeshShader)
            .average()
            .orElse(0) / 1_000_000.0;
        
        double avgFragment = frames.stream()
            .mapToLong(f -> f.gpuFragment)
            .average()
            .orElse(0) / 1_000_000.0;
        
        double avgStreaming = frames.stream()
            .mapToLong(f -> f.cpuStreaming)
            .average()
            .orElse(0) / 1_000_000.0;
        
        // Find maximum
        double max = Math.max(Math.max(avgTask, avgMesh), 
                             Math.max(avgFragment, avgStreaming));
        
        if (max == avgTask) {
            stats.primaryBottleneck = "Task Shader (GPU Culling)";
            stats.bottleneckPercentage = (float) (avgTask / stats.gpuTiming.avg);
        } else if (max == avgMesh) {
            stats.primaryBottleneck = "Mesh Shader (Vertex Processing)";
            stats.bottleneckPercentage = (float) (avgMesh / stats.gpuTiming.avg);
        } else if (max == avgFragment) {
            stats.primaryBottleneck = "Fragment Shader (Pixel Processing)";
            stats.bottleneckPercentage = (float) (avgFragment / stats.gpuTiming.avg);
        } else {
            stats.primaryBottleneck = "CPU Streaming";
            stats.bottleneckPercentage = (float) (avgStreaming / stats.cpuTiming.avg);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // REPORTING
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Generates formatted performance report.
     */
    public String generateReport() {
        AggregatedStats stats = analyze();
        
        return String.format("""
            ╔════════════════════════════════════════════════════════════════╗
            ║            MESHLET RENDERING PERFORMANCE REPORT                ║
            ╠════════════════════════════════════════════════════════════════╣
            ║ Frames Analyzed: %-45d ║
            ║ FPS: %.1f avg (%.1f min, %.1f max)%26s║
            ╠════════════════════════════════════════════════════════════════╣
            ║ CPU Timing (ms)                                                ║
            ║   Average:  %6.2f  │ Min: %6.2f  │ Max: %6.2f            ║
            ║   Median:   %6.2f  │ P90: %6.2f  │ P99: %6.2f            ║
            ╠════════════════════════════════════════════════════════════════╣
            ║ GPU Timing (ms)                                                ║
            ║   Average:  %6.2f  │ Min: %6.2f  │ Max: %6.2f            ║
            ║   Median:   %6.2f  │ P90: %6.2f  │ P99: %6.2f            ║
            ╠════════════════════════════════════════════════════════════════╣
            ║ Culling Efficiency                                             ║
            ║   Total Processed:  %,15d meshlets                    ║
            ║   Total Rendered:   %,15d meshlets                    ║
            ║   Avg Efficiency:   %6.1f%% culled%27s║
            ╠════════════════════════════════════════════════════════════════╣
            ║ Memory Usage                                                   ║
            ║   GPU Peak:  %,12d MB │ GPU Avg: %,12d MB         ║
            ║   Bandwidth: %,12d MB/s                              ║
            ╠════════════════════════════════════════════════════════════════╣
            ║ Bottleneck Analysis                                            ║
            ║   Primary:  %-50s  ║
            ║   Impact:   %6.1f%% of frame time%27s║
            ╚════════════════════════════════════════════════════════════════╝
            """,
            stats.totalFrames,
            stats.avgFPS, stats.minFPS, stats.maxFPS, "",
            stats.cpuTiming.avg, stats.cpuTiming.min, stats.cpuTiming.max,
            stats.cpuTiming.median, stats.cpuTiming.p90, stats.cpuTiming.p99,
            stats.gpuTiming.avg, stats.gpuTiming.min, stats.gpuTiming.max,
            stats.gpuTiming.median, stats.gpuTiming.p90, stats.gpuTiming.p99,
            stats.totalMeshletsProcessed,
            stats.totalMeshletsRendered,
            stats.avgCullingEfficiency * 100, "",
            stats.peakGPUMemory / (1024 * 1024), stats.avgGPUMemory / (1024 * 1024),
            stats.avgBandwidth / (1024 * 1024),
            stats.primaryBottleneck,
            stats.bottleneckPercentage * 100, ""
        );
    }
    
    public void shutdown() {
        // Free GPU queries
        for (Long query : availableQueries) {
            backend.destroyTimestampQuery(query);
        }
    }
}
