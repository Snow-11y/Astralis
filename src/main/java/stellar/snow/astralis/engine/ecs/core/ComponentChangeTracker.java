package ecs.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Fine-grained component change tracking for reactive patterns.
 * 
 * Features:
 * - Per-component-type dirty flags
 * - Generation-based versioning for optimistic reads
 * - Lock-free dirty set updates using atomics
 * - Batch dirty range queries
 * - Change callbacks with filtering
 * - Memory-efficient sparse tracking (only tracks changed entities)
 * - Minimal overhead (atomic operations only)
 */
public class ComponentChangeTracker {
    
    /**
     * Type of change that occurred
     */
    public enum ChangeType {
        ADDED,      // Component was added to entity
        MODIFIED,   // Component was modified
        REMOVED     // Component was removed from entity
    }
    
    /**
     * Represents a single component change
     */
    public static class ComponentChange {
        public final int entityId;
        public final Class<?> componentType;
        public final ChangeType changeType;
        public final long generation;
        public final long timestamp;
        
        public ComponentChange(int entityId, Class<?> componentType, 
                             ChangeType changeType, long generation) {
            this.entityId = entityId;
            this.componentType = componentType;
            this.changeType = changeType;
            this.generation = generation;
            this.timestamp = System.nanoTime();
        }
        
        @Override
        public String toString() {
            return String.format("Change[entity=%d, type=%s, change=%s, gen=%d]",
                entityId, componentType.getSimpleName(), changeType, generation);
        }
    }
    
    /**
     * Tracks changes for a specific component type
     */
    private static class ComponentTypeTracker {
        final Class<?> componentType;
        final Set<Integer> dirtyEntities;  // Thread-safe set
        final AtomicLong generation;
        final Map<Integer, Long> entityGenerations;
        final List<Consumer<ComponentChange>> callbacks;
        
        ComponentTypeTracker(Class<?> componentType) {
            this.componentType = componentType;
            this.dirtyEntities = ConcurrentHashMap.newKeySet();
            this.generation = new AtomicLong(0);
            this.entityGenerations = new ConcurrentHashMap<>();
            this.callbacks = Collections.synchronizedList(new ArrayList<>());
        }
        
        void markDirty(int entityId, ChangeType changeType) {
            long gen = generation.incrementAndGet();
            dirtyEntities.add(entityId);
            entityGenerations.put(entityId, gen);
            
            // Notify callbacks
            if (!callbacks.isEmpty()) {
                ComponentChange change = new ComponentChange(
                    entityId, componentType, changeType, gen);
                synchronized (callbacks) {
                    for (Consumer<ComponentChange> callback : callbacks) {
                        callback.accept(change);
                    }
                }
            }
        }
        
        void clearDirty(int entityId) {
            dirtyEntities.remove(entityId);
        }
        
        void clearAll() {
            dirtyEntities.clear();
        }
        
        boolean isDirty(int entityId) {
            return dirtyEntities.contains(entityId);
        }
        
        long getGeneration(int entityId) {
            return entityGenerations.getOrDefault(entityId, 0L);
        }
    }
    
    private final int maxEntities;
    private final Map<Class<?>, ComponentTypeTracker> trackers;
    private final AtomicInteger totalChanges;
    private final List<ComponentChange> changeHistory;
    private final int historySize;
    
    public ComponentChangeTracker(int maxEntities) {
        this(maxEntities, 1000);
    }
    
    public ComponentChangeTracker(int maxEntities, int historySize) {
        this.maxEntities = maxEntities;
        this.trackers = new ConcurrentHashMap<>();
        this.totalChanges = new AtomicInteger(0);
        this.changeHistory = Collections.synchronizedList(new ArrayList<>());
        this.historySize = historySize;
    }
    
    /**
     * Mark a component as dirty
     */
    public void markDirty(int entityId, Class<?> componentType, ChangeType changeType) {
        if (entityId < 0 || entityId >= maxEntities) {
            throw new IllegalArgumentException("Invalid entity ID: " + entityId);
        }
        
        ComponentTypeTracker tracker = trackers.computeIfAbsent(
            componentType, ComponentTypeTracker::new);
        
        tracker.markDirty(entityId, changeType);
        totalChanges.incrementAndGet();
        
        // Add to history
        synchronized (changeHistory) {
            changeHistory.add(new ComponentChange(
                entityId, componentType, changeType, tracker.generation.get()));
            
            // Trim history if needed
            while (changeHistory.size() > historySize) {
                changeHistory.remove(0);
            }
        }
    }
    
    /**
     * Mark multiple entities as dirty
     */
    public void markDirtyBatch(int[] entityIds, Class<?> componentType, ChangeType changeType) {
        ComponentTypeTracker tracker = trackers.computeIfAbsent(
            componentType, ComponentTypeTracker::new);
        
        for (int entityId : entityIds) {
            if (entityId >= 0 && entityId < maxEntities) {
                tracker.markDirty(entityId, changeType);
            }
        }
        
        totalChanges.addAndGet(entityIds.length);
    }
    
    /**
     * Clear dirty flag for an entity
     */
    public void clearDirty(int entityId, Class<?> componentType) {
        ComponentTypeTracker tracker = trackers.get(componentType);
        if (tracker != null) {
            tracker.clearDirty(entityId);
        }
    }
    
    /**
     * Clear all dirty flags for a component type
     */
    public void clearAllDirty(Class<?> componentType) {
        ComponentTypeTracker tracker = trackers.get(componentType);
        if (tracker != null) {
            tracker.clearAll();
        }
    }
    
    /**
     * Clear all dirty flags for all component types
     */
    public void clearAll() {
        for (ComponentTypeTracker tracker : trackers.values()) {
            tracker.clearAll();
        }
    }
    
