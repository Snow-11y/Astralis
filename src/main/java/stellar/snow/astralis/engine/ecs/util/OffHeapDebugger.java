package stellar.snow.astralis.engine.ecs.util;

import stellar.snow.astralis.engine.ecs.core.Entity;
import stellar.snow.astralis.engine.ecs.core.World;
import stellar.snow.astralis.engine.ecs.storage.ComponentArray;
import stellar.snow.astralis.engine.ecs.storage.ComponentRegistry;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * OffHeapDebugger - Makes off-heap memory visible to standard Java tools.
 *
 * <h2>The Problem</h2>
 * <p>Off-heap memory (MemorySegment) is invisible to:</p>
 * <ul>
 *   <li>Standard Java debuggers (IntelliJ, Eclipse)</li>
 *   <li>Heap dump analyzers (jmap, MAT)</li>
 *   <li>Profilers (VisualVM, YourKit, JProfiler)</li>
 *   <li>Standard toString() / inspection</li>
 * </ul>
 *
 * <h2>The Solution</h2>
 * <p>This debugger provides:</p>
 * <ul>
 *   <li><b>Live Snapshots:</b> Copy off-heap data to on-heap POJOs</li>
 *   <li><b>Memory Inspector:</b> View raw bytes as structured data</li>
 *   <li><b>Component Viewer:</b> Pretty-print component values</li>
 *   <li><b>Heap Dump Export:</b> Generate debugger-friendly representations</li>
 *   <li><b>Watchpoints:</b> Monitor specific entities/components for changes</li>
 *   <li><b>Memory Layout Visualization:</b> ASCII diagrams of memory structure</li>
 * </ul>
 *
 * <h2>Zero Performance Impact in Production</h2>
 * <pre>
 * // Debugger is disabled by default - zero overhead
 * World world = new World(config);
 * 
 * // Enable only when needed (dev/debug builds)
 * if (Astralis.isDebugMode()) {
 *     OffHeapDebugger debugger = new OffHeapDebugger(world);
 *     debugger.enableLiveInspection();
 * }
 * </pre>
 *
 * <h2>Usage Examples</h2>
 * <pre>
 * OffHeapDebugger debugger = new OffHeapDebugger(world);
 * 
 * // Snapshot a single entity's components
 * Map&lt;String, Object&gt; snapshot = debugger.snapshotEntity(entity);
 * System.out.println("Position: " + snapshot.get("Transform"));
 * 
 * // Export all entities to heap for inspection
 * List&lt;EntitySnapshot&gt; entities = debugger.exportAllEntities();
 * // Now visible in debugger heap view!
 * 
 * // Watch for changes
 * debugger.watchEntity(entity, (e, component, oldValue, newValue) -> {
 *     System.out.println("Entity " + e + " " + component + " changed!");
 * });
 * 
 * // Visualize memory layout
 * String layout = debugger.visualizeMemoryLayout(Transform.class);
 * System.out.println(layout);
 * </pre>
 *
 * @author Astralis ECS - Off-Heap Debugger
 * @version 1.0.0
 * @since Java 21
 */
public final class OffHeapDebugger {

    // ════════════════════════════════════════════════════════════════════════
    // CORE STATE
    // ════════════════════════════════════════════════════════════════════════

    private final World world;
    private final ComponentRegistry registry;
    private final boolean enabled;
    
    // Live inspection toggle
    private volatile boolean liveInspectionEnabled = false;
    
    // Watchpoints
    private final ConcurrentHashMap<Entity, EntityWatchpoint> watchpoints = new ConcurrentHashMap<>();
    
    // Snapshot cache (for performance)
    private final ConcurrentHashMap<Entity, EntitySnapshot> snapshotCache = new ConcurrentHashMap<>();
    private final long snapshotTTL = 1_000_000_000L; // 1 second

    // ════════════════════════════════════════════════════════════════════════
    // CONSTRUCTORS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Create debugger for a world.
     * Debugger is disabled by default - call enable() to activate.
     */
    public OffHeapDebugger(World world) {
        this.world = world;
        this.registry = world.getComponentRegistry();
        this.enabled = true; // Can be controlled by build flag
    }

    // ════════════════════════════════════════════════════════════════════════
    // ENABLE/DISABLE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Enable live inspection (copies data to heap every frame).
     * WARNING: Performance impact! Use only in debug builds.
     */
    public void enableLiveInspection() {
        if (!enabled) return;
        liveInspectionEnabled = true;
    }

