package stellar.snow.astralis.engine.ecs.storage;

import stellar.snow.astralis.Astralis;

import java.lang.annotation.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * ComponentRegistry - Advanced component type metadata management with Java 21+ features.
 *
 * <p>Core Features:</p>
 * <ul>
 *   <li>Record-based ComponentType with immutable metadata</li>
 *   <li>Annotation-driven component configuration</li>
 *   <li>Automatic size/alignment calculation via reflection</li>
 *   <li>Component categories and tagging</li>
 *   <li>Schema versioning for serialization</li>
 *   <li>Memory layout optimization hints</li>
 *   <li>Thread-safe registration with optimistic locking</li>
 *   <li>Component relationship tracking</li>
 *   <li>Validation and constraint support</li>
 *   <li>Hot-reload support for development</li>
 * </ul>
 *
 * @author Enhanced ECS Framework
 * @version 2.0.0
 * @since Java 21
 */
public final class ComponentRegistry {

    // ========================================================================
    // SINGLETON
    // ========================================================================

    private static final ComponentRegistry INSTANCE = new ComponentRegistry();

    public static ComponentRegistry get() { return INSTANCE; }

    // ========================================================================
    // CONSTANTS
    // ========================================================================

    /** Maximum supported component types (limited by 64-bit mask) */
    public static final int MAX_COMPONENT_TYPES = 64;

    /** Extended component types (beyond mask-based queries) */
    public static final int MAX_EXTENDED_TYPES = 256;

    /** Cache line size for alignment optimization */
    private static final int CACHE_LINE_SIZE = 64;

    /** Default alignment for components */
    private static final int DEFAULT_ALIGNMENT = 4;

    // ========================================================================
    // ANNOTATIONS
    // ========================================================================

