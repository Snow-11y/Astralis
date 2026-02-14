package stellar.snow.astralis.engine.render.core;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;

/**
 * ResourceSlot<T> - Type-Safe Resource Handle
 * 
 * Prevents accidental type confusion at compile time.
 * You cannot treat a Texture as a Buffer - the compiler prevents it.
 * 
 * Features:
 * - Type-safe resource access
 * - Automatic lifecycle management
 * - Weak reference support for automatic cleanup
 * - Resource versioning for cache invalidation
 * - Thread-safe access
 * - Lazy initialization support
 */
public final class ResourceSlot<T extends AutoCloseable> {
    
    // Unique slot ID
    private final long slotId;
    private static final AtomicLong nextSlotId = new AtomicLong(0);
    
    // Resource type information
    private final Class<T> resourceType;
    private final String debugName;
    
    // Resource storage (nullable until initialized)
    private volatile T resource;
    
    // Weak reference for automatic cleanup
    private WeakReference<T> weakResource;
    
    // Resource state
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean sealed = new AtomicBoolean(false);
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    
    // Version counter for cache invalidation
    private final AtomicLong version = new AtomicLong(0);
    
    // Lazy initialization
    private Supplier<T> lazyInitializer;
    
    // Thread safety
    private final ReentrantLock lock = new ReentrantLock();
    
    // Access tracking for debugging
    private final AtomicLong accessCount = new AtomicLong(0);
    private final AtomicLong lastAccessTime = new AtomicLong(0);
    private final long creationTime;
    
    // Dependency tracking
    private final Set<ResourceSlot<?>> dependencies = ConcurrentHashMap.newKeySet();
    private final Set<ResourceSlot<?>> dependents = ConcurrentHashMap.newKeySet();
    
    // Metadata
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();
    
    /**
     * Create a new resource slot with eager initialization
     */
    public ResourceSlot(Class<T> type, T resource, String debugName) {
        this.slotId = nextSlotId.getAndIncrement();
        this.resourceType = type;
        this.debugName = debugName;
        this.resource = resource;
        this.creationTime = System.nanoTime();
        this.initialized.set(true);
        
        if (resource == null) {
            throw new IllegalArgumentException("Resource cannot be null for eager initialization");
        }
    }
    
    /**
     * Create a new resource slot with lazy initialization
     */
    public ResourceSlot(Class<T> type, Supplier<T> lazyInit, String debugName) {
        this.slotId = nextSlotId.getAndIncrement();
        this.resourceType = type;
        this.debugName = debugName;
        this.lazyInitializer = lazyInit;
        this.creationTime = System.nanoTime();
        this.initialized.set(false);
    }
    
    /**
     * Create an empty slot (must be initialized later)
     */
    public ResourceSlot(Class<T> type, String debugName) {
        this.slotId = nextSlotId.getAndIncrement();
        this.resourceType = type;
        this.debugName = debugName;
        this.creationTime = System.nanoTime();
        this.initialized.set(false);
    }
    
    /**
     * Get the resource (initializes if lazy)
     */
    public T get() {
        if (disposed.get()) {
            throw new IllegalStateException("Cannot access disposed resource slot: " + debugName);
        }
        
        if (!initialized.get() && lazyInitializer != null) {
            lock.lock();
            try {
                if (!initialized.get()) {
                    resource = lazyInitializer.get();
                    initialized.set(true);
                    version.incrementAndGet();
                }
            } finally {
                lock.unlock();
            }
        }
        
        if (!initialized.get()) {
            throw new IllegalStateException("Resource slot not initialized: " + debugName);
        }
        
        accessCount.incrementAndGet();
        lastAccessTime.set(System.nanoTime());
        
        return resource;
    }
    
    /**
     * Try to get the resource (returns null if not initialized)
     */
    public T tryGet() {
        if (!initialized.get() || disposed.get()) {
            return null;
        }
        
        accessCount.incrementAndGet();
        lastAccessTime.set(System.nanoTime());
        
        return resource;
    }
    