    /**
     * Check if an entity's component is dirty
     */
    public boolean isDirty(int entityId, Class<?> componentType) {
        ComponentTypeTracker tracker = trackers.get(componentType);
        return tracker != null && tracker.isDirty(entityId);
    }
    
    /**
     * Get all dirty entities for a component type
     */
    public Set<Integer> getDirtyEntities(Class<?> componentType) {
        ComponentTypeTracker tracker = trackers.get(componentType);
        if (tracker == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(tracker.dirtyEntities);
    }
    
    /**
     * Get dirty entities in a range
     */
    public Set<Integer> getDirtyEntitiesInRange(Class<?> componentType, 
                                                int startId, int endId) {
        ComponentTypeTracker tracker = trackers.get(componentType);
        if (tracker == null) {
            return Collections.emptySet();
        }
        
        Set<Integer> result = new HashSet<>();
        for (int entityId : tracker.dirtyEntities) {
            if (entityId >= startId && entityId < endId) {
                result.add(entityId);
            }
        }
        return result;
    }
    
    /**
     * Get the generation number for an entity's component
     * Can be used for optimistic read checks
     */
    public long getGeneration(int entityId, Class<?> componentType) {
        ComponentTypeTracker tracker = trackers.get(componentType);
        if (tracker == null) {
            return 0;
        }
        return tracker.getGeneration(entityId);
    }
    
    /**
     * Check if entity's component has changed since generation
     */
    public boolean hasChangedSince(int entityId, Class<?> componentType, long generation) {
        long currentGen = getGeneration(entityId, componentType);
        return currentGen > generation;
    }
    
    /**
     * Register a callback for component changes
     * Callback is invoked on the thread that makes the change
     */
    public void onComponentChange(Class<?> componentType, 
                                 Consumer<ComponentChange> callback) {
        ComponentTypeTracker tracker = trackers.computeIfAbsent(
            componentType, ComponentTypeTracker::new);
        tracker.callbacks.add(callback);
    }
    
    /**
     * Register a callback with filtering by change type
     */
    public void onComponentChange(Class<?> componentType, ChangeType changeType,
                                 Consumer<ComponentChange> callback) {
        onComponentChange(componentType, change -> {
            if (change.changeType == changeType) {
                callback.accept(change);
            }
        });
    }
    
    /**
     * Remove all callbacks for a component type
     */
    public void clearCallbacks(Class<?> componentType) {
        ComponentTypeTracker tracker = trackers.get(componentType);
        if (tracker != null) {
            tracker.callbacks.clear();
        }
    }
    
    /**
     * Get recent change history
     */
    public List<ComponentChange> getChangeHistory() {
        synchronized (changeHistory) {
            return new ArrayList<>(changeHistory);
        }
    }
    
    /**
     * Get changes for a specific component type
     */
    public List<ComponentChange> getChangeHistory(Class<?> componentType) {
        synchronized (changeHistory) {
            return changeHistory.stream()
                .filter(c -> c.componentType == componentType)
                .collect(java.util.stream.Collectors.toList());
        }
    }
    
    /**
     * Get changes for a specific entity
     */
    public List<ComponentChange> getChangeHistory(int entityId) {
        synchronized (changeHistory) {
            return changeHistory.stream()
                .filter(c -> c.entityId == entityId)
                .collect(java.util.stream.Collectors.toList());
        }
    }
    
    /**
     * Get changes since a timestamp
     */
    public List<ComponentChange> getChangesSince(long timestampNs) {
        synchronized (changeHistory) {
            return changeHistory.stream()
                .filter(c -> c.timestamp >= timestampNs)
                .collect(java.util.stream.Collectors.toList());
        }
    }
    
    /**
     * Get total number of changes tracked
     */
    public int getTotalChanges() {
        return totalChanges.get();
    }
    
    /**
     * Get number of dirty entities across all component types
     */
    public int getTotalDirtyEntities() {
        Set<Integer> allDirty = new HashSet<>();
        for (ComponentTypeTracker tracker : trackers.values()) {
            allDirty.addAll(tracker.dirtyEntities);
        }
        return allDirty.size();
    }
    
    /**
     * Get statistics for debugging
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalChanges", totalChanges.get());
        stats.put("totalDirtyEntities", getTotalDirtyEntities());
        stats.put("trackedComponentTypes", trackers.size());
        stats.put("historySize", changeHistory.size());
        
        Map<String, Integer> dirtyByType = new LinkedHashMap<>();
        for (Map.Entry<Class<?>, ComponentTypeTracker> entry : trackers.entrySet()) {
            dirtyByType.put(
                entry.getKey().getSimpleName(),
                entry.getValue().dirtyEntities.size()
            );
        }
        stats.put("dirtyEntitiesByType", dirtyByType);
        
        return stats;
    }
    
    /**
     * Print statistics to console
     */
    public void printStatistics() {
        System.out.println("Component Change Tracker Statistics:");
        System.out.println("===================================");
        System.out.println("Total changes: " + totalChanges.get());
        System.out.println("Total dirty entities: " + getTotalDirtyEntities());
        System.out.println("Tracked component types: " + trackers.size());
        System.out.println("History size: " + changeHistory.size());
        
        if (!trackers.isEmpty()) {
            System.out.println("\nDirty entities by type:");
            for (Map.Entry<Class<?>, ComponentTypeTracker> entry : trackers.entrySet()) {
                System.out.printf("  %-30s: %d dirty\n",
                    entry.getKey().getSimpleName(),
                    entry.getValue().dirtyEntities.size());
            }
        }
    }
    
    /**
     * Reset all tracking data
     */
    public void reset() {
        trackers.clear();
        totalChanges.set(0);
        synchronized (changeHistory) {
            changeHistory.clear();
        }
    }
}
