package stellar.snow.astralis.engine.ecs.core;

import stellar.snow.astralis.Astralis;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Category;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * SystemScheduler - High-performance system execution with virtual threads and structured concurrency.
 * 
 * <p>Modern features:</p>
 * <ul>
 *   <li>Virtual thread execution for massive parallelism</li>
 *   <li>Structured concurrency with proper cancellation</li>
 *   <li>Lock-free dependency tracking</li>
 *   <li>JFR profiling integration</li>
 *   <li>Work-stealing load balancing</li>
 *   <li>Tarjan cycle detection</li>
 * </ul>
 */
public final class SystemScheduler {
    
    public enum Stage {
        PRE_UPDATE,
        UPDATE,
        POST_UPDATE,
        RENDER_PREPARE,
        RENDER
    }
    
    /**
     * JFR event for system execution profiling.
     */
    @Category("ECS")
    @Label("System Execution")
    private static final class SystemExecutionEvent extends Event {
        @Label("System Name") String systemName;
        @Label("Stage") String stage;
        @Label("Duration Nanos") long durationNanos;
        @Label("Entity Count") int entityCount;
    }
    
    /**
     * System registration with concurrent-safe metadata.
     */
    private static final class SystemEntry {
        final System system;
        final Stage stage;
        final Set<String> dependencies = ConcurrentHashMap.newKeySet();
        final Set<String> dependents = ConcurrentHashMap.newKeySet();
        final LongAdder executionTime = new LongAdder();
        volatile boolean completed;
        volatile boolean executing;
        
        SystemEntry(System system, Stage stage) {
            this.system = system;
            this.stage = stage;
        }
    }
    
    /**
     * Dependency graph node for Tarjan's algorithm.
     */
    private static final class GraphNode {
        final SystemEntry entry;
        int index = -1;
        int lowLink = -1;
        boolean onStack = false;
        
        GraphNode(SystemEntry entry) {
            this.entry = entry;
        }
    }
    
    private final ConcurrentHashMap<String, SystemEntry> systems = new ConcurrentHashMap<>();
    private final Map<Stage, List<SystemEntry>> stageOrder = new EnumMap<>(Stage.class);
    private final ExecutorService executor;
    private final ForkJoinPool workStealingPool;
    private final int parallelism;
    private final boolean useVirtualThreads;
    
    private volatile boolean orderDirty = true;
    private volatile boolean profilingEnabled = false;
    
    /**
     * Create scheduler with virtual threads (Java 21+) or fallback to platform threads.
     */
    public SystemScheduler(int parallelism, boolean preferVirtualThreads) {
        this.parallelism = parallelism;
        this.useVirtualThreads = preferVirtualThreads && isVirtualThreadsSupported();
        
        if (useVirtualThreads) {
            this.executor = Executors.newVirtualThreadPerTaskExecutor();
            Astralis.LOGGER.info("[ECS] Using virtual threads for system execution");
        } else {
            this.executor = Executors.newFixedThreadPool(parallelism, r -> {
                Thread t = new Thread(r, "ECS-Worker-" + System.nanoTime());
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY + 1);
                return t;
            });
        }
        
        // Work-stealing pool for fine-grained parallelism
        this.workStealingPool = new ForkJoinPool(
            parallelism,
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null,
            true // asyncMode for better throughput
        );
        
