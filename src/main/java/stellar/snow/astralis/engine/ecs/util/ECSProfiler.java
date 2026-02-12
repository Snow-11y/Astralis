package stellar.snow.astralis.engine.ecs.util;

import stellar.snow.astralis.engine.ecs.core.SnowySystem;
import stellar.snow.astralis.engine.ecs.core.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ECSProfiler - Performance monitoring and profiling for ECS.
 * 
 * <p>Tracks system execution times, entity counts, component usage,
 * and memory allocation. Provides insights into ECS performance.
 * 
 * <h2>Usage</h2>
 * <pre>
 * ECSProfiler profiler = new ECSProfiler();
 * 
 * // Profile a frame
 * profiler.beginFrame();
 * profiler.beginSystem("PhysicsSystem");
 * // ... system execution ...
 * profiler.endSystem("PhysicsSystem");
 * profiler.endFrame();
 * 
 * // Get statistics
 * ECSProfiler.FrameStats stats = profiler.getLastFrameStats();
 * System.out.println("Frame time: " + stats.totalTimeMs() + "ms");
 * </pre>
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public class ECSProfiler {
    
    private final Map<String, SystemStats> systemStats;
    private final Map<String, Long> systemStartTimes;
    private final List<FrameStats> frameHistory;
    private final int maxFrameHistory;
    
    private long frameStartTime;
    private long frameNumber;
    private boolean inFrame;
    
    /**
     * Create profiler with default history size (300 frames = 5 seconds at 60fps).
     */
    public ECSProfiler() {
        this(300);
    }
    
    /**
     * Create profiler with custom history size.
     */
    public ECSProfiler(int maxFrameHistory) {
        this.systemStats = new ConcurrentHashMap<>();
        this.systemStartTimes = new ConcurrentHashMap<>();
        this.frameHistory = new ArrayList<>(maxFrameHistory);
        this.maxFrameHistory = maxFrameHistory;
        this.frameNumber = 0;
        this.inFrame = false;
    }
    
    // ========================================================================
    // FRAME PROFILING
    // ========================================================================
    
    /**
     * Begin profiling a frame.
     */
    public void beginFrame() {
        frameStartTime = System.nanoTime();
        inFrame = true;
        systemStartTimes.clear();
    }
    
    /**
     * End profiling a frame.
     */
    public void endFrame() {
        if (!inFrame) {
            return;
        }
        
        long frameEndTime = System.nanoTime();
        long frameDuration = frameEndTime - frameStartTime;
        
        // Create frame stats
        FrameStats stats = new FrameStats(
            frameNumber++,
            frameDuration / 1_000_000.0, // Convert to milliseconds
            new HashMap<>(systemStats)
        );
        
        // Add to history
        frameHistory.add(stats);
        if (frameHistory.size() > maxFrameHistory) {
            frameHistory.remove(0);
        }
        
        inFrame = false;
    }
    
    // ========================================================================
    // SYSTEM PROFILING
    // ========================================================================
    
    /**
     * Begin profiling a system.
     */
    public void beginSystem(String systemName) {
        systemStartTimes.put(systemName, System.nanoTime());
    }
    
    /**
     * End profiling a system.
     */
    public void endSystem(String systemName) {
        Long startTime = systemStartTimes.remove(systemName);
        if (startTime == null) {
            return;
        }
        
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        
        // Update stats
        systemStats.computeIfAbsent(systemName, k -> new SystemStats(systemName))
            .record(duration);
    }
    
    /**
     * Record entity count for a system.
     */
    public void recordEntityCount(String systemName, int count) {
        systemStats.computeIfAbsent(systemName, k -> new SystemStats(systemName))
            .recordEntityCount(count);
    }
    
    // ========================================================================
    // STATISTICS
    // ========================================================================
    
    /**
     * Get stats for the last completed frame.
     */
    public FrameStats getLastFrameStats() {
        return frameHistory.isEmpty() ? null : frameHistory.get(frameHistory.size() - 1);
    }
    
    /**
     * Get average frame time over history.
     */
    public double getAverageFrameTimeMs() {
        if (frameHistory.isEmpty()) {
            return 0;
        }
        return frameHistory.stream()
            .mapToDouble(FrameStats::totalTimeMs)
            .average()
            .orElse(0);
    }
    
    /**
     * Get average FPS over history.
     */
    public double getAverageFPS() {
        double avgFrameTime = getAverageFrameTimeMs();
        return avgFrameTime > 0 ? 1000.0 / avgFrameTime : 0;
    }
    
    /**
     * Get stats for a specific system.
     */
    public SystemStats getSystemStats(String systemName) {
        return systemStats.get(systemName);
    }
    
    /**
     * Get all system stats.
     */
    public Map<String, SystemStats> getAllSystemStats() {
        return Collections.unmodifiableMap(systemStats);
    }
    
    /**
     * Get frame history.
     */
    public List<FrameStats> getFrameHistory() {
        return Collections.unmodifiableList(frameHistory);
    }
    
    /**
     * Reset all statistics.
     */
    public void reset() {
        systemStats.clear();
        systemStartTimes.clear();
        frameHistory.clear();
        frameNumber = 0;
        inFrame = false;
    }
    
    /**
     * Print performance report.
     */
    public void printReport() {
        System.out.println("=".repeat(80));
        System.out.println("ECS Performance Report");
        System.out.println("=".repeat(80));
        System.out.println(String.format("Frames: %d", frameNumber));
        System.out.println(String.format("Avg Frame Time: %.2f ms", getAverageFrameTimeMs()));
        System.out.println(String.format("Avg FPS: %.1f", getAverageFPS()));
        System.out.println();
        System.out.println("System Statistics:");
        System.out.println("-".repeat(80));
        
        systemStats.values().stream()
            .sorted((a, b) -> Double.compare(b.getAverageTimeMs(), a.getAverageTimeMs()))
            .forEach(stats -> {
                System.out.println(String.format("  %-30s | Avg: %6.2f ms | Calls: %8d | Entities: %6d",
                    stats.name(),
                    stats.getAverageTimeMs(),
                    stats.callCount(),
                    stats.lastEntityCount()
                ));
            });
        
        System.out.println("=".repeat(80));
    }
    
    // ========================================================================
    // STATS RECORDS
    // ========================================================================
    
    /**
     * Frame statistics.
     */
    public record FrameStats(
        long frameNumber,
        double totalTimeMs,
        Map<String, SystemStats> systemStats
    ) {}
    
    /**
     * System statistics.
     */
    public static class SystemStats {
        private final String name;
        private final AtomicLong totalTimeNs;
        private final AtomicLong callCount;
        private volatile long lastExecutionNs;
        private volatile int lastEntityCount;
        
        public SystemStats(String name) {
            this.name = name;
            this.totalTimeNs = new AtomicLong(0);
            this.callCount = new AtomicLong(0);
            this.lastExecutionNs = 0;
            this.lastEntityCount = 0;
        }
        
        void record(long durationNs) {
            totalTimeNs.addAndGet(durationNs);
            callCount.incrementAndGet();
            lastExecutionNs = durationNs;
        }
        
        void recordEntityCount(int count) {
            lastEntityCount = count;
        }
        
        public String name() {
            return name;
        }
        
        public long totalTimeNs() {
            return totalTimeNs.get();
        }
        
        public long callCount() {
            return callCount.get();
        }
        
        public double getAverageTimeMs() {
            long count = callCount.get();
            return count > 0 ? (totalTimeNs.get() / count) / 1_000_000.0 : 0;
        }
        
        public double getLastTimeMs() {
            return lastExecutionNs / 1_000_000.0;
        }
        
        public int lastEntityCount() {
            return lastEntityCount;
        }
    }
}
