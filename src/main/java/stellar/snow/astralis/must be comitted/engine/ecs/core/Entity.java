package stellar.snow.astralis.engine.ecs.core;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Comparator;
import java.util.Objects;

/**
 * Entity - Lightweight, immutable entity handle using record semantics.
 *
 * <p>Core Features:</p>
 * <ul>
 *   <li>Generational index for safe handle recycling</li>
 *   <li>64-bit packing for efficient serialization</li>
 *   <li>Comparable for ordered collections</li>
 *   <li>Null-safe sentinel pattern</li>
 *   <li>Versioned entity support for change tracking</li>
 * </ul>
 *
 * @param index      Entity index (slot in entity array)
 * @param generation Generation counter for validity checking
 *
 * @author Enhanced ECS Framework
 * @version 2.0.0
 * @since Java 21
 */
public record Entity(int index, int generation) implements Comparable<Entity> {

    // ========================================================================
    // CONSTANTS
    // ========================================================================

    /** Invalid/null entity sentinel */
    public static final Entity NULL = new Entity(0, 0);

    /** Maximum valid entity index */
    public static final int MAX_INDEX = Integer.MAX_VALUE - 1;

    /** Maximum generation value before wrap */
    public static final int MAX_GENERATION = Integer.MAX_VALUE;

    /** Comparator by index then generation */
    public static final Comparator<Entity> INDEX_ORDER = 
        Comparator.comparingInt(Entity::index).thenComparingInt(Entity::generation);

    /** Comparator by generation then index */
    public static final Comparator<Entity> GENERATION_ORDER = 
        Comparator.comparingInt(Entity::generation).thenComparingInt(Entity::index);

    // ========================================================================
    // COMPACT CONSTRUCTOR
    // ========================================================================

    /**
     * Compact constructor with validation.
     */
    public Entity {
        // Allow NULL entity (0,0), but validate others
        if (index < 0) {
            throw new IllegalArgumentException("Entity index cannot be negative: " + index);
        }
        if (generation < 0) {
            throw new IllegalArgumentException("Entity generation cannot be negative: " + generation);
        }
    }

    // ========================================================================
    // STATIC FACTORY METHODS
    // ========================================================================

    /**
     * Create entity from index with generation 1 (new entity).
     */
    public static Entity of(int index) {
        return new Entity(index, 1);
    }

    /**
     * Create entity from index and generation.
     */
    public static Entity of(int index, int generation) {
        return new Entity(index, generation);
    }

    /**
     * Unpack entity from 64-bit handle.
     *
     * <p>Format: [generation:32][index:32]</p>
     */
    public static Entity unpack(long packed) {
        int idx = (int) (packed & 0xFFFFFFFFL);
        int gen = (int) (packed >>> 32);
        return new Entity(idx, gen);
    }

