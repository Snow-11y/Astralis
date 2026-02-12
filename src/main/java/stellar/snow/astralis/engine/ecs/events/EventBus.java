package stellar.snow.astralis.engine.ecs.events;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * EventBus - Type-safe event system for decoupled communication.
 * 
 * <p>Allows systems and entities to communicate without direct dependencies.
 * Events are dispatched to all registered listeners of that event type.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Define event
 * public record DamageEvent(Entity target, float amount) {}
 * 
 * // Register listener
 * eventBus.subscribe(DamageEvent.class, event -> {
 *     System.out.println("Damage: " + event.amount());
 * });
 * 
 * // Dispatch event
 * eventBus.publish(new DamageEvent(entity, 50));
 * </pre>
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Type-safe event dispatch</li>
 *   <li>Thread-safe registration</li>
 *   <li>Immediate and queued dispatch</li>
 *   <li>Event filtering</li>
 *   <li>Listener priority</li>
 * </ul>
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public class EventBus {
    
    private final Map<Class<?>, List<ListenerWrapper<?>>> listeners;
    private final Queue<Object> eventQueue;
    private final boolean threadSafe;
    
    /**
     * Create thread-safe event bus.
     */
    public EventBus() {
        this(true);
    }
    
    /**
     * Create event bus with optional thread safety.
     */
    public EventBus(boolean threadSafe) {
        this.threadSafe = threadSafe;
        this.listeners = new ConcurrentHashMap<>();
        this.eventQueue = new ArrayDeque<>();
    }
    
    // ========================================================================
    // SUBSCRIPTION
    // ========================================================================
    
    /**
     * Subscribe to events of a specific type.
     */
    public <T> Subscription subscribe(Class<T> eventType, Consumer<T> listener) {
        return subscribe(eventType, listener, 0);
    }
    
    /**
     * Subscribe to events with priority (higher = earlier).
     */
    public <T> Subscription subscribe(Class<T> eventType, Consumer<T> listener, int priority) {
        ListenerWrapper<T> wrapper = new ListenerWrapper<>(listener, priority);
        
        List<ListenerWrapper<?>> eventListeners = listeners.computeIfAbsent(
            eventType,
            k -> threadSafe ? new CopyOnWriteArrayList<>() : new ArrayList<>()
        );
        
        eventListeners.add(wrapper);
        
        // Sort by priority (descending)
        if (eventListeners.size() > 1) {
            eventListeners.sort((a, b) -> Integer.compare(b.priority, a.priority));
        }
        
        return new Subscription(this, eventType, wrapper);
    }
    
    /**
     * Unsubscribe from events.
     */
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        List<ListenerWrapper<?>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.removeIf(wrapper -> wrapper.listener == listener);
        }
    }
    
    /**
     * Unsubscribe all listeners for an event type.
     */
    public void unsubscribeAll(Class<?> eventType) {
        listeners.remove(eventType);
    }
    
    /**
     * Unsubscribe all listeners.
     */
    public void unsubscribeAll() {
        listeners.clear();
    }
    
    // ========================================================================
    // PUBLISHING
    // ========================================================================
    
    /**
     * Publish event immediately to all listeners.
     */
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        if (event == null) {
            return;
        }
        
        Class<?> eventType = event.getClass();
        List<ListenerWrapper<?>> eventListeners = listeners.get(eventType);
        
        if (eventListeners != null) {
            for (ListenerWrapper<?> wrapper : eventListeners) {
                try {
                    ((Consumer<T>) wrapper.listener).accept(event);
                } catch (Exception e) {
                    handleListenerError(event, e);
                }
            }
        }
    }
    
    /**
     * Queue event for later processing.
     */
    public <T> void queue(T event) {
        if (event != null) {
            synchronized (eventQueue) {
                eventQueue.offer(event);
            }
        }
    }
    
    /**
     * Process all queued events.
     */
    public void processQueue() {
        synchronized (eventQueue) {
            while (!eventQueue.isEmpty()) {
                Object event = eventQueue.poll();
                if (event != null) {
                    publish(event);
                }
            }
        }
    }
    
    /**
     * Get number of queued events.
     */
    public int getQueueSize() {
        synchronized (eventQueue) {
            return eventQueue.size();
        }
    }
    
    /**
     * Clear event queue without processing.
     */
    public void clearQueue() {
        synchronized (eventQueue) {
            eventQueue.clear();
        }
    }
    
    // ========================================================================
    // STATISTICS
    // ========================================================================
    
    /**
     * Get number of listeners for an event type.
     */
    public int getListenerCount(Class<?> eventType) {
        List<ListenerWrapper<?>> eventListeners = listeners.get(eventType);
        return eventListeners != null ? eventListeners.size() : 0;
    }
    
    /**
     * Get total number of registered listeners.
     */
    public int getTotalListenerCount() {
        return listeners.values().stream()
            .mapToInt(List::size)
            .sum();
    }
    
    /**
     * Get all registered event types.
     */
    public Set<Class<?>> getRegisteredEventTypes() {
        return Collections.unmodifiableSet(listeners.keySet());
    }
    
    // ========================================================================
    // ERROR HANDLING
    // ========================================================================
    
    /**
     * Handle listener errors (override to customize).
     */
    protected void handleListenerError(Object event, Exception error) {
        System.err.println("Error processing event " + event.getClass().getSimpleName() + ": " + error.getMessage());
        error.printStackTrace();
    }
    
    // ========================================================================
    // INTERNAL CLASSES
    // ========================================================================
    
    /**
     * Listener wrapper with priority.
     */
    private static class ListenerWrapper<T> {
        final Consumer<T> listener;
        final int priority;
        
        ListenerWrapper(Consumer<T> listener, int priority) {
            this.listener = listener;
            this.priority = priority;
        }
    }
    
    /**
     * Subscription handle for unsubscribing.
     */
    public static class Subscription {
        private final EventBus bus;
        private final Class<?> eventType;
        private final ListenerWrapper<?> wrapper;
        private boolean active;
        
        Subscription(EventBus bus, Class<?> eventType, ListenerWrapper<?> wrapper) {
            this.bus = bus;
            this.eventType = eventType;
            this.wrapper = wrapper;
            this.active = true;
        }
        
        /**
         * Cancel this subscription.
         */
        public void cancel() {
            if (active) {
                List<ListenerWrapper<?>> eventListeners = bus.listeners.get(eventType);
                if (eventListeners != null) {
                    eventListeners.remove(wrapper);
                }
                active = false;
            }
        }
        
        /**
         * Check if subscription is active.
         */
        public boolean isActive() {
            return active;
        }
    }
}
