package stellar.snow.astralis.engine.ecs.integration;

import stellar.snow.astralis.engine.ecs.core.ComponentScanner;
import stellar.snow.astralis.engine.ecs.core.World;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * ForgeIntegration - Event-driven registration for multi-mod ECS environments.
 *
 * <h2>The Multi-Mod Challenge</h2>
 * <p>In modding ecosystems like Forge, multiple mods load independently and need to:</p>
 * <ul>
 *   <li>Register components without conflicts</li>
 *   <li>Hook into initialization phases (pre-init, init, post-init)</li>
 *   <li>Discover other mods' components</li>
 *   <li>Maintain deterministic load order</li>
 * </ul>
 *
 * <h2>Event-Driven Solution</h2>
 * <p>ForgeIntegration provides lifecycle events for safe ECS initialization:</p>
 * <pre>
 * {@code @Mod.EventBusSubscriber}
 * public class MyModECSHandler {
 *     {@code @SubscribeEvent}
 *     public static void onECSPreInit(ECSPreInitEvent event) {
 *         // Register components before worlds are created
 *         ComponentScanner.scanModComponents(MyMod.class);
 *     }
 *     
 *     {@code @SubscribeEvent}
 *     public static void onECSInit(ECSInitEvent event) {
 *         // Add systems to the main world
 *         event.getWorld().addSystem(new MyCustomSystem());
 *     }
 * }
 * </pre>
 *
 * <h2>Load Phases</h2>
 * <ol>
 *   <li><b>PRE_INIT:</b> Component and struct registration</li>
 *   <li><b>INIT:</b> System registration and world setup</li>
 *   <li><b>POST_INIT:</b> Cross-mod integration and finalization</li>
 * </ol>
 *
 * <h2>Priority System</h2>
 * <p>Mods can specify load priority to control registration order:</p>
 * <pre>
 * {@code @Mod(modid = "mymod", dependencies = "required-after:ecs_core")}
 * {@code @ECSMod(priority = 100)}  // Higher priority loads first
 * public class MyMod {
 *     // ...
 * }
 * </pre>
 *
 * @author Enhanced ECS Framework (Surpassing Kirino)
 * @version 2.0.0
 * @since Java 21
 */
public final class ForgeIntegration {

    // ========================================================================
    // ANNOTATIONS
    // ========================================================================

