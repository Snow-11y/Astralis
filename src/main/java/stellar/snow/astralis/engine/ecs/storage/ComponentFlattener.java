package ecs.storage;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced component flattening for optimal Structure-of-Arrays (SoA) layout.
 * 
 * Superior to Kirino's ECS flattening with:
 * - 8 primitive types vs Kirino's 3 (byte, short, int, long, float, double, boolean, char)
 * - Nested struct flattening with full path tracking
 * - Automatic SIMD alignment (16/64 byte boundaries)
 * - MethodHandle-based access (zero reflection overhead)
 * - Optimal layout calculation for cache efficiency
 * - Memory layout optimization
 */
public class ComponentFlattener {
    
    public enum ScalarType {
        BYTE(1, Byte.TYPE, Byte.class),
        SHORT(2, Short.TYPE, Short.class),
        INT(4, Integer.TYPE, Integer.class),
        LONG(8, Long.TYPE, Long.class),
        FLOAT(4, Float.TYPE, Float.class),
        DOUBLE(8, Double.TYPE, Double.class),
        BOOLEAN(1, Boolean.TYPE, Boolean.class),
        CHAR(2, Character.TYPE, Character.class);
        
        public final int size;
        public final Class<?> primitiveType;
        public final Class<?> wrapperType;
        
        ScalarType(int size, Class<?> primitiveType, Class<?> wrapperType) {
            this.size = size;
            this.primitiveType = primitiveType;
            this.wrapperType = wrapperType;
        }
        
        public static ScalarType fromClass(Class<?> clazz) {
            for (ScalarType type : values()) {
                if (type.primitiveType == clazz || type.wrapperType == clazz) {
                    return type;
                }
            }
            return null;
        }
        
        public int getSIMDAlignment() {
            // Align floats/doubles to 16 bytes for SSE, 64 for AVX-512
            if (this == FLOAT || this == DOUBLE) {
                return 64;
            }
            return 16;
        }
    }
    
    /**
     * Describes a flattened component layout
     */
    public static class FlattenedDescriptor {
        public final Class<?> componentClass;
        public final List<FieldDescriptor> fields;
        public final Map<ScalarType, Integer> counts;
        public final int totalSize;
        public final int alignment;
        
        public FlattenedDescriptor(Class<?> componentClass, List<FieldDescriptor> fields) {
            this.componentClass = componentClass;
            this.fields = Collections.unmodifiableList(fields);
            this.counts = calculateCounts();
            this.totalSize = calculateTotalSize();
            this.alignment = calculateAlignment();
        }
        
        private Map<ScalarType, Integer> calculateCounts() {
            Map<ScalarType, Integer> result = new EnumMap<>(ScalarType.class);
            for (FieldDescriptor field : fields) {
                result.merge(field.scalarType, 1, Integer::sum);
            }
            return Collections.unmodifiableMap(result);
        }
        
        private int calculateTotalSize() {
            return fields.stream()
                .mapToInt(f -> f.scalarType.size)
                .sum();
        }
        
        private int calculateAlignment() {
            return fields.stream()
                .mapToInt(f -> f.scalarType.getSIMDAlignment())
                .max()
                .orElse(8);
        }
    }
    
    /**
     * Describes a single flattened field
     */
    public static class FieldDescriptor {
        public final String path;              // e.g., "position.x"
        public final String flatName;          // e.g., "position_x"
        public final ScalarType scalarType;
        public final MethodHandle getter;
        public final MethodHandle setter;
        public final int offset;               // Offset in flattened layout
        
        public FieldDescriptor(String path, String flatName, ScalarType scalarType,
                             MethodHandle getter, MethodHandle setter, int offset) {
            this.path = path;
            this.flatName = flatName;
            this.scalarType = scalarType;
            this.getter = getter;
            this.setter = setter;
            this.offset = offset;
        }
    }
    
    private static final Map<Class<?>, FlattenedDescriptor> DESCRIPTOR_CACHE = 
        new ConcurrentHashMap<>();
    
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    
    /**
     * Flatten a component class into optimal SoA layout.
     * Results are cached for performance.
     */
    public static FlattenedDescriptor flatten(Class<?> componentClass) {
        return DESCRIPTOR_CACHE.computeIfAbsent(componentClass, ComponentFlattener::flattenImpl);
    }
    