    /**
     * Parse entity from string format "Entity[index:generation]" or "index:generation".
     */
    public static Entity parse(String str) {
        Objects.requireNonNull(str, "Entity string cannot be null");
        
        String cleaned = str.trim();
        if (cleaned.startsWith("Entity[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(7, cleaned.length() - 1);
        }

        int colonIdx = cleaned.indexOf(':');
        if (colonIdx < 0) {
            throw new IllegalArgumentException("Invalid entity format: " + str);
        }

        try {
            int idx = Integer.parseInt(cleaned.substring(0, colonIdx).trim());
            int gen = Integer.parseInt(cleaned.substring(colonIdx + 1).trim());
            return new Entity(idx, gen);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid entity format: " + str, e);
        }
    }

    /**
     * Create null/invalid entity.
     */
    public static Entity nullEntity() {
        return NULL;
    }

    // ========================================================================
    // INSTANCE METHODS
    // ========================================================================

    /**
     * Pack entity into 64-bit handle.
     *
     * <p>Format: [generation:32][index:32]</p>
     */
    public long pack() {
        return ((long) generation << 32) | (index & 0xFFFFFFFFL);
    }

    /**
     * Check if entity is valid (non-null, non-zero).
     */
    public boolean isValid() {
        return index != 0 || generation != 0;
    }

    /**
     * Check if entity is null/invalid.
     */
    public boolean isNull() {
        return index == 0 && generation == 0;
    }

    /**
     * Check if this entity is the same slot as another (ignoring generation).
     */
    public boolean sameSlot(Entity other) {
        return other != null && this.index == other.index;
    }

    /**
     * Check if this entity is newer than another (same slot, higher generation).
     */
    public boolean isNewerThan(Entity other) {
        return other != null && this.index == other.index && this.generation > other.generation;
    }

    /**
     * Check if this entity is older than another (same slot, lower generation).
     */
    public boolean isOlderThan(Entity other) {
        return other != null && this.index == other.index && this.generation < other.generation;
    }

    /**
     * Get next generation of this entity (for recycling).
     */
    public Entity nextGeneration() {
        int nextGen = (generation == MAX_GENERATION) ? 1 : generation + 1;
        return new Entity(index, nextGen);
    }

    /**
     * Create a versioned entity with additional version tracking.
     */
    public VersionedEntity withVersion(long version) {
        return new VersionedEntity(this, version);
    }

    // ========================================================================
    // COMPARABLE
    // ========================================================================

    @Override
    public int compareTo(Entity other) {
        int cmp = Integer.compare(this.index, other.index);
        return cmp != 0 ? cmp : Integer.compare(this.generation, other.generation);
    }

    // ========================================================================
    // OBJECT METHODS
    // ========================================================================

    @Override
    public String toString() {
        if (isNull()) {
            return "Entity[NULL]";
        }
        return "Entity[" + index + ":" + generation + "]";
    }

    /**
     * Compact string representation.
     */
    public String toCompactString() {
        return index + ":" + generation;
    }

    // ========================================================================
    // VERSIONED ENTITY
    // ========================================================================

    /**
     * Entity with additional version tracking for change detection.
     */
    public record VersionedEntity(Entity entity, long version) implements Comparable<VersionedEntity> {
        
        public int index() { return entity.index(); }
        public int generation() { return entity.generation(); }
        public boolean isValid() { return entity.isValid(); }
        public long pack() { return entity.pack(); }

        /**
         * Check if entity has been modified since given version.
         */
        public boolean modifiedSince(long sinceVersion) {
            return this.version > sinceVersion;
        }

        /**
         * Create with incremented version.
         */
        public VersionedEntity incrementVersion() {
            return new VersionedEntity(entity, version + 1);
        }

        @Override
        public int compareTo(VersionedEntity other) {
            int cmp = entity.compareTo(other.entity);
            return cmp != 0 ? cmp : Long.compare(this.version, other.version);
        }
    }

    // ========================================================================
    // ENTITY REFERENCE (MUTABLE)
    // ========================================================================

    /**
     * Mutable entity reference for atomic operations.
     * 
     * <p>Useful for lock-free algorithms where entity handle needs atomic updates.</p>
     */
    public static final class Ref {
        private static final VarHandle PACKED_HANDLE;
        
        static {
            try {
                PACKED_HANDLE = MethodHandles.lookup()
                    .findVarHandle(Ref.class, "packed", long.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        @SuppressWarnings("unused")
        private volatile long packed;

        public Ref() {
            this.packed = NULL.pack();
        }

        public Ref(Entity entity) {
            this.packed = entity.pack();
        }

        public Entity get() {
            return Entity.unpack((long) PACKED_HANDLE.getVolatile(this));
        }

        public void set(Entity entity) {
            PACKED_HANDLE.setVolatile(this, entity.pack());
        }

        public boolean compareAndSet(Entity expected, Entity update) {
            return PACKED_HANDLE.compareAndSet(this, expected.pack(), update.pack());
        }

        public Entity getAndSet(Entity newValue) {
            return Entity.unpack((long) PACKED_HANDLE.getAndSet(this, newValue.pack()));
        }

        public void clear() {
            set(NULL);
        }

        public boolean isNull() {
            return get().isNull();
        }

        @Override
        public String toString() {
            return "EntityRef[" + get() + "]";
        }
    }

    // ========================================================================
    // ENTITY BUILDER (FOR COMPLEX SCENARIOS)
    // ========================================================================

    /**
     * Builder for creating entities with additional metadata.
     */
    public static final class Builder {
        private int index = 0;
        private int generation = 1;
        private long version = 0;
        private boolean versioned = false;

        public Builder index(int index) {
            this.index = index;
            return this;
        }

        public Builder generation(int generation) {
            this.generation = generation;
            return this;
        }

        public Builder version(long version) {
            this.version = version;
            this.versioned = true;
            return this;
        }

        public Entity build() {
            return new Entity(index, generation);
        }

        public VersionedEntity buildVersioned() {
            return new VersionedEntity(new Entity(index, generation), version);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
