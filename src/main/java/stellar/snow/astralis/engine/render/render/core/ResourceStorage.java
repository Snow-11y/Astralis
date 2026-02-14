package stellar.snow.astralis.engine.render.core;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * ResourceStorage - Centralized Type-Safe Resource Management
 * 
 * Manages collections of ResourceSlots with:
 * - Type-safe registration and retrieval
 * - Automatic dependency tracking
 * - Lifecycle management
 * - Resource pooling and reuse
 * - Memory usage monitoring
 * 
 * Example:
 *   ResourceStorage storage = new ResourceStorage();
 *   ResourceSlot<Texture> tex = storage.register(Texture.class, myTexture, "albedo");
 *   storage.seal(); // Prevent accidental overwrites during frame
 */
public final class ResourceStorage implements AutoCloseable {
    
    // Type-safe resource registry
    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, ResourceSlot<?>>> resourcesByType = new ConcurrentHashMap<>();
    
    // Global slot registry (by ID)
    private final ConcurrentHashMap<Long, ResourceSlot<?>> slotsById = new ConcurrentHashMap<>();
    
    // Resource pools for reuse
    private final ConcurrentHashMap<Class<?>, Queue<ResourceSlot<?>>> resourcePools = new ConcurrentHashMap<>();
    
    // Storage state
    private final AtomicBoolean sealed = new AtomicBoolean(false);
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    
    // Statistics
    private final AtomicLong totalRegistrations = new AtomicLong(0);
    private final AtomicLong totalDeletions = new AtomicLong(0);
    private final AtomicLong totalReuses = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    
    // Thread safety
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Callbacks
    private final List<Runnable> sealCallbacks = new CopyOnWriteArrayList<>();
    private final List<Runnable> unsealCallbacks = new CopyOnWriteArrayList<>();
    
    // Memory tracking
    private final AtomicLong estimatedMemoryUsage = new AtomicLong(0);
    