    private static FlattenedDescriptor flattenImpl(Class<?> clazz) {
        List<FieldDescriptor> fields = new ArrayList<>();
        flattenRecursive(clazz, "", null, fields, 0);
        
        // Sort by SIMD alignment requirements (largest first) for optimal layout
        fields.sort(Comparator
            .comparingInt((FieldDescriptor f) -> -f.scalarType.getSIMDAlignment())
            .thenComparingInt(f -> -f.scalarType.size));
        
        // Recalculate offsets after sorting
        int offset = 0;
        List<FieldDescriptor> reorderedFields = new ArrayList<>();
        for (FieldDescriptor field : fields) {
            // Align to SIMD boundaries
            int alignment = field.scalarType.getSIMDAlignment();
            offset = (offset + alignment - 1) & ~(alignment - 1);
            
            reorderedFields.add(new FieldDescriptor(
                field.path, field.flatName, field.scalarType,
                field.getter, field.setter, offset
            ));
            
            offset += field.scalarType.size;
        }
        
        return new FlattenedDescriptor(clazz, reorderedFields);
    }
    
    private static void flattenRecursive(Class<?> clazz, String prefix, 
                                        MethodHandle parentGetter,
                                        List<FieldDescriptor> fields, int depth) {
        if (depth > 10) {
            throw new IllegalArgumentException("Nesting depth too deep: " + clazz);
        }
        
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || 
                Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            
            Class<?> fieldType = field.getType();
            String path = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();
            String flatName = path.replace('.', '_');
            
            ScalarType scalarType = ScalarType.fromClass(fieldType);
            
            try {
                field.setAccessible(true);
                MethodHandle getter = LOOKUP.unreflectGetter(field);
                MethodHandle setter = LOOKUP.unreflectSetter(field);
                
                // Chain parent getter if needed
                if (parentGetter != null) {
                    getter = MethodHandles.filterArguments(getter, 0, parentGetter);
                    setter = MethodHandles.filterArguments(setter, 0, parentGetter);
                }
                
                if (scalarType != null) {
                    // Primitive field - add to flat list
                    fields.add(new FieldDescriptor(path, flatName, scalarType, getter, setter, 0));
                } else if (!fieldType.isPrimitive() && !fieldType.isArray()) {
                    // Nested struct - flatten recursively
                    flattenRecursive(fieldType, path, getter, fields, depth + 1);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access field: " + path, e);
            }
        }
    }
    
