package stellar.snow.astralis.engine.ecs.util;

import stellar.snow.astralis.engine.ecs.core.World;

import java.util.concurrent.atomic.AtomicLong;

/**
 * WorldStatistics - Real-time ECS statistics and metrics.
 * 
 * <p>Tracks entities, components, systems, memory, and performance.
 * Updated automatically by the World during operations.
 * 
 * <h2>Usage</h2>
 * <pre>
 * WorldStatistics stats = world.getStatistics();
 * 
 * System.out.println("Entities: " + stats.getEntityCount());
 * System.out.println("Systems: " + stats.getSystemCount());
 * System.out.println("Memory: " + stats.getMemoryUsageMB() + " MB");
 * 
 * stats.printReport();
 * </pre>
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public class WorldStatistics {
    
    // Entity statistics
    private final AtomicLong entitiesCreated = new AtomicLong(0);
    private final AtomicLong entitiesDestroyed = new AtomicLong(0);
    private final AtomicLong entitiesAlive = new AtomicLong(0);
    private final AtomicLong peakEntityCount = new AtomicLong(0);
    
    // Component statistics
    private final AtomicLong componentsAdded = new AtomicLong(0);
    private final AtomicLong componentsRemoved = new AtomicLong(0);
    private final AtomicLong componentTypes = new AtomicLong(0);
    
    // System statistics
    private final AtomicLong systemCount = new AtomicLong(0);
    private final AtomicLong systemUpdates = new AtomicLong(0);
    
    // Archetype statistics
    private final AtomicLong archetypeCount = new AtomicLong(0);
    private final AtomicLong archetypeTransitions = new AtomicLong(0);
    
    // Performance statistics
    private final AtomicLong framesProcessed = new AtomicLong(0);
    private volatile double averageFrameTimeMs = 0;
    private volatile double peakFrameTimeMs = 0;
    private volatile long lastFrameTimestamp = 0;
    
    // Memory statistics
    private final AtomicLong memoryAllocated = new AtomicLong(0);
    private final AtomicLong memoryFreed = new AtomicLong(0);
    
    /**
     * Create statistics tracker.
     */
    public WorldStatistics() {
        reset();
    }
    
    // ========================================================================
    // ENTITY TRACKING
    // ========================================================================
    
    public void recordEntityCreated() {
        long count = entitiesCreated.incrementAndGet();
        long alive = entitiesAlive.incrementAndGet();
        
        // Update peak
        long peak = peakEntityCount.get();
        while (alive > peak) {
            if (peakEntityCount.compareAndSet(peak, alive)) {
                break;
            }
            peak = peakEntityCount.get();
        }
    }
    
    public void recordEntityDestroyed() {
        entitiesDestroyed.incrementAndGet();
        entitiesAlive.decrementAndGet();
    }
    
    public void setEntityCount(long count) {
        entitiesAlive.set(count);
    }
    
    // ========================================================================
    // COMPONENT TRACKING
    // ========================================================================
    
    public void recordComponentAdded() {
        componentsAdded.incrementAndGet();
    }
    
    public void recordComponentRemoved() {
        componentsRemoved.incrementAndGet();
    }
    
    public void setComponentTypeCount(long count) {
        componentTypes.set(count);
    }
    
    // ========================================================================
    // SYSTEM TRACKING
    // ========================================================================
    
    public void setSystemCount(long count) {
        systemCount.set(count);
    }
    
    public void recordSystemUpdate() {
        systemUpdates.incrementAndGet();
    }
    
    // ========================================================================
    // ARCHETYPE TRACKING
    // ========================================================================
    
    public void setArchetypeCount(long count) {
        archetypeCount.set(count);
    }
    
    public void recordArchetypeTransition() {
        archetypeTransitions.incrementAndGet();
    }
    
    // ========================================================================
    // PERFORMANCE TRACKING
    // ========================================================================
    
    public void recordFrame(double frameTimeMs) {
        framesProcessed.incrementAndGet();
        
        // Update average (exponential moving average)
        double alpha = 0.1; // Smoothing factor
        averageFrameTimeMs = alpha * frameTimeMs + (1 - alpha) * averageFrameTimeMs;
        
        // Update peak
        if (frameTimeMs > peakFrameTimeMs) {
            peakFrameTimeMs = frameTimeMs;
        }
        
        lastFrameTimestamp = System.nanoTime();
    }
    
    // ========================================================================
    // MEMORY TRACKING
    // ========================================================================
    
    public void recordMemoryAllocated(long bytes) {
        memoryAllocated.addAndGet(bytes);
    }
    
    public void recordMemoryFreed(long bytes) {
        memoryFreed.addAndGet(bytes);
    }
    
    // ========================================================================
    // GETTERS
    // ========================================================================
    
    public long getEntitiesCreated() { return entitiesCreated.get(); }
    public long getEntitiesDestroyed() { return entitiesDestroyed.get(); }
    public long getEntityCount() { return entitiesAlive.get(); }
    public long getPeakEntityCount() { return peakEntityCount.get(); }
    
    public long getComponentsAdded() { return componentsAdded.get(); }
    public long getComponentsRemoved() { return componentsRemoved.get(); }
    public long getComponentTypeCount() { return componentTypes.get(); }
    
    public long getSystemCount() { return systemCount.get(); }
    public long getSystemUpdates() { return systemUpdates.get(); }
    
    public long getArchetypeCount() { return archetypeCount.get(); }
    public long getArchetypeTransitions() { return archetypeTransitions.get(); }
    
    public long getFramesProcessed() { return framesProcessed.get(); }
    public double getAverageFrameTimeMs() { return averageFrameTimeMs; }
    public double getPeakFrameTimeMs() { return peakFrameTimeMs; }
    public double getAverageFPS() { 
        return averageFrameTimeMs > 0 ? 1000.0 / averageFrameTimeMs : 0; 
    }
    
    public long getMemoryAllocated() { return memoryAllocated.get(); }
    public long getMemoryFreed() { return memoryFreed.get(); }
    public long getMemoryUsageBytes() { return memoryAllocated.get() - memoryFreed.get(); }
    public double getMemoryUsageMB() { return getMemoryUsageBytes() / (1024.0 * 1024.0); }
    
    // ========================================================================
    // UTILITIES
    // ========================================================================
    
    /**
     * Reset all statistics.
     */
    public void reset() {
        entitiesCreated.set(0);
        entitiesDestroyed.set(0);
        entitiesAlive.set(0);
        peakEntityCount.set(0);
        
        componentsAdded.set(0);
        componentsRemoved.set(0);
        componentTypes.set(0);
        
        systemCount.set(0);
        systemUpdates.set(0);
        
        archetypeCount.set(0);
        archetypeTransitions.set(0);
        
        framesProcessed.set(0);
        averageFrameTimeMs = 0;
        peakFrameTimeMs = 0;
        
        memoryAllocated.set(0);
        memoryFreed.set(0);
    }
    
    /**
     * Print detailed statistics report.
     */
    public void printReport() {
        System.out.println("=".repeat(80));
        System.out.println("ECS World Statistics");
        System.out.println("=".repeat(80));
        
        System.out.println("\nEntities:");
        System.out.println("  Alive:     " + getEntityCount());
        System.out.println("  Created:   " + getEntitiesCreated());
        System.out.println("  Destroyed: " + getEntitiesDestroyed());
        System.out.println("  Peak:      " + getPeakEntityCount());
        
        System.out.println("\nComponents:");
        System.out.println("  Types:   " + getComponentTypeCount());
        System.out.println("  Added:   " + getComponentsAdded());
        System.out.println("  Removed: " + getComponentsRemoved());
        
        System.out.println("\nSystems:");
        System.out.println("  Count:   " + getSystemCount());
        System.out.println("  Updates: " + getSystemUpdates());
        
        System.out.println("\nArchetypes:");
        System.out.println("  Count:       " + getArchetypeCount());
        System.out.println("  Transitions: " + getArchetypeTransitions());
        
        System.out.println("\nPerformance:");
        System.out.println(String.format("  Frames:          %d", getFramesProcessed()));
        System.out.println(String.format("  Avg Frame Time:  %.2f ms", getAverageFrameTimeMs()));
        System.out.println(String.format("  Peak Frame Time: %.2f ms", getPeakFrameTimeMs()));
        System.out.println(String.format("  Avg FPS:         %.1f", getAverageFPS()));
        
        System.out.println("\nMemory:");
        System.out.println(String.format("  Allocated: %.2f MB", getMemoryAllocated() / (1024.0 * 1024.0)));
        System.out.println(String.format("  Freed:     %.2f MB", getMemoryFreed() / (1024.0 * 1024.0)));
        System.out.println(String.format("  Current:   %.2f MB", getMemoryUsageMB()));
        
        System.out.println("=".repeat(80));
    }
}