    /**
     * Initialize the slot with a resource
     */
    public void initialize(T resource) {
        if (sealed.get()) {
            throw new IllegalStateException("Cannot initialize sealed resource slot: " + debugName);
        }
        
        lock.lock();
        try {
            if (initialized.get()) {
                throw new IllegalStateException("Resource slot already initialized: " + debugName);
            }
            
            this.resource = resource;
            this.initialized.set(true);
            this.version.incrementAndGet();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Replace the resource (only if not sealed)
     */
    public void replace(T newResource) {
        if (sealed.get()) {
            throw new IllegalStateException("Cannot replace sealed resource: " + debugName);
        }
        
        lock.lock();
        try {
            // Close old resource
            if (resource != null && resource != newResource) {
                try {
                    resource.close();
                } catch (Exception e) {
                    System.err.println("Error closing old resource: " + e.getMessage());
                }
            }
            
            this.resource = newResource;
            this.initialized.set(true);
            this.version.incrementAndGet();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Seal the slot to prevent further modifications
     */
    public void seal() {
        if (!initialized.get()) {
            throw new IllegalStateException("Cannot seal uninitialized resource slot: " + debugName);
        }
        sealed.set(true);
    }
    
    /**
     * Check if the slot is sealed
     */
    public boolean isSealed() {
        return sealed.get();
    }
    
    /**
     * Check if the slot is initialized
     */
    public boolean isInitialized() {
        return initialized.get();
    }
    
    /**
     * Get current version (for cache invalidation)
     */
    public long getVersion() {
        return version.get();
    }
    
    /**
     * Invalidate the resource (increment version)
     */
    public void invalidate() {
        version.incrementAndGet();
    }
    
    /**
     * Add a dependency
     */
    public void addDependency(ResourceSlot<?> dependency) {
        dependencies.add(dependency);
        dependency.dependents.add(this);
    }
    
    /**
     * Get all dependencies
     */
    public Set<ResourceSlot<?>> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }
    
    /**
     * Get all dependents
     */
    public Set<ResourceSlot<?>> getDependents() {
        return Collections.unmodifiableSet(dependents);
    }
    
    /**
     * Set metadata
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    /**
     * Get metadata
     */
    @SuppressWarnings("unchecked")
    public <V> V getMetadata(String key, Class<V> type) {
        Object value = metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return (V) value;
        }
        return null;
    }
    
    /**
     * Dispose the resource
     */
    public void dispose() {
        if (disposed.compareAndSet(false, true)) {
            lock.lock();
            try {
                if (resource != null) {
                    try {
                        resource.close();
                    } catch (Exception e) {
                        System.err.println("Error disposing resource " + debugName + ": " + e.getMessage());
                    }
                    resource = null;
                }
                initialized.set(false);
                
                // Remove from dependency graphs
                for (ResourceSlot<?> dep : dependencies) {
                    dep.dependents.remove(this);
                }
                for (ResourceSlot<?> dep : dependents) {
                    dep.dependencies.remove(this);
                }
                dependencies.clear();
                dependents.clear();
            } finally {
                lock.unlock();
            }
        }
    }
    
    /**
     * Get slot ID
     */
    public long getSlotId() {
        return slotId;
    }
    
    /**
     * Get resource type
     */
    public Class<T> getResourceType() {
        return resourceType;
    }
    
    /**
     * Get debug name
     */
    public String getDebugName() {
        return debugName;
    }
    
    /**
     * Get access count
     */
    public long getAccessCount() {
        return accessCount.get();
    }
    
    /**
     * Get time since last access in milliseconds
     */
    public long getTimeSinceLastAccess() {
        long last = lastAccessTime.get();
        if (last == 0) return -1;
        return (System.nanoTime() - last) / 1_000_000;
    }
    
    /**
     * Get age in milliseconds
     */
    public long getAge() {
        return (System.nanoTime() - creationTime) / 1_000_000;
    }
    
    /**
     * Get debug info
     */
    public String getDebugInfo() {
        return String.format("ResourceSlot[id=%d, type=%s, name=%s, initialized=%s, sealed=%s, version=%d, accesses=%d, age=%dms]",
            slotId, resourceType.getSimpleName(), debugName, initialized.get(), sealed.get(), 
            version.get(), accessCount.get(), getAge());
    }
    
    @Override
    public String toString() {
        return String.format("ResourceSlot<%s>(%s)", resourceType.getSimpleName(), debugName);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ResourceSlot)) return false;
        ResourceSlot<?> other = (ResourceSlot<?>) obj;
        return slotId == other.slotId;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(slotId);
    }
    
    /**
     * Builder for creating resource slots
     */
    public static class Builder<T extends AutoCloseable> {
        private final Class<T> type;
        private String name;
        private T resource;
        private Supplier<T> lazyInit;
        private boolean autoSeal = false;
        
        public Builder(Class<T> type) {
            this.type = type;
        }
        
        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder<T> resource(T resource) {
            this.resource = resource;
            return this;
        }
        
        public Builder<T> lazyInit(Supplier<T> init) {
            this.lazyInit = init;
            return this;
        }
        
        public Builder<T> autoSeal(boolean seal) {
            this.autoSeal = seal;
            return this;
        }
        
        public ResourceSlot<T> build() {
            if (name == null) {
                name = "unnamed_" + type.getSimpleName();
            }
            
            ResourceSlot<T> slot;
            if (resource != null) {
                slot = new ResourceSlot<>(type, resource, name);
            } else if (lazyInit != null) {
                slot = new ResourceSlot<>(type, lazyInit, name);
            } else {
                slot = new ResourceSlot<>(type, name);
            }
            
            if (autoSeal && slot.isInitialized()) {
                slot.seal();
            }
            
            return slot;
        }
    }
}