    /**
     * Mark a mod as ECS-enabled for automatic discovery.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ECSMod {
        /** Load priority (higher = earlier) */
        int priority() default 0;
        /** Whether to auto-scan components */
        boolean autoScan() default true;
    }

    /**
     * Mark a method as an ECS event handler.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ECSEventHandler {
        /** Event phase */
        Phase phase() default Phase.INIT;
        /** Handler priority */
        int priority() default 0;
    }

    /**
     * ECS initialization phases.
     */
    public enum Phase {
        PRE_INIT,   // Component registration
        INIT,       // System registration
        POST_INIT   // Finalization
    }

    // ========================================================================
    // EVENT BUS
    // ========================================================================

    private static final Map<Phase, List<EventHandler>> EVENT_HANDLERS = new ConcurrentHashMap<>();
    private static final Set<Class<?>> REGISTERED_MODS = ConcurrentHashMap.newKeySet();

    static {
        for (Phase phase : Phase.values()) {
            EVENT_HANDLERS.put(phase, new CopyOnWriteArrayList<>());
        }
    }

    // ========================================================================
    // MOD REGISTRATION
    // ========================================================================

    /**
     * Register a mod with the ECS event system.
     */
    public static void registerMod(Class<?> modClass) {
        if (!REGISTERED_MODS.add(modClass)) {
            return; // Already registered
        }

        ECSMod annotation = modClass.getAnnotation(ECSMod.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Class must be annotated with @ECSMod: " + modClass.getName());
        }

        // Scan for event handler methods
        for (Method method : modClass.getDeclaredMethods()) {
            ECSEventHandler handler = method.getAnnotation(ECSEventHandler.class);
            if (handler != null) {
                registerEventHandler(modClass, method, handler);
            }
        }

        // Auto-scan components if enabled
        if (annotation.autoScan()) {
            ComponentScanner.scanModComponents(modClass);
        }

        System.out.println("[ECS Integration] Registered mod: " + modClass.getSimpleName());
    }

    /**
     * Register an event handler method.
     */
    private static void registerEventHandler(Class<?> modClass, Method method, ECSEventHandler annotation) {
        // Validate method signature
        Class<?>[] params = method.getParameterTypes();
        if (params.length != 1 || !ECSEvent.class.isAssignableFrom(params[0])) {
            throw new IllegalArgumentException(
                "Event handler must accept single ECSEvent parameter: " + 
                modClass.getName() + "." + method.getName()
            );
        }

        EventHandler handler = new EventHandler(
            modClass,
            method,
            annotation.phase(),
            annotation.priority()
        );

        EVENT_HANDLERS.get(annotation.phase()).add(handler);
        
        // Sort by priority (higher first)
        EVENT_HANDLERS.get(annotation.phase()).sort((a, b) -> 
            Integer.compare(b.priority, a.priority)
        );
    }

    // ========================================================================
    // EVENT DISPATCH
    // ========================================================================

    /**
     * Fire an event to all registered handlers in the specified phase.
     */
    public static void fireEvent(Phase phase, ECSEvent event) {
        List<EventHandler> handlers = EVENT_HANDLERS.get(phase);
        
        for (EventHandler handler : handlers) {
            try {
                handler.invoke(event);
            } catch (Exception e) {
                System.err.println("[ECS Integration] Event handler failed: " + 
                    handler.modClass.getName() + "." + handler.method.getName());
                e.printStackTrace();
            }
        }
    }

    /**
     * Run the complete ECS initialization pipeline.
     */
    public static void initializeECS(World world) {
        System.out.println("[ECS Integration] Starting ECS initialization...");

        // Phase 1: Pre-initialization (component registration)
        fireEvent(Phase.PRE_INIT, new ECSPreInitEvent(world));

        // Phase 2: Initialization (system registration)
        fireEvent(Phase.INIT, new ECSInitEvent(world));

        // Phase 3: Post-initialization (finalization)
        fireEvent(Phase.POST_INIT, new ECSPostInitEvent(world));

        System.out.println("[ECS Integration] ECS initialization complete!");
    }

    // ========================================================================
    // EVENT CLASSES
    // ========================================================================

    /**
     * Base ECS event.
     */
    public static abstract class ECSEvent {
        private final World world;

        protected ECSEvent(World world) {
            this.world = world;
        }

        public World getWorld() {
            return world;
        }
    }

    /**
     * Pre-initialization event for component registration.
     */
    public static final class ECSPreInitEvent extends ECSEvent {
        public ECSPreInitEvent(World world) {
            super(world);
        }
    }

    /**
     * Initialization event for system registration.
     */
    public static final class ECSInitEvent extends ECSEvent {
        public ECSInitEvent(World world) {
            super(world);
        }
    }

    /**
     * Post-initialization event for finalization.
     */
    public static final class ECSPostInitEvent extends ECSEvent {
        public ECSPostInitEvent(World world) {
            super(world);
        }
    }

    // ========================================================================
    // EVENT HANDLER
    // ========================================================================

    private static final class EventHandler {
        final Class<?> modClass;
        final Method method;
        final Phase phase;
        final int priority;

        EventHandler(Class<?> modClass, Method method, Phase phase, int priority) {
            this.modClass = modClass;
            this.method = method;
            this.phase = phase;
            this.priority = priority;
            
            method.setAccessible(true);
        }

        void invoke(ECSEvent event) throws Exception {
            // Assume static method
            method.invoke(null, event);
        }
    }

    // ========================================================================
    // COMPONENT CONFLICT RESOLUTION
    // ========================================================================

    /**
     * Registry for tracking component ownership by mods.
     */
    private static final Map<Class<?>, String> COMPONENT_OWNERS = new ConcurrentHashMap<>();

    /**
     * Register a component with its owning mod.
     */
    public static void registerComponentOwner(Class<?> componentClass, String modId) {
        String existing = COMPONENT_OWNERS.putIfAbsent(componentClass, modId);
        if (existing != null && !existing.equals(modId)) {
            System.err.println(
                "[ECS Integration] WARNING: Component conflict detected!\n" +
                "  Component: " + componentClass.getName() + "\n" +
                "  First registered by: " + existing + "\n" +
                "  Attempted re-registration by: " + modId
            );
        }
    }

    /**
     * Get the mod that owns a component.
     */
    public static String getComponentOwner(Class<?> componentClass) {
        return COMPONENT_OWNERS.get(componentClass);
    }

    // ========================================================================
    // CROSS-MOD QUERIES
    // ========================================================================

    /**
     * Find all mods that provide a specific component type.
     */
    public static List<String> findModsWithComponent(Class<?> componentClass) {
        List<String> mods = new ArrayList<>();
        for (Map.Entry<Class<?>, String> entry : COMPONENT_OWNERS.entrySet()) {
            if (componentClass.isAssignableFrom(entry.getKey())) {
                mods.add(entry.getValue());
            }
        }
        return mods;
    }

    /**
     * Check if a mod is registered.
     */
    public static boolean isModRegistered(Class<?> modClass) {
        return REGISTERED_MODS.contains(modClass);
    }

    /**
     * Get all registered mods.
     */
    public static Set<Class<?>> getRegisteredMods() {
        return Set.copyOf(REGISTERED_MODS);
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    public static String describe() {
        int totalHandlers = EVENT_HANDLERS.values().stream()
            .mapToInt(List::size)
            .sum();

        return String.format(
            "ForgeIntegration[mods=%d, handlers=%d, components=%d]",
            REGISTERED_MODS.size(),
            totalHandlers,
            COMPONENT_OWNERS.size()
        );
    }

    // Prevent instantiation
    private ForgeIntegration() {}
}