    /**
     * Flatten a component instance into typed arrays.
     * Returns a map of ScalarType -> array of values
     */
    public static Map<ScalarType, Object> flattenComponent(Object component) {
        FlattenedDescriptor desc = flatten(component.getClass());
        Map<ScalarType, List<Object>> tempMap = new EnumMap<>(ScalarType.class);
        
        for (ScalarType type : ScalarType.values()) {
            tempMap.put(type, new ArrayList<>());
        }
        
        try {
            for (FieldDescriptor field : desc.fields) {
                Object value = field.getter.invoke(component);
                tempMap.get(field.scalarType).add(value);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to flatten component", e);
        }
        
        // Convert to primitive arrays
        Map<ScalarType, Object> result = new EnumMap<>(ScalarType.class);
        for (Map.Entry<ScalarType, List<Object>> entry : tempMap.entrySet()) {
            ScalarType type = entry.getKey();
            List<Object> values = entry.getValue();
            if (values.isEmpty()) continue;
            
            result.put(type, convertToArray(type, values));
        }
        
        return result;
    }
    
    private static Object convertToArray(ScalarType type, List<Object> values) {
        int size = values.size();
        switch (type) {
            case BYTE:
                byte[] bytes = new byte[size];
                for (int i = 0; i < size; i++) bytes[i] = (Byte) values.get(i);
                return bytes;
            case SHORT:
                short[] shorts = new short[size];
                for (int i = 0; i < size; i++) shorts[i] = (Short) values.get(i);
                return shorts;
            case INT:
                int[] ints = new int[size];
                for (int i = 0; i < size; i++) ints[i] = (Integer) values.get(i);
                return ints;
            case LONG:
                long[] longs = new long[size];
                for (int i = 0; i < size; i++) longs[i] = (Long) values.get(i);
                return longs;
            case FLOAT:
                float[] floats = new float[size];
                for (int i = 0; i < size; i++) floats[i] = (Float) values.get(i);
                return floats;
            case DOUBLE:
                double[] doubles = new double[size];
                for (int i = 0; i < size; i++) doubles[i] = (Double) values.get(i);
                return doubles;
            case BOOLEAN:
                boolean[] booleans = new boolean[size];
                for (int i = 0; i < size; i++) booleans[i] = (Boolean) values.get(i);
                return booleans;
            case CHAR:
                char[] chars = new char[size];
                for (int i = 0; i < size; i++) chars[i] = (Character) values.get(i);
                return chars;
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }
    
    /**
     * Reconstruct a component from flattened arrays.
     */
    public static <T> T reconstructComponent(Class<T> componentClass, 
                                            Map<ScalarType, Object> flattenedData) {
        FlattenedDescriptor desc = flatten(componentClass);
        
        try {
            T instance = componentClass.getDeclaredConstructor().newInstance();
            
            Map<ScalarType, Integer> indices = new EnumMap<>(ScalarType.class);
            for (ScalarType type : ScalarType.values()) {
                indices.put(type, 0);
            }
            
            for (FieldDescriptor field : desc.fields) {
                Object array = flattenedData.get(field.scalarType);
                if (array == null) continue;
                
                int index = indices.get(field.scalarType);
                Object value = getArrayValue(array, index, field.scalarType);
                
                // Set the field value
                field.setter.invoke(instance, value);
                
                indices.put(field.scalarType, index + 1);
            }
            
            return instance;
        } catch (Throwable e) {
            throw new RuntimeException("Failed to reconstruct component", e);
        }
    }
    
    private static Object getArrayValue(Object array, int index, ScalarType type) {
        switch (type) {
            case BYTE: return ((byte[]) array)[index];
            case SHORT: return ((short[]) array)[index];
            case INT: return ((int[]) array)[index];
            case LONG: return ((long[]) array)[index];
            case FLOAT: return ((float[]) array)[index];
            case DOUBLE: return ((double[]) array)[index];
            case BOOLEAN: return ((boolean[]) array)[index];
            case CHAR: return ((char[]) array)[index];
            default: throw new IllegalArgumentException("Unknown type: " + type);
        }
    }
    
    /**
     * Calculate optimal memory layout size for N entities.
     * Includes SIMD alignment padding.
     */
    public static long calculateLayoutSize(Class<?> componentClass, int entityCount) {
        FlattenedDescriptor desc = flatten(componentClass);
        long totalSize = 0;
        
        for (FieldDescriptor field : desc.fields) {
            int alignment = field.scalarType.getSIMDAlignment();
            int elementSize = field.scalarType.size;
            
            // Align array start
            totalSize = (totalSize + alignment - 1) & ~(alignment - 1);
            
            // Add array size
            totalSize += (long) elementSize * entityCount;
        }
        
        // Final alignment
        int maxAlignment = desc.alignment;
        totalSize = (totalSize + maxAlignment - 1) & ~(maxAlignment - 1);
        
        return totalSize;
    }
    
    /**
     * Print flattened layout for debugging
     */
    public static void printLayout(Class<?> componentClass) {
        FlattenedDescriptor desc = flatten(componentClass);
        System.out.println("Component: " + componentClass.getSimpleName());
        System.out.println("Total Size: " + desc.totalSize + " bytes");
        System.out.println("Alignment: " + desc.alignment + " bytes");
        System.out.println("\nFields:");
        
        for (FieldDescriptor field : desc.fields) {
            System.out.printf("  [%4d] %-20s %-10s (align: %2d)\n",
                field.offset, field.flatName, field.scalarType, 
                field.scalarType.getSIMDAlignment());
        }
        
        System.out.println("\nType Counts:");
        desc.counts.forEach((type, count) -> 
            System.out.printf("  %-10s: %d\n", type, count));
    }
}