        for (Stage stage : Stage.values()) {
            stageOrder.put(stage, new CopyOnWriteArrayList<>());
        }
    }
    
    public SystemScheduler(int parallelism) {
        this(parallelism, true);
    }
    
    public SystemScheduler() {
        this(Math.max(2, Runtime.getRuntime().availableProcessors() - 1), true);
    }
    
    /**
     * Check if virtual threads are available (Java 21+).
     */
    private static boolean isVirtualThreadsSupported() {
        try {
            Class.forName("java.lang.VirtualThread");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public void register(System system, Stage stage) {
        SystemEntry entry = new SystemEntry(system, stage);
        systems.put(system.name, entry);
        orderDirty = true;
    }
    
    public void register(System system, Stage stage, String... dependencies) {
        SystemEntry entry = new SystemEntry(system, stage);
        entry.dependencies.addAll(Arrays.asList(dependencies));
        systems.put(system.name, entry);
        orderDirty = true;
    }
    
    public void addDependency(String systemName, String dependsOn) {
        SystemEntry entry = systems.get(systemName);
        SystemEntry depEntry = systems.get(dependsOn);
        
        if (entry != null && depEntry != null) {
            entry.dependencies.add(dependsOn);
            depEntry.dependents.add(systemName);
            orderDirty = true;
        }
    }
    
    public void unregister(String systemName) {
        SystemEntry entry = systems.remove(systemName);
        if (entry != null) {
            for (String dep : entry.dependencies) {
                SystemEntry depEntry = systems.get(dep);
                if (depEntry != null) {
                    depEntry.dependents.remove(systemName);
                }
            }
            orderDirty = true;
        }
    }
    
    public void initialize(World world) {
        rebuildOrder();
        
        // Parallel initialization where possible
        for (Stage stage : Stage.values()) {
            List<SystemEntry> entries = stageOrder.get(stage);
            if (entries.isEmpty()) continue;
            
            entries.parallelStream().forEach(entry -> {
                try {
                    entry.system.initialize(world);
                } catch (Exception e) {
                    Astralis.LOGGER.error("[ECS] Failed to initialize system {}: {}", 
                        entry.system.name, e.getMessage());
                }
            });
        }
    }
    
    public void executeStage(World world, Stage stage, float deltaTime) {
        if (orderDirty) {
            rebuildOrder();
        }
        
        List<SystemEntry> stageSystems = stageOrder.get(stage);
        if (stageSystems.isEmpty()) return;
        
        stageSystems.forEach(e -> {
            e.completed = false;
            e.executing = false;
        });
        
        if (parallelism > 1 && stageSystems.size() > 1) {
            executeParallelStructured(world, stageSystems, deltaTime);
        } else {
            executeSequential(world, stageSystems, deltaTime);
        }
    }
    
    public void executeAll(World world, float deltaTime) {
        for (Stage stage : Stage.values()) {
            executeStage(world, stage, deltaTime);
        }
    }
    
    private void executeSequential(World world, List<SystemEntry> entries, float deltaTime) {
        for (SystemEntry entry : entries) {
            if (!entry.system.enabled) {
                entry.completed = true;
                continue;
            }
            executeSystem(world, entry, deltaTime);
        }
    }
    
    /**
     * Structured concurrent execution with proper cancellation propagation.
     */
    private void executeParallelStructured(World world, List<SystemEntry> entries, float deltaTime) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Create dependency-aware task graph
            Map<String, StructuredTaskScope.Subtask<Void>> taskMap = new ConcurrentHashMap<>();
            
            for (SystemEntry entry : entries) {
                if (!entry.system.enabled) {
                    entry.completed = true;
                    continue;
                }
                
                StructuredTaskScope.Subtask<Void> task = scope.fork(() -> {
                    waitForDependencies(entry, taskMap);
                    executeSystem(world, entry, deltaTime);
                    entry.completed = true;
                    return null;
                });
                
                taskMap.put(entry.system.name, task);
            }
            
            scope.join();
            scope.throwIfFailed();
            
        } catch (InterruptedException e) {
            Astralis.LOGGER.warn("[ECS] System execution interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Astralis.LOGGER.error("[ECS] System execution failed: {}", e.getMessage());
        }
    }
    
    /**
     * Wait for dependencies with exponential backoff.
     */
    private void waitForDependencies(SystemEntry entry, Map<String, StructuredTaskScope.Subtask<Void>> taskMap) {
        if (entry.dependencies.isEmpty()) return;
        
        int backoff = 1;
        while (!allDependenciesComplete(entry)) {
            try {
                Thread.sleep(Math.min(backoff, 50));
                backoff *= 2;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
    
    private boolean allDependenciesComplete(SystemEntry entry) {
        for (String dep : entry.dependencies) {
            SystemEntry depEntry = systems.get(dep);
            if (depEntry != null && !depEntry.completed) {
                return false;
            }
        }
        return true;
    }
    
    private void executeSystem(World world, SystemEntry entry, float deltaTime) {
        if (entry.executing) return;
        entry.executing = true;
        
        System system = entry.system;
        long startTime = java.lang.System.nanoTime();
        int entityCount = 0;
        
        SystemExecutionEvent event = null;
        if (profilingEnabled) {
            event = new SystemExecutionEvent();
            event.systemName = system.name;
            event.stage = entry.stage.name();
            event.begin();
        }
        
        try {
            system.onBeforeUpdate(world, deltaTime);
            
            List<Archetype> matchingArchetypes = world.getArchetypes().stream()
                .filter(system::matchesArchetype)
                .toList();
            
            // Work-stealing parallel archetype processing for large systems
            if (matchingArchetypes.size() > 4 && parallelism > 1) {
                workStealingPool.submit(() ->
                    matchingArchetypes.parallelStream().forEach(archetype -> {
                        system.update(world, archetype, deltaTime);
                    })
                ).join();
            } else {
                for (Archetype archetype : matchingArchetypes) {
                    system.update(world, archetype, deltaTime);
                    entityCount += archetype.getEntityCount();
                }
            }
            
            system.onAfterUpdate(world, deltaTime);
            
        } catch (Exception e) {
            Astralis.LOGGER.error("[ECS] System {} failed: {}", system.name, e.getMessage());
            e.printStackTrace();
        }
        
        long endTime = java.lang.System.nanoTime();
        long duration = endTime - startTime;
        
        system.lastExecutionTimeNanos = duration;
        system.totalExecutionTimeNanos += duration;
        system.executionCount++;
        entry.executionTime.add(duration);
        
        if (event != null) {
            event.durationNanos = duration;
            event.entityCount = entityCount;
            event.commit();
        }
        
        entry.completed = true;
        entry.executing = false;
    }
    
    /**
     * Rebuild execution order using Tarjan's algorithm for cycle detection.
     */
    private void rebuildOrder() {
        for (Stage stage : Stage.values()) {
            stageOrder.get(stage).clear();
        }
        
        for (SystemEntry entry : systems.values()) {
            stageOrder.get(entry.stage).add(entry);
        }
        
        for (Stage stage : Stage.values()) {
            List<SystemEntry> stageSystems = stageOrder.get(stage);
            stageSystems.sort(Comparator.comparingInt(e -> e.system.priority));
        }
        
        // Tarjan's strongly connected components for cycle detection
        for (Stage stage : Stage.values()) {
            List<SystemEntry> sorted = tarjanTopologicalSort(stageOrder.get(stage));
            stageOrder.put(stage, sorted);
        }
        
        orderDirty = false;
    }
    
    /**
     * Tarjan's algorithm for topological sort with cycle detection.
     */
    private List<SystemEntry> tarjanTopologicalSort(List<SystemEntry> entries) {
        Map<String, GraphNode> nodes = entries.stream()
            .collect(Collectors.toMap(e -> e.system.name, GraphNode::new));
        
        Deque<GraphNode> stack = new ArrayDeque<>();
        List<List<GraphNode>> stronglyConnected = new ArrayList<>();
        AtomicInteger index = new AtomicInteger(0);
        
        for (GraphNode node : nodes.values()) {
            if (node.index == -1) {
                tarjanStrongConnect(node, nodes, stack, stronglyConnected, index);
            }
        }
        
        // Check for cycles
        for (List<GraphNode> component : stronglyConnected) {
            if (component.size() > 1) {
                String cycle = component.stream()
                    .map(n -> n.entry.system.name)
                    .collect(Collectors.joining(" -> "));
                Astralis.LOGGER.error("[ECS] Circular dependency detected: {}", cycle);
            }
        }
        
        // Topological sort
        List<SystemEntry> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        
        for (SystemEntry entry : entries) {
            if (!visited.contains(entry.system.name)) {
                topologicalVisit(entry, nodes, visited, result);
            }
        }
        
        return result;
    }
    
    private void tarjanStrongConnect(GraphNode node, Map<String, GraphNode> nodes,
                                     Deque<GraphNode> stack, List<List<GraphNode>> components,
                                     AtomicInteger index) {
        node.index = index.get();
        node.lowLink = index.get();
        index.incrementAndGet();
        stack.push(node);
        node.onStack = true;
        
        for (String depName : node.entry.dependencies) {
            GraphNode depNode = nodes.get(depName);
            if (depNode == null) continue;
            
            if (depNode.index == -1) {
                tarjanStrongConnect(depNode, nodes, stack, components, index);
                node.lowLink = Math.min(node.lowLink, depNode.lowLink);
            } else if (depNode.onStack) {
                node.lowLink = Math.min(node.lowLink, depNode.index);
            }
        }
        
        if (node.lowLink == node.index) {
            List<GraphNode> component = new ArrayList<>();
            GraphNode w;
            do {
                w = stack.pop();
                w.onStack = false;
                component.add(w);
            } while (w != node);
            components.add(component);
        }
    }
    
    private void topologicalVisit(SystemEntry entry, Map<String, GraphNode> nodes,
                                   Set<String> visited, List<SystemEntry> result) {
        if (visited.contains(entry.system.name)) return;
        visited.add(entry.system.name);
        
        for (String depName : entry.dependencies) {
            GraphNode depNode = nodes.get(depName);
            if (depNode != null) {
                topologicalVisit(depNode.entry, nodes, visited, result);
            }
        }
        
        result.add(entry);
    }
    
    public void shutdown(World world) {
        List<Stage> reverseStages = Arrays.asList(Stage.values());
        Collections.reverse(reverseStages);
        
        for (Stage stage : reverseStages) {
            List<SystemEntry> stageSystems = new ArrayList<>(stageOrder.get(stage));
            Collections.reverse(stageSystems);
            
            stageSystems.parallelStream().forEach(entry -> {
                try {
                    entry.system.shutdown(world);
                } catch (Exception e) {
                    Astralis.LOGGER.error("[ECS] Failed to shutdown system {}: {}",
                        entry.system.name, e.getMessage());
                }
            });
        }
        
        executor.shutdown();
        workStealingPool.shutdown();
        
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
            workStealingPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
            workStealingPool.shutdownNow();
        }
    }
    
    public System getSystem(String name) {
        SystemEntry entry = systems.get(name);
        return entry != null ? entry.system : null;
    }
    
    public List<System> getSystemsInStage(Stage stage) {
        return stageOrder.get(stage).stream()
            .map(e -> e.system)
            .toList();
    }
    
    public void enableProfiling(boolean enabled) {
        this.profilingEnabled = enabled;
    }
    
    public String getPerformanceReport() {
        StringBuilder sb = new StringBuilder("=== ECS System Performance Report ===\n");
        sb.append(String.format("Execution Mode: %s\n", 
            useVirtualThreads ? "Virtual Threads" : "Platform Threads"));
        sb.append(String.format("Parallelism: %d\n\n", parallelism));
        
        for (Stage stage : Stage.values()) {
            List<SystemEntry> stageSystems = stageOrder.get(stage);
            if (stageSystems.isEmpty()) continue;
            
            sb.append(stage).append(":\n");
            for (SystemEntry entry : stageSystems) {
                System s = entry.system;
                sb.append(String.format("  %-30s: %.3f ms (avg: %.3f ms, %d exec)%s\n",
                    s.name,
                    s.getLastExecutionTimeMs(),
                    s.getAverageExecutionTimeMs(),
                    s.executionCount,
                    s.enabled ? "" : " [DISABLED]"));
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    public Map<String, Double> getSystemMetrics() {
        return systems.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().system.getAverageExecutionTimeMs()
            ));
    }
}