    /**
     * Declare component size and alignment.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ComponentSize {
        int bytes();
        int alignment() default 4;
    }

    /**
     * Mark component as GPU-accessible.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface GpuComponent {
        /** Binding point hint */
        int binding() default -1;
        /** Whether component is frequently updated */
        boolean dynamic() default true;
    }

    /**
     * Component category for organization.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ComponentCategory {
        String value();
    }

    /**
     * Component dependencies (requires these components to exist).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface RequiresComponents {
        Class<?>[] value();
    }

    /**
     * Component exclusions (cannot coexist with these).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ExcludesComponents {
        Class<?>[] value();
    }

    /**
     * Mark component as tag-only (zero size).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface TagComponent {}

    /**
     * Mark component as singleton (one per world).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface SingletonComponent {}

    /**
     * Component schema version for serialization.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface SchemaVersion {
        int value();
    }

    /**
     * Mark field as serializable in component.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ComponentField {
        int order() default 0;
        boolean optional() default false;
    }

    // ========================================================================
    // ENUMS
    // ========================================================================

    /**
     * Component storage strategy hints.
     */
    public enum StorageHint {
        /** Standard archetype storage */
        ARCHETYPE,
        /** Sparse set for rarely-used components */
        SPARSE,
        /** Dense array for common components */
        DENSE,
        /** Shared/singleton storage */
        SHARED,
        /** Tag component (no storage) */
        TAG
    }

    /**
     * Memory access pattern hints.
     */
    public enum AccessPattern {
        /** Read-mostly component */
        READ_HEAVY,
        /** Write-mostly component */
        WRITE_HEAVY,
        /** Balanced read/write */
        BALANCED,
        /** Rarely accessed */
        COLD
    }

    /**
     * Component flags.
     */
    public enum ComponentFlag {
        GPU_ACCESSIBLE,
        DYNAMIC_UPDATE,
        SINGLETON,
        TAG_ONLY,
        POOLED,
        COMPRESSED,
        ENCRYPTED,
        NETWORKED,
        PERSISTENT,
        TRANSIENT,
        HOT_RELOADABLE
    }

    // ========================================================================
    // RECORDS
    // ========================================================================

    /**
     * Immutable component type metadata.
     */
    public record ComponentType(
        int id,
        Class<?> clazz,
        String name,
        String qualifiedName,
        int sizeBytes,
        int alignment,
        int schemaVersion,
        String category,
        StorageHint storageHint,
        AccessPattern accessPattern,
        EnumSet<ComponentFlag> flags,
        List<FieldInfo> fields,
        Set<Class<?>> requiredComponents,
        Set<Class<?>> excludedComponents,
        long registrationTime
    ) {
        /**
         * Check if component has flag.
         */
        public boolean hasFlag(ComponentFlag flag) {
            return flags.contains(flag);
        }

        /**
         * Check if component is a tag (zero-size).
         */
        public boolean isTag() {
            return sizeBytes == 0 || flags.contains(ComponentFlag.TAG_ONLY);
        }

        /**
         * Check if component is GPU-accessible.
         */
        public boolean isGpuAccessible() {
            return flags.contains(ComponentFlag.GPU_ACCESSIBLE);
        }

        /**
         * Check if component is singleton.
         */
        public boolean isSingleton() {
            return flags.contains(ComponentFlag.SINGLETON);
        }

        /**
         * Get component mask bit.
         */
        public long maskBit() {
            return id < 64 ? (1L << id) : 0L;
        }

        /**
         * Check if this component requires another.
         */
        public boolean requires(Class<?> other) {
            return requiredComponents.contains(other);
        }

        /**
         * Check if this component excludes another.
         */
        public boolean excludes(Class<?> other) {
            return excludedComponents.contains(other);
        }

        @Override
        public String toString() {
            return String.format("ComponentType[%d:%s, size=%d, align=%d, flags=%s]",
                id, name, sizeBytes, alignment, flags);
        }
    }

    /**
     * Field information within a component.
     */
    public record FieldInfo(
        String name,
        Class<?> type,
        int offset,
        int size,
        int order,
        boolean optional,
        boolean primitive
    ) {
        public static FieldInfo from(Field field, int offset) {
            ComponentField annotation = field.getAnnotation(ComponentField.class);
            int order = annotation != null ? annotation.order() : 0;
            boolean optional = annotation != null && annotation.optional();
            
            Class<?> type = field.getType();
            int size = getPrimitiveSize(type);
            
            return new FieldInfo(
                field.getName(),
                type,
                offset,
                size,
                order,
                optional,
                type.isPrimitive()
            );
        }
    }

    /**
     * Component registration builder.
     */
    public static final class Registration<T> {
        private final ComponentRegistry registry;
        private final Class<T> clazz;
        private int sizeBytes = -1;  // -1 = auto-detect
        private int alignment = DEFAULT_ALIGNMENT;
        private int schemaVersion = 1;
        private String category = "default";
        private StorageHint storageHint = StorageHint.ARCHETYPE;
        private AccessPattern accessPattern = AccessPattern.BALANCED;
        private final EnumSet<ComponentFlag> flags = EnumSet.noneOf(ComponentFlag.class);
        private final Set<Class<?>> required = new HashSet<>();
        private final Set<Class<?>> excluded = new HashSet<>();

        private Registration(ComponentRegistry registry, Class<T> clazz) {
            this.registry = registry;
            this.clazz = clazz;
            processAnnotations();
        }

        private void processAnnotations() {
            // ComponentSize
            if (clazz.isAnnotationPresent(ComponentSize.class)) {
                ComponentSize cs = clazz.getAnnotation(ComponentSize.class);
                sizeBytes = cs.bytes();
                alignment = cs.alignment();
            }

            // GpuComponent
            if (clazz.isAnnotationPresent(GpuComponent.class)) {
                flags.add(ComponentFlag.GPU_ACCESSIBLE);
                if (clazz.getAnnotation(GpuComponent.class).dynamic()) {
                    flags.add(ComponentFlag.DYNAMIC_UPDATE);
                }
            }

            // ComponentCategory
            if (clazz.isAnnotationPresent(ComponentCategory.class)) {
                category = clazz.getAnnotation(ComponentCategory.class).value();
            }

            // RequiresComponents
            if (clazz.isAnnotationPresent(RequiresComponents.class)) {
                Collections.addAll(required, clazz.getAnnotation(RequiresComponents.class).value());
            }

            // ExcludesComponents
            if (clazz.isAnnotationPresent(ExcludesComponents.class)) {
                Collections.addAll(excluded, clazz.getAnnotation(ExcludesComponents.class).value());
            }

            // TagComponent
            if (clazz.isAnnotationPresent(TagComponent.class)) {
                flags.add(ComponentFlag.TAG_ONLY);
                sizeBytes = 0;
                storageHint = StorageHint.TAG;
            }

            // SingletonComponent
            if (clazz.isAnnotationPresent(SingletonComponent.class)) {
                flags.add(ComponentFlag.SINGLETON);
                storageHint = StorageHint.SHARED;
            }

            // SchemaVersion
            if (clazz.isAnnotationPresent(SchemaVersion.class)) {
                schemaVersion = clazz.getAnnotation(SchemaVersion.class).value();
            }
        }

        public Registration<T> size(int bytes) {
            this.sizeBytes = bytes;
            return this;
        }

        public Registration<T> size(int bytes, int alignment) {
            this.sizeBytes = bytes;
            this.alignment = alignment;
            return this;
        }

        public Registration<T> alignment(int alignment) {
            this.alignment = alignment;
            return this;
        }

        public Registration<T> schemaVersion(int version) {
            this.schemaVersion = version;
            return this;
        }

        public Registration<T> category(String category) {
            this.category = category;
            return this;
        }

        public Registration<T> storageHint(StorageHint hint) {
            this.storageHint = hint;
            return this;
        }

        public Registration<T> accessPattern(AccessPattern pattern) {
            this.accessPattern = pattern;
            return this;
        }

        public Registration<T> flag(ComponentFlag flag) {
            this.flags.add(flag);
            return this;
        }

        public Registration<T> flags(ComponentFlag... flags) {
            Collections.addAll(this.flags, flags);
            return this;
        }

        public Registration<T> gpuAccessible() {
            return flag(ComponentFlag.GPU_ACCESSIBLE);
        }

        public Registration<T> dynamic() {
            return flag(ComponentFlag.DYNAMIC_UPDATE);
        }

        public Registration<T> singleton() {
            return flag(ComponentFlag.SINGLETON).storageHint(StorageHint.SHARED);
        }

        public Registration<T> tag() {
            this.sizeBytes = 0;
            return flag(ComponentFlag.TAG_ONLY).storageHint(StorageHint.TAG);
        }

        public Registration<T> requires(Class<?>... components) {
            Collections.addAll(required, components);
            return this;
        }

        public Registration<T> excludes(Class<?>... components) {
            Collections.addAll(excluded, components);
            return this;
        }

        public ComponentType register() {
            return registry.registerInternal(
                clazz, sizeBytes, alignment, schemaVersion,
                category, storageHint, accessPattern,
                flags, required, excluded
            );
        }
    }

    // ========================================================================
    // STATE
    // ========================================================================

    private final AtomicInteger nextComponentId = new AtomicInteger(0);
    private final ConcurrentHashMap<Class<?>, ComponentType> typesByClass = new ConcurrentHashMap<>();
    private final AtomicReferenceArray<ComponentType> typesById = new AtomicReferenceArray<>(MAX_EXTENDED_TYPES);
    private final StampedLock registrationLock = new StampedLock();

    // Category indexing
    private final ConcurrentHashMap<String, Set<ComponentType>> typesByCategory = new ConcurrentHashMap<>();

    // Name to type mapping for serialization
    private final ConcurrentHashMap<String, ComponentType> typesByName = new ConcurrentHashMap<>();

    // Relationship graph
    private final ConcurrentHashMap<Class<?>, Set<Class<?>>> dependencies = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, Set<Class<?>>> exclusions = new ConcurrentHashMap<>();

    // Statistics
    private final LongAdder registrationCount = new LongAdder();
    private final LongAdder lookupCount = new LongAdder();

    // Pre-registered primitive component sizes
    private static final Map<Class<?>, Integer> PRIMITIVE_SIZES = Map.ofEntries(
        Map.entry(byte.class, 1),
        Map.entry(Byte.class, 1),
        Map.entry(short.class, 2),
        Map.entry(Short.class, 2),
        Map.entry(char.class, 2),
        Map.entry(Character.class, 2),
        Map.entry(int.class, 4),
        Map.entry(Integer.class, 4),
        Map.entry(long.class, 8),
        Map.entry(Long.class, 8),
        Map.entry(float.class, 4),
        Map.entry(Float.class, 4),
        Map.entry(double.class, 8),
        Map.entry(Double.class, 8),
        Map.entry(boolean.class, 1),
        Map.entry(Boolean.class, 1)
    );

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    private ComponentRegistry() {
        // Register common primitive wrappers automatically
        registerBuiltinTypes();
    }

    private void registerBuiltinTypes() {
        // Could pre-register common types here
    }

    // ========================================================================
    // REGISTRATION API
    // ========================================================================

    /**
     * Start fluent registration for a component type.
     */
    public <T> Registration<T> builder(Class<T> clazz) {
        return new Registration<>(this, clazz);
    }

    /**
     * Register a component type with explicit size and alignment.
     */
    public <T> ComponentType register(Class<T> clazz, int sizeBytes, int alignment) {
        return builder(clazz).size(sizeBytes, alignment).register();
    }

    /**
     * Register with auto-detected size.
     */
    public <T> ComponentType register(Class<T> clazz) {
        return builder(clazz).register();
    }

    /**
     * Register or get existing type.
     */
    public <T> ComponentType registerOrGet(Class<T> clazz) {
        ComponentType existing = typesByClass.get(clazz);
        if (existing != null) return existing;
        return register(clazz);
    }

    /**
     * Internal registration implementation.
     */
    private <T> ComponentType registerInternal(
            Class<T> clazz,
            int sizeBytes,
            int alignment,
            int schemaVersion,
            String category,
            StorageHint storageHint,
            AccessPattern accessPattern,
            EnumSet<ComponentFlag> flags,
            Set<Class<?>> required,
            Set<Class<?>> excluded) {

        return typesByClass.computeIfAbsent(clazz, c -> {
            long stamp = registrationLock.writeLock();
            try {
                // Double-check
                ComponentType existing = typesByClass.get(clazz);
                if (existing != null) return existing;

                int id = nextComponentId.getAndIncrement();
                if (id >= MAX_EXTENDED_TYPES) {
                    throw new ComponentLimitExceededException(MAX_EXTENDED_TYPES);
                }

                // Auto-detect size if not specified
                int actualSize = sizeBytes >= 0 ? sizeBytes : estimateSize(clazz);
                int actualAlignment = Math.max(alignment, 1);

                // Analyze fields
                List<FieldInfo> fields = analyzeFields(clazz);

                ComponentType type = new ComponentType(
                    id,
                    clazz,
                    clazz.getSimpleName(),
                    clazz.getName(),
                    actualSize,
                    actualAlignment,
                    schemaVersion,
                    category,
                    storageHint,
                    accessPattern,
                    EnumSet.copyOf(flags),
                    List.copyOf(fields),
                    Set.copyOf(required),
                    Set.copyOf(excluded),
                    System.nanoTime()
                );

                // Store in all indices
                typesById.set(id, type);
                typesByName.put(clazz.getSimpleName(), type);
                typesByName.put(clazz.getName(), type);
                typesByCategory.computeIfAbsent(category, k -> ConcurrentHashMap.newKeySet()).add(type);

                // Update relationship graph
                for (Class<?> req : required) {
                    dependencies.computeIfAbsent(clazz, k -> ConcurrentHashMap.newKeySet()).add(req);
                }
                for (Class<?> excl : excluded) {
                    exclusions.computeIfAbsent(clazz, k -> ConcurrentHashMap.newKeySet()).add(excl);
                }

                registrationCount.increment();

                Astralis.LOGGER.debug("[ECS] Registered component: {} (id={}, size={}, align={})",
                    type.name, id, actualSize, actualAlignment);

                return type;
            } finally {
                registrationLock.unlockWrite(stamp);
            }
        });
    }

    // ========================================================================
    // LOOKUP API
    // ========================================================================

    /**
     * Get component type by class (throws if not registered).
     */
    public ComponentType getType(Class<?> clazz) {
        lookupCount.increment();
        ComponentType type = typesByClass.get(clazz);
        if (type == null) {
            throw new ComponentNotRegisteredException(clazz);
        }
        return type;
    }

    /**
     * Get component type by class (returns null if not registered).
     */
    public Optional<ComponentType> findType(Class<?> clazz) {
        lookupCount.increment();
        return Optional.ofNullable(typesByClass.get(clazz));
    }

    /**
     * Get component type by ID.
     */
    public ComponentType getType(int id) {
        if (id < 0 || id >= MAX_EXTENDED_TYPES) {
            throw new IllegalArgumentException("Invalid component type ID: " + id);
        }
        ComponentType type = typesById.get(id);
        if (type == null) {
            throw new IllegalArgumentException("Component type not found for ID: " + id);
        }
        return type;
    }

    /**
     * Get component type by ID (optional).
     */
    public Optional<ComponentType> findType(int id) {
        if (id < 0 || id >= MAX_EXTENDED_TYPES) {
            return Optional.empty();
        }
        return Optional.ofNullable(typesById.get(id));
    }

    /**
     * Get component type by name.
     */
    public Optional<ComponentType> findTypeByName(String name) {
        return Optional.ofNullable(typesByName.get(name));
    }

    /**
     * Check if type is registered.
     */
    public boolean isRegistered(Class<?> clazz) {
        return typesByClass.containsKey(clazz);
    }

    /**
     * Get component mask for multiple types.
     */
    public long computeMask(Class<?>... classes) {
        long mask = 0;
        for (Class<?> clazz : classes) {
            ComponentType type = getType(clazz);
            if (type.id < 64) {
                mask |= (1L << type.id);
            }
        }
        return mask;
    }

    /**
     * Get component mask for collection of types.
     */
    public long computeMask(Collection<Class<?>> classes) {
        long mask = 0;
        for (Class<?> clazz : classes) {
            ComponentType type = getType(clazz);
            if (type.id < 64) {
                mask |= (1L << type.id);
            }
        }
        return mask;
    }

    /**
     * Get classes from mask.
     */
    public List<Class<?>> getClassesFromMask(long mask) {
        List<Class<?>> result = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            if ((mask & (1L << i)) != 0) {
                ComponentType type = typesById.get(i);
                if (type != null) {
                    result.add(type.clazz);
                }
            }
        }
        return result;
    }

    // ========================================================================
    // QUERY API
    // ========================================================================

    /**
     * Get all registered types.
     */
    public Collection<ComponentType> getAllTypes() {
        return Collections.unmodifiableCollection(typesByClass.values());
    }

    /**
     * Get types by category.
     */
    public Set<ComponentType> getTypesByCategory(String category) {
        return Collections.unmodifiableSet(
            typesByCategory.getOrDefault(category, Set.of()));
    }

    /**
     * Get all categories.
     */
    public Set<String> getCategories() {
        return Collections.unmodifiableSet(typesByCategory.keySet());
    }

    /**
     * Get types with specific flag.
     */
    public List<ComponentType> getTypesWithFlag(ComponentFlag flag) {
        return typesByClass.values().stream()
            .filter(t -> t.hasFlag(flag))
            .toList();
    }

    /**
     * Get GPU-accessible types.
     */
    public List<ComponentType> getGpuTypes() {
        return getTypesWithFlag(ComponentFlag.GPU_ACCESSIBLE);
    }

    /**
     * Get tag components.
     */
    public List<ComponentType> getTagTypes() {
        return typesByClass.values().stream()
            .filter(ComponentType::isTag)
            .toList();
    }

    /**
     * Get singleton components.
     */
    public List<ComponentType> getSingletonTypes() {
        return getTypesWithFlag(ComponentFlag.SINGLETON);
    }

    /**
     * Get number of registered types.
     */
    public int getTypeCount() {
        return nextComponentId.get();
    }

    /**
     * Stream all types.
     */
    public Stream<ComponentType> stream() {
        return typesByClass.values().stream();
    }

    // ========================================================================
    // RELATIONSHIP API
    // ========================================================================

    /**
     * Get required components for a type.
     */
    public Set<Class<?>> getRequiredComponents(Class<?> clazz) {
        return Collections.unmodifiableSet(
            dependencies.getOrDefault(clazz, Set.of()));
    }

    /**
     * Get excluded components for a type.
     */
    public Set<Class<?>> getExcludedComponents(Class<?> clazz) {
        return Collections.unmodifiableSet(
            exclusions.getOrDefault(clazz, Set.of()));
    }

    /**
     * Validate component combination.
     */
    public ValidationResult validateCombination(Collection<Class<?>> components) {
        List<String> errors = new ArrayList<>();
        Set<Class<?>> componentSet = new HashSet<>(components);

        for (Class<?> component : components) {
            ComponentType type = findType(component).orElse(null);
            if (type == null) {
                errors.add("Component not registered: " + component.getName());
                continue;
            }

            // Check required
            for (Class<?> required : type.requiredComponents()) {
                if (!componentSet.contains(required)) {
                    errors.add(type.name + " requires " + required.getSimpleName());
                }
            }

            // Check excluded
            for (Class<?> excluded : type.excludedComponents()) {
                if (componentSet.contains(excluded)) {
                    errors.add(type.name + " cannot coexist with " + excluded.getSimpleName());
                }
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public record ValidationResult(boolean valid, List<String> errors) {
        public void throwIfInvalid() {
            if (!valid) {
                throw new InvalidComponentCombinationException(errors);
            }
        }
    }

    // ========================================================================
    // SIZE ESTIMATION
    // ========================================================================

    /**
     * Get primitive size.
     */
    public static int getPrimitiveSize(Class<?> type) {
        return PRIMITIVE_SIZES.getOrDefault(type, 8); // Default to reference size
    }

    /**
     * Estimate size of a class based on fields.
     */
    private int estimateSize(Class<?> clazz) {
        // Check for tag annotation
        if (clazz.isAnnotationPresent(TagComponent.class)) {
            return 0;
        }

        int size = 0;
        for (Field field : getAllFields(clazz)) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (Modifier.isTransient(field.getModifiers())) continue;

            Class<?> fieldType = field.getType();
            Integer fieldSize = PRIMITIVE_SIZES.get(fieldType);
            
            if (fieldSize != null) {
                size += fieldSize;
            } else if (fieldType.isArray()) {
                // Reference + length estimate
                size += 16;
            } else if (fieldType.isEnum()) {
                size += 4; // Ordinal as int
            } else if (fieldType == String.class) {
                size += 8; // Reference
            } else {
                // Nested object - estimate recursively with depth limit
                size += 8; // Reference
            }
        }

        return Math.max(size, 4); // Minimum 4 bytes
    }

    /**
     * Analyze fields of a class.
     */
    private List<FieldInfo> analyzeFields(Class<?> clazz) {
        List<FieldInfo> fields = new ArrayList<>();
        int offset = 0;

        for (Field field : getAllFields(clazz)) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (Modifier.isTransient(field.getModifiers())) continue;

            FieldInfo info = FieldInfo.from(field, offset);
            fields.add(info);
            offset += info.size();
        }

        // Sort by order annotation
        fields.sort(Comparator.comparingInt(FieldInfo::order));

        return fields;
    }

    /**
     * Get all fields including inherited.
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        
        while (current != null && current != Object.class) {
            Collections.addAll(fields, current.getDeclaredFields());
            current = current.getSuperclass();
        }
        
        return fields;
    }

    /**
     * Calculate aligned size.
     */
    public static int alignUp(int value, int alignment) {
        return (value + alignment - 1) & ~(alignment - 1);
    }

    // ========================================================================
    // SERIALIZATION SUPPORT
    // ========================================================================

    /**
     * Create schema snapshot for serialization.
     */
    public SchemaSnapshot createSnapshot() {
        Map<String, TypeSchema> schemas = new HashMap<>();
        
        for (ComponentType type : typesByClass.values()) {
            schemas.put(type.qualifiedName(), new TypeSchema(
                type.id(),
                type.qualifiedName(),
                type.sizeBytes(),
                type.alignment(),
                type.schemaVersion(),
                type.fields().stream()
                    .map(f -> new FieldSchema(f.name(), f.type().getName(), f.offset(), f.size()))
                    .toList()
            ));
        }
        
        return new SchemaSnapshot(schemas, System.currentTimeMillis());
    }

    public record SchemaSnapshot(Map<String, TypeSchema> schemas, long timestamp) {}
    public record TypeSchema(int id, String name, int size, int alignment, int version, List<FieldSchema> fields) {}
    public record FieldSchema(String name, String type, int offset, int size) {}

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Get registry statistics.
     */
    public RegistryStats getStats() {
        int gpuCount = (int) stream().filter(ComponentType::isGpuAccessible).count();
        int tagCount = (int) stream().filter(ComponentType::isTag).count();
        int singletonCount = (int) stream().filter(ComponentType::isSingleton).count();
        long totalSize = stream().mapToLong(ComponentType::sizeBytes).sum();

        return new RegistryStats(
            getTypeCount(),
            gpuCount,
            tagCount,
            singletonCount,
            typesByCategory.size(),
            totalSize,
            registrationCount.sum(),
            lookupCount.sum()
        );
    }

    public record RegistryStats(
        int typeCount,
        int gpuTypeCount,
        int tagTypeCount,
        int singletonTypeCount,
        int categoryCount,
        long totalSizeBytes,
        long registrationCount,
        long lookupCount
    ) {}

    // ========================================================================
    // DEBUG
    // ========================================================================

    /**
     * Get detailed registry information.
     */
    public String describe() {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append("  Component Registry\n");
        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append("  Types: ").append(getTypeCount()).append("/").append(MAX_EXTENDED_TYPES).append("\n");
        sb.append("  Categories: ").append(typesByCategory.keySet()).append("\n");
        sb.append("───────────────────────────────────────────────────────────────\n");

        // Group by category
        for (String category : typesByCategory.keySet()) {
            sb.append("  [").append(category).append("]\n");
            for (ComponentType type : typesByCategory.get(category)) {
                sb.append("    ").append(type).append("\n");
            }
        }

        sb.append("═══════════════════════════════════════════════════════════════\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("ComponentRegistry[types=%d, categories=%d]",
            getTypeCount(), typesByCategory.size());
    }

    // ========================================================================
    // EXCEPTIONS
    // ========================================================================

    public static class ComponentLimitExceededException extends RuntimeException {
        public ComponentLimitExceededException(int limit) {
            super("Component type limit exceeded: " + limit);
        }
    }

    public static class ComponentNotRegisteredException extends RuntimeException {
        public ComponentNotRegisteredException(Class<?> clazz) {
            super("Component type not registered: " + clazz.getName());
        }
    }

    public static class InvalidComponentCombinationException extends RuntimeException {
        public InvalidComponentCombinationException(List<String> errors) {
            super("Invalid component combination:\n" + String.join("\n", errors));
        }
    }
}