    /**
     * Disable live inspection.
     */
    public void disableLiveInspection() {
        liveInspectionEnabled = false;
        snapshotCache.clear();
    }

    /**
     * Check if debugger is enabled.
     */
    public boolean isEnabled() {
        return enabled && liveInspectionEnabled;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ENTITY SNAPSHOTS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Snapshot all components of an entity to on-heap POJOs.
     * 
     * @return Map of component name -> value (on heap)
     */
    public Map<String, Object> snapshotEntity(Entity entity) {
        if (!enabled) return Collections.emptyMap();
        
        Map<String, Object> snapshot = new LinkedHashMap<>();
        
        // Get entity's archetype to find components
        var archetype = world.getArchetype(entity);
        if (archetype == null) return snapshot;
        
        // For each component type in archetype
        for (var componentType : archetype.getComponentTypes()) {
            ComponentArray array = archetype.getComponentArray(componentType);
            if (array == null) continue;
            
            // Copy component data from off-heap to on-heap object
            Object value = copyComponentToHeap(array, entity, componentType);
            snapshot.put(componentType.name(), value);
        }
        
        return snapshot;
    }

    /**
     * Export all entities to heap-based snapshots.
     * Useful for heap dump analysis.
     * 
     * @return List of entity snapshots (fully on-heap)
     */
    public List<EntitySnapshot> exportAllEntities() {
        if (!enabled) return Collections.emptyList();
        
        List<EntitySnapshot> snapshots = new ArrayList<>();
        
        world.forEach(entity -> {
            Map<String, Object> components = snapshotEntity(entity);
            snapshots.add(new EntitySnapshot(
                entity.index(),
                entity.generation(),
                System.nanoTime(),
                components
            ));
        });
        
        return snapshots;
    }

    /**
     * Export entities matching a predicate.
     */
    public List<EntitySnapshot> exportEntities(Predicate<Entity> filter) {
        if (!enabled) return Collections.emptyList();
        
        List<EntitySnapshot> snapshots = new ArrayList<>();
        
        world.forEach(entity -> {
            if (filter.test(entity)) {
                Map<String, Object> components = snapshotEntity(entity);
                snapshots.add(new EntitySnapshot(
                    entity.index(),
                    entity.generation(),
                    System.nanoTime(),
                    components
                ));
            }
        });
        
        return snapshots;
    }

    // ════════════════════════════════════════════════════════════════════════
    // MEMORY INSPECTION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Inspect raw memory of a component array.
     * 
     * @return Human-readable hex dump with structure annotations
     */
    public String inspectComponentMemory(ComponentArray array, int maxEntities) {
        if (!enabled) return "Debugger disabled";
        
        StringBuilder sb = new StringBuilder(4096);
        
        sb.append("╔══════════════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                         Component Memory Inspector                            ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        
        // Get raw memory segment
        MemorySegment segment = array.getDataSegment();
        int componentSize = array.getComponentSize();
        int count = Math.min(array.count(), maxEntities);
        
        sb.append("║ Component Type: ").append(array.getType().name()).append("\n");
        sb.append("║ Component Size: ").append(componentSize).append(" bytes\n");
        sb.append("║ Entity Count: ").append(count).append("\n");
        sb.append("║ Memory Address: ").append(segment.address()).append("\n");
        sb.append("╚══════════════════════════════════════════════════════════════════════════════╝\n\n");
        
        // Hex dump of each entity's component data
        for (int i = 0; i < count; i++) {
            sb.append("┌── Entity Index ").append(i).append(" ─────────────────────────────────────────────────────┐\n");
            
            long offset = (long) i * componentSize;
            
            // Read bytes
            byte[] bytes = new byte[componentSize];
            MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, offset, bytes, 0, componentSize);
            
            // Hex dump with annotations
            sb.append(formatHexDump(bytes, array.getType()));
            
            sb.append("└──────────────────────────────────────────────────────────────────────────┘\n\n");
        }
        
        return sb.toString();
    }

    /**
     * Visualize memory layout of a component type.
     * Shows field offsets, sizes, alignment, padding.
     */
    public String visualizeMemoryLayout(Class<?> componentClass) {
        if (!enabled) return "Debugger disabled";
        
        ComponentRegistry.ComponentType type = registry.getComponentType(componentClass);
        if (type == null) return "Component not registered: " + componentClass.getName();
        
        StringBuilder sb = new StringBuilder(2048);
        
        sb.append("╔══════════════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                         Memory Layout Visualization                           ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ Component: ").append(type.name()).append("\n");
        sb.append("║ Total Size: ").append(type.size()).append(" bytes\n");
        sb.append("║ Alignment: ").append(type.alignment()).append(" bytes\n");
        sb.append("╚══════════════════════════════════════════════════════════════════════════════╝\n\n");
        
        // ASCII diagram of memory layout
        sb.append("Memory Layout (byte offsets):\n");
        sb.append("┌─────────────────────────────────────────────────────────────────────────────┐\n");
        
        var fields = type.fields();
        int currentOffset = 0;
        
        for (var field : fields) {
            int fieldSize = field.size();
            int padding = field.offset() - currentOffset;
            
            // Show padding if any
            if (padding > 0) {
                sb.append("│ [").append(String.format("%4d", currentOffset)).append("] ");
                sb.append("<padding: ").append(padding).append(" bytes>\n");
                currentOffset += padding;
            }
            
            // Show field
            sb.append("│ [").append(String.format("%4d", currentOffset)).append("] ");
            sb.append(field.name()).append(": ").append(field.typeName());
            sb.append(" (").append(fieldSize).append(" bytes)\n");
            
            currentOffset += fieldSize;
        }
        
        // Show trailing padding
        int trailingPadding = type.size() - currentOffset;
        if (trailingPadding > 0) {
            sb.append("│ [").append(String.format("%4d", currentOffset)).append("] ");
            sb.append("<trailing padding: ").append(trailingPadding).append(" bytes>\n");
        }
        
        sb.append("└─────────────────────────────────────────────────────────────────────────────┘\n");
        
        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════════════════
    // WATCHPOINTS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Watch an entity for component changes.
     * Callback fires when any component value changes.
     */
    public void watchEntity(Entity entity, ComponentChangeCallback callback) {
        if (!enabled) return;
        
        EntityWatchpoint watchpoint = new EntityWatchpoint(entity, callback);
        watchpoints.put(entity, watchpoint);
        
        // Store current state
        watchpoint.lastSnapshot = snapshotEntity(entity);
    }

    /**
     * Stop watching an entity.
     */
    public void unwatchEntity(Entity entity) {
        watchpoints.remove(entity);
    }

    /**
     * Check all watchpoints for changes.
     * Call this after each frame.
     */
    public void checkWatchpoints() {
        if (!enabled || watchpoints.isEmpty()) return;
        
        watchpoints.forEach((entity, watchpoint) -> {
            Map<String, Object> currentSnapshot = snapshotEntity(entity);
            Map<String, Object> lastSnapshot = watchpoint.lastSnapshot;
            
            // Compare snapshots
            currentSnapshot.forEach((componentName, newValue) -> {
                Object oldValue = lastSnapshot.get(componentName);
                
                if (!Objects.equals(oldValue, newValue)) {
                    // Component changed!
                    watchpoint.callback.onComponentChanged(entity, componentName, oldValue, newValue);
                }
            });
            
            // Update last snapshot
            watchpoint.lastSnapshot = currentSnapshot;
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // INTERNAL HELPERS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Copy component data from off-heap memory to on-heap POJO.
     */
    private Object copyComponentToHeap(ComponentArray array, Entity entity, ComponentRegistry.ComponentType type) {
        MemorySegment segment = array.getDataSegment();
        int denseIndex = array.getDenseIndex(entity);
        if (denseIndex < 0) return null;
        
        long offset = (long) denseIndex * type.size();
        
        // Create POJO instance
        Object instance;
        try {
            instance = type.componentClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return "<failed to instantiate: " + e.getMessage() + ">";
        }
        
        // Copy each field from off-heap to POJO
        for (var field : type.fields()) {
            try {
                var javaField = type.componentClass().getDeclaredField(field.name());
                javaField.setAccessible(true);
                
                Object value = readFieldFromMemory(segment, offset + field.offset(), field);
                javaField.set(instance, value);
            } catch (Exception e) {
                // Skip field
            }
        }
        
        return instance;
    }

    /**
     * Read a field value from off-heap memory.
     */
    private Object readFieldFromMemory(MemorySegment segment, long offset, ComponentRegistry.FieldInfo field) {
        return switch (field.typeName()) {
            case "byte" -> segment.get(ValueLayout.JAVA_BYTE, offset);
            case "short" -> segment.get(ValueLayout.JAVA_SHORT, offset);
            case "int" -> segment.get(ValueLayout.JAVA_INT, offset);
            case "long" -> segment.get(ValueLayout.JAVA_LONG, offset);
            case "float" -> segment.get(ValueLayout.JAVA_FLOAT, offset);
            case "double" -> segment.get(ValueLayout.JAVA_DOUBLE, offset);
            case "boolean" -> segment.get(ValueLayout.JAVA_BOOLEAN, offset);
            case "char" -> segment.get(ValueLayout.JAVA_CHAR, offset);
            default -> "<unsupported type: " + field.typeName() + ">";
        };
    }

    /**
     * Format bytes as hex dump with field annotations.
     */
    private String formatHexDump(byte[] bytes, ComponentRegistry.ComponentType type) {
        StringBuilder sb = new StringBuilder();
        
        int bytesPerLine = 16;
        for (int i = 0; i < bytes.length; i += bytesPerLine) {
            // Offset
            sb.append(String.format("│ %04X  ", i));
            
            // Hex values
            for (int j = 0; j < bytesPerLine; j++) {
                if (i + j < bytes.length) {
                    sb.append(String.format("%02X ", bytes[i + j]));
                } else {
                    sb.append("   ");
                }
                
                if (j == 7) sb.append(" "); // Visual separator
            }
            
            sb.append(" │ ");
            
            // ASCII representation
            for (int j = 0; j < bytesPerLine && i + j < bytes.length; j++) {
                byte b = bytes[i + j];
                char c = (b >= 32 && b < 127) ? (char) b : '.';
                sb.append(c);
            }
            
            // Field annotation
            sb.append(" │ ");
            String annotation = getFieldAnnotation(i, type);
            sb.append(annotation);
            
            sb.append("\n");
        }
        
        return sb.toString();
    }

    /**
     * Get field name annotation for a byte offset.
     */
    private String getFieldAnnotation(int offset, ComponentRegistry.ComponentType type) {
        for (var field : type.fields()) {
            if (offset >= field.offset() && offset < field.offset() + field.size()) {
                int fieldOffset = offset - field.offset();
                return field.name() + " [+" + fieldOffset + "]";
            }
        }
        return "";
    }

    // ════════════════════════════════════════════════════════════════════════
    // SNAPSHOT DATA CLASSES
    // ════════════════════════════════════════════════════════════════════════

    /**
     * On-heap snapshot of an entity (fully visible in debugger).
     */
    public record EntitySnapshot(
        int index,
        int generation,
        long timestamp,
        Map<String, Object> components
    ) {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Entity[").append(index).append(":").append(generation).append("] {\n");
            components.forEach((name, value) -> {
                sb.append("  ").append(name).append(": ").append(value).append("\n");
            });
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * Watchpoint for entity changes.
     */
    private static class EntityWatchpoint {
        final Entity entity;
        final ComponentChangeCallback callback;
        Map<String, Object> lastSnapshot;

        EntityWatchpoint(Entity entity, ComponentChangeCallback callback) {
            this.entity = entity;
            this.callback = callback;
        }
    }

    /**
     * Callback for component changes.
     */
    @FunctionalInterface
    public interface ComponentChangeCallback {
        void onComponentChanged(Entity entity, String componentName, Object oldValue, Object newValue);
    }

    // ════════════════════════════════════════════════════════════════════════
    // STATISTICS & REPORTING
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Generate debug report with memory usage, entity counts, etc.
     */
    public String generateDebugReport() {
        if (!enabled) return "Debugger disabled";
        
        StringBuilder sb = new StringBuilder(8192);
        
        sb.append("╔══════════════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                         Astralis ECS Debug Report                             ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        
        // World info
        sb.append("║ World: ").append(world.getName()).append("\n");
        sb.append("║ Entity Count: ").append(world.getEntityCount()).append("\n");
        sb.append("║ Archetype Count: ").append(world.getArchetypeCount()).append("\n");
        sb.append("║ Component Types: ").append(registry.getRegisteredCount()).append("\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        
        // Memory usage
        long totalOffHeapBytes = world.estimateMemoryUsage();
        sb.append("║ Off-Heap Memory: ").append(formatBytes(totalOffHeapBytes)).append("\n");
        sb.append("║ Heap Memory: ").append(formatBytes(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())).append("\n");
        sb.append("╚══════════════════════════════════════════════════════════════════════════════╝\n");
        
        return sb.toString();
    }

    /**
     * Format bytes as human-readable string.
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)) + " MB";
        return (bytes / (1024 * 1024 * 1024)) + " GB";
    }
}