    /**
     * Register a resource with eager initialization
     */
    public <T extends AutoCloseable> ResourceSlot<T> register(Class<T> type, T resource, String name) {
        if (sealed.get()) {
            throw new IllegalStateException("Cannot register resources while storage is sealed");
        }
        
        if (disposed.get()) {
            throw new IllegalStateException("Cannot register resources in disposed storage");
        }
        
        lock.writeLock().lock();
        try {
            // Check for existing resource with same name
            ResourceSlot<T> existing = getSlot(type, name);
            if (existing != null) {
                throw new IllegalStateException("Resource already registered: " + name);
            }
            
            // Create new slot
            ResourceSlot<T> slot = new ResourceSlot<>(type, resource, name);
            
            // Register in type map
            resourcesByType
                .computeIfAbsent(type, k -> new ConcurrentHashMap<>())
                .put(name, slot);
            
            // Register in global map
            slotsById.put(slot.getSlotId(), slot);
            
            totalRegistrations.incrementAndGet();
            
            return slot;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Register a resource with lazy initialization
     */
    public <T extends AutoCloseable> ResourceSlot<T> registerLazy(Class<T> type, java.util.function.Supplier<T> initializer, String name) {
        if (sealed.get()) {
            throw new IllegalStateException("Cannot register resources while storage is sealed");
        }
        
        lock.writeLock().lock();
        try {
            ResourceSlot<T> slot = new ResourceSlot<>(type, initializer, name);
            
            resourcesByType
                .computeIfAbsent(type, k -> new ConcurrentHashMap<>())
                .put(name, slot);
            
            slotsById.put(slot.getSlotId(), slot);
            
            totalRegistrations.incrementAndGet();
            
            return slot;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Get a resource slot by type and name
     */
    @SuppressWarnings("unchecked")
    public <T extends AutoCloseable> ResourceSlot<T> getSlot(Class<T> type, String name) {
        lock.readLock().lock();
        try {
            Map<String, ResourceSlot<?>> typeMap = resourcesByType.get(type);
            if (typeMap == null) {
                cacheMisses.incrementAndGet();
                return null;
            }
            
            ResourceSlot<?> slot = typeMap.get(name);
            if (slot != null) {
                cacheHits.incrementAndGet();
                return (ResourceSlot<T>) slot;
            } else {
                cacheMisses.incrementAndGet();
                return null;
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get a resource slot by ID
     */
    @SuppressWarnings("unchecked")
    public <T extends AutoCloseable> ResourceSlot<T> getSlotById(long slotId) {
        return (ResourceSlot<T>) slotsById.get(slotId);
    }
    
    /**
     * Get a resource directly (convenience method)
     */
    public <T extends AutoCloseable> T get(Class<T> type, String name) {
        ResourceSlot<T> slot = getSlot(type, name);
        if (slot == null) {
            throw new IllegalArgumentException("Resource not found: " + name + " (" + type.getSimpleName() + ")");
        }
        return slot.get();
    }
    
    /**
     * Try to get a resource (returns null if not found)
     */
    public <T extends AutoCloseable> T tryGet(Class<T> type, String name) {
        ResourceSlot<T> slot = getSlot(type, name);
        return slot != null ? slot.tryGet() : null;
    }
    
    /**
     * Check if a resource exists
     */
    public boolean contains(Class<?> type, String name) {
        lock.readLock().lock();
        try {
            Map<String, ResourceSlot<?>> typeMap = resourcesByType.get(type);
            return typeMap != null && typeMap.containsKey(name);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get all slots of a specific type
     */
    @SuppressWarnings("unchecked")
    public <T extends AutoCloseable> List<ResourceSlot<T>> getAllSlots(Class<T> type) {
        lock.readLock().lock();
        try {
            Map<String, ResourceSlot<?>> typeMap = resourcesByType.get(type);
            if (typeMap == null) {
                return Collections.emptyList();
            }
            return typeMap.values().stream()
                .map(slot -> (ResourceSlot<T>) slot)
                .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get all resources of a specific type
     */
    public <T extends AutoCloseable> List<T> getAll(Class<T> type) {
        return getAllSlots(type).stream()
            .filter(ResourceSlot::isInitialized)
            .map(ResourceSlot::get)
            .collect(Collectors.toList());
    }
    
    /**
     * Remove a resource
     */
    public <T extends AutoCloseable> void remove(Class<T> type, String name) {
        if (sealed.get()) {
            throw new IllegalStateException("Cannot remove resources while storage is sealed");
        }
        
        lock.writeLock().lock();
        try {
            Map<String, ResourceSlot<?>> typeMap = resourcesByType.get(type);
            if (typeMap != null) {
                ResourceSlot<?> slot = typeMap.remove(name);
                if (slot != null) {
                    slotsById.remove(slot.getSlotId());
                    slot.dispose();
                    totalDeletions.incrementAndGet();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Remove all resources of a specific type
     */
    public void removeAll(Class<?> type) {
        if (sealed.get()) {
            throw new IllegalStateException("Cannot remove resources while storage is sealed");
        }
        
        lock.writeLock().lock();
        try {
            Map<String, ResourceSlot<?>> typeMap = resourcesByType.remove(type);
            if (typeMap != null) {
                for (ResourceSlot<?> slot : typeMap.values()) {
                    slotsById.remove(slot.getSlotId());
                    slot.dispose();
                    totalDeletions.incrementAndGet();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Seal the storage to prevent modifications
     * This is typically called at the start of a frame to prevent accidental overwrites
     */
    public void seal() {
        if (sealed.compareAndSet(false, true)) {
            // Seal all individual slots
            lock.readLock().lock();
            try {
                for (ResourceSlot<?> slot : slotsById.values()) {
                    if (slot.isInitialized()) {
                        slot.seal();
                    }
                }
            } finally {
                lock.readLock().unlock();
            }
            
            // Notify callbacks
            for (Runnable callback : sealCallbacks) {
                callback.run();
            }
        }
    }
    
    /**
     * Unseal the storage to allow modifications
     */
    public void unseal() {
        if (sealed.compareAndSet(true, false)) {
            // Note: We don't unseal individual slots - they remain sealed
            // This is intentional to prevent accidental modifications
            
            // Notify callbacks
            for (Runnable callback : unsealCallbacks) {
                callback.run();
            }
        }
    }
    
    /**
     * Check if storage is sealed
     */
    public boolean isSealed() {
        return sealed.get();
    }
    
    /**
     * Add seal callback
     */
    public void addSealCallback(Runnable callback) {
        sealCallbacks.add(callback);
    }
    
    /**
     * Add unseal callback
     */
    public void addUnsealCallback(Runnable callback) {
        unsealCallbacks.add(callback);
    }
    
    /**
     * Get or create a resource from pool
     */
    @SuppressWarnings("unchecked")
    public <T extends AutoCloseable> ResourceSlot<T> getOrCreateFromPool(
            Class<T> type, 
            String name, 
            java.util.function.Supplier<T> factory) {
        
        lock.writeLock().lock();
        try {
            // Check if already exists
            ResourceSlot<T> existing = getSlot(type, name);
            if (existing != null) {
                return existing;
            }
            
            // Try to get from pool
            Queue<ResourceSlot<?>> pool = resourcePools.get(type);
            if (pool != null) {
                ResourceSlot<?> pooled = pool.poll();
                if (pooled != null) {
                    ResourceSlot<T> slot = (ResourceSlot<T>) pooled;
                    totalReuses.incrementAndGet();
                    
                    // Re-register with new name
                    resourcesByType
                        .computeIfAbsent(type, k -> new ConcurrentHashMap<>())
                        .put(name, slot);
                    
                    return slot;
                }
            }
            
            // Create new resource
            return register(type, factory.get(), name);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Return a resource to the pool for reuse
     */
    public <T extends AutoCloseable> void returnToPool(ResourceSlot<T> slot) {
        if (slot == null) return;
        
        lock.writeLock().lock();
        try {
            Class<?> type = slot.getResourceType();
            
            // Remove from active registry
            Map<String, ResourceSlot<?>> typeMap = resourcesByType.get(type);
            if (typeMap != null) {
                typeMap.values().remove(slot);
            }
            
            // Add to pool
            resourcePools
                .computeIfAbsent(type, k -> new ConcurrentLinkedQueue<>())
                .offer(slot);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Find resources matching a predicate
     */
    @SuppressWarnings("unchecked")
    public <T extends AutoCloseable> List<ResourceSlot<T>> find(Class<T> type, Predicate<ResourceSlot<T>> predicate) {
        return getAllSlots(type).stream()
            .filter(predicate)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all resource types
     */
    public Set<Class<?>> getResourceTypes() {
        lock.readLock().lock();
        try {
            return new HashSet<>(resourcesByType.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get total resource count
     */
    public int getTotalResourceCount() {
        return slotsById.size();
    }
    
    /**
     * Get resource count by type
     */
    public int getResourceCount(Class<?> type) {
        lock.readLock().lock();
        try {
            Map<String, ResourceSlot<?>> typeMap = resourcesByType.get(type);
            return typeMap != null ? typeMap.size() : 0;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get statistics
     */
    public StorageStatistics getStatistics() {
        return new StorageStatistics(
            totalRegistrations.get(),
            totalDeletions.get(),
            totalReuses.get(),
            cacheHits.get(),
            cacheMisses.get(),
            getTotalResourceCount(),
            estimatedMemoryUsage.get(),
            sealed.get()
        );
    }
    
    /**
     * Clear all resources
     */
    public void clear() {
        if (sealed.get()) {
            throw new IllegalStateException("Cannot clear sealed storage");
        }
        
        lock.writeLock().lock();
        try {
            // Dispose all slots
            for (ResourceSlot<?> slot : slotsById.values()) {
                slot.dispose();
            }
            
            resourcesByType.clear();
            slotsById.clear();
            resourcePools.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Dispose the storage
     */
    @Override
    public void close() {
        if (disposed.compareAndSet(false, true)) {
            clear();
        }
    }
    
    /**
     * Storage statistics
     */
    public static class StorageStatistics {
        public final long totalRegistrations;
        public final long totalDeletions;
        public final long totalReuses;
        public final long cacheHits;
        public final long cacheMisses;
        public final int activeResources;
        public final long estimatedMemoryBytes;
        public final boolean sealed;
        
        public StorageStatistics(long registrations, long deletions, long reuses,
                                long hits, long misses, int active, long memory, boolean sealed) {
            this.totalRegistrations = registrations;
            this.totalDeletions = deletions;
            this.totalReuses = reuses;
            this.cacheHits = hits;
            this.cacheMisses = misses;
            this.activeResources = active;
            this.estimatedMemoryBytes = memory;
            this.sealed = sealed;
        }
        
        public double getCacheHitRate() {
            long total = cacheHits + cacheMisses;
            return total > 0 ? (double) cacheHits / total : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("StorageStats[active=%d, reg=%d, del=%d, reuses=%d, hitRate=%.2f%%, mem=%dMB, sealed=%s]",
                activeResources, totalRegistrations, totalDeletions, totalReuses,
                getCacheHitRate() * 100, estimatedMemoryBytes / (1024 * 1024), sealed);
        }
    }
}
