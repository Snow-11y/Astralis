package stellar.snow.astralis.engine.ecs.core;

import stellar.snow.astralis.engine.ecs.core.SnowySystem;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * SystemOrchestrator - Directed Acyclic Graph (DAG) based deterministic system execution.
 *
 * <h2>The Problem with Ad-Hoc Execution</h2>
 * <p>Traditional ECS schedulers have unclear execution order:</p>
 * <pre>
 * // What order do these run in?
 * scheduler.addSystem(physicsSystem);
 * scheduler.addSystem(collisionSystem);
 * scheduler.addSystem(renderSystem);
 * 
 * // Race conditions and non-determinism!
 * </pre>
 *
 * <h2>The DAG Solution</h2>
 * <p>SystemOrchestrator enforces explicit dependencies between systems:</p>
 * <pre>
 * orchestrator.addSystem(inputSystem);
 * orchestrator.addSystem(physicsSystem).after(inputSystem);
 * orchestrator.addSystem(collisionSystem).after(physicsSystem);
 * orchestrator.addSystem(renderSystem).after(collisionSystem);
 * 
 * // Execution order is crystal clear and deterministic
 * // Can also run independent branches in parallel
 * </pre>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Deterministic Flow:</b> Systems always run in the exact order specified</li>
 *   <li><b>Dependency Tracking:</b> Explicit before/after relationships</li>
 *   <li><b>Cycle Detection:</b> Prevents circular dependencies at build time</li>
 *   <li><b>Parallel Branches:</b> Independent system chains run concurrently</li>
 *   <li><b>Clear Debugging:</b> Visualize execution flow as a graph</li>
 *   <li><b>Hot-Reload Safe:</b> Can rebuild DAG at runtime</li>
 * </ul>
 *
 * <h2>Example: Game Loop Pipeline</h2>
 * <pre>
 * // Input phase
 * var input = orchestrator.addSystem(new InputSystem());
 * 
 * // Parallel simulation branches
 * var physics = orchestrator.addSystem(new PhysicsSystem()).after(input);
 * var ai = orchestrator.addSystem(new AISystem()).after(input);
 * 
 * // Converge for collision
 * var collision = orchestrator.addSystem(new CollisionSystem())
 *     .after(physics)
 *     .after(ai);
 * 
 * // Rendering depends on collision results
 * var render = orchestrator.addSystem(new RenderSystem()).after(collision);
 * 
 * // Execute entire pipeline
 * orchestrator.execute(world);
 * </pre>
 *
 * <h2>Performance</h2>
 * <ul>
 *   <li>DAG build: O(V + E) where V = systems, E = dependencies</li>
 *   <li>Cycle detection: O(V + E) using DFS</li>
 *   <li>Topological sort: O(V + E)</li>
 *   <li>Execution: Systems run in optimal parallel order</li>
 * </ul>
 *
 * @author Enhanced ECS Framework (Surpassing Kirino)
 * @version 2.0.0
 * @since Java 21
 */
public final class SystemOrchestrator {

    // ========================================================================
    // CORE STATE
    // ========================================================================

    /** All registered systems and their metadata */
    private final Map<String, SystemNode> systems = new LinkedHashMap<>();

    /** Topologically sorted execution order (cached) */
    private List<SystemNode> executionOrder = null;

    /** Whether the DAG needs to be rebuilt */
    private boolean dirty = true;

    /** Executor for parallel system execution */
    private final ForkJoinPool executor;

    /** Execution statistics */
    private final ExecutionStats stats = new ExecutionStats();

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    public SystemOrchestrator() {
        this(ForkJoinPool.commonPool());
    }

    public SystemOrchestrator(ForkJoinPool executor) {
        this.executor = executor;
    }

    // ========================================================================
    // SYSTEM REGISTRATION
    // ========================================================================

    /**
     * Add a system to the orchestrator.
     * Returns a builder for specifying dependencies.
     */
    public SystemBuilder addSystem(SnowySystem system) {
        return addSystem(system.getClass().getSimpleName(), system);
    }

    /**
     * Add a system with explicit ID.
     */
    public SystemBuilder addSystem(String id, SnowySystem system) {
        if (systems.containsKey(id)) {
            throw new IllegalArgumentException("System already registered: " + id);
        }

        SystemNode node = new SystemNode(id, system);
        systems.put(id, node);
        dirty = true;

        return new SystemBuilder(node, this);
    }

    /**
     * Remove a system from the orchestrator.
     */
    public void removeSystem(String id) {
        SystemNode removed = systems.remove(id);
        if (removed != null) {
            // Remove all references to this node
            for (SystemNode node : systems.values()) {
                node.dependencies.remove(removed);
                node.dependents.remove(removed);
            }
            dirty = true;
        }
    }

    /**
     * Get a system by ID.
     */
    public SnowySystem getSystem(String id) {
        SystemNode node = systems.get(id);
        return node != null ? node.system : null;
    }

    // ========================================================================
    // EXECUTION
    // ========================================================================

    /**
     * Execute all systems in dependency order.
     */
    public void execute(World world, float deltaTime) {
        if (dirty) {
            rebuildExecutionOrder();
        }

        long startTime = System.nanoTime();

        // Execute systems in topological order
        // Systems with no outstanding dependencies can run in parallel
        Set<SystemNode> completed = ConcurrentHashMap.newKeySet();
        Set<SystemNode> ready = ConcurrentHashMap.newKeySet();

        // Find initial ready systems (no dependencies)
        for (SystemNode node : systems.values()) {
            if (node.dependencies.isEmpty()) {
                ready.add(node);
            }
        }

        while (!ready.isEmpty()) {
            // Execute all ready systems in parallel
            List<CompletableFuture<Void>> futures = ready.stream()
                .map(node -> CompletableFuture.runAsync(() -> {
                    long systemStart = System.nanoTime();
                    try {
                        node.system.update(world, deltaTime);
                        long systemDuration = System.nanoTime() - systemStart;
                        stats.recordSystemExecution(node.id, systemDuration);
                    } catch (Exception e) {
                        System.err.println("System execution failed: " + node.id + " - " + e.getMessage());
                        e.printStackTrace();
                    }
                    completed.add(node);
                }, executor))
                .toList();

            // Wait for batch to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Find next batch of ready systems
            Set<SystemNode> nextReady = new HashSet<>();
            for (SystemNode node : systems.values()) {
                if (!completed.contains(node) && !ready.contains(node)) {
                    // Check if all dependencies are completed
                    if (completed.containsAll(node.dependencies)) {
                        nextReady.add(node);
                    }
                }
            }

            ready.clear();
            ready.addAll(nextReady);
        }

        long totalDuration = System.nanoTime() - startTime;
        stats.recordFrameExecution(totalDuration);
    }

    /**
     * Execute systems serially (for debugging).
     */
    public void executeSerial(World world, float deltaTime) {
        if (dirty) {
            rebuildExecutionOrder();
        }

        long startTime = System.nanoTime();

        for (SystemNode node : executionOrder) {
            long systemStart = System.nanoTime();
            node.system.update(world, deltaTime);
            long systemDuration = System.nanoTime() - systemStart;
            stats.recordSystemExecution(node.id, systemDuration);
        }

        long totalDuration = System.nanoTime() - startTime;
        stats.recordFrameExecution(totalDuration);
    }

    // ========================================================================
    // DAG CONSTRUCTION
    // ========================================================================

    /**
     * Rebuild the execution order from the dependency graph.
     */
    private void rebuildExecutionOrder() {
        // Detect cycles
        if (hasCycle()) {
            throw new IllegalStateException("Circular dependency detected in system graph!");
        }

        // Topological sort (Kahn's algorithm)
        executionOrder = topologicalSort();
        dirty = false;
    }

    /**
     * Detect cycles using DFS.
     */
    private boolean hasCycle() {
        Set<SystemNode> visited = new HashSet<>();
        Set<SystemNode> recursionStack = new HashSet<>();

        for (SystemNode node : systems.values()) {
            if (hasCycleUtil(node, visited, recursionStack)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasCycleUtil(SystemNode node, Set<SystemNode> visited, Set<SystemNode> recursionStack) {
        if (recursionStack.contains(node)) {
            return true; // Cycle detected
        }

        if (visited.contains(node)) {
            return false; // Already checked
        }

        visited.add(node);
        recursionStack.add(node);

        for (SystemNode dependent : node.dependents) {
            if (hasCycleUtil(dependent, visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(node);
        return false;
    }

    /**
     * Topological sort using Kahn's algorithm.
     */
    private List<SystemNode> topologicalSort() {
        List<SystemNode> sorted = new ArrayList<>();
        Queue<SystemNode> queue = new ArrayDeque<>();

        // Count in-degrees
        Map<SystemNode, Integer> inDegree = new HashMap<>();
        for (SystemNode node : systems.values()) {
            inDegree.put(node, node.dependencies.size());
            if (node.dependencies.isEmpty()) {
                queue.offer(node);
            }
        }

        while (!queue.isEmpty()) {
            SystemNode node = queue.poll();
            sorted.add(node);

            for (SystemNode dependent : node.dependents) {
                int degree = inDegree.get(dependent) - 1;
                inDegree.put(dependent, degree);

                if (degree == 0) {
                    queue.offer(dependent);
                }
            }
        }

        if (sorted.size() != systems.size()) {
            throw new IllegalStateException("Failed to create topological sort - cycle detected!");
        }

        return sorted;
    }

    // ========================================================================
    // SYSTEM NODE
    // ========================================================================

    /**
     * Internal node representing a system and its dependencies.
     */
    private static final class SystemNode {
        final String id;
        final SnowySystem system;
        final Set<SystemNode> dependencies = new HashSet<>();
        final Set<SystemNode> dependents = new HashSet<>();

        SystemNode(String id, SnowySystem system) {
            this.id = id;
            this.system = system;
        }

        void addDependency(SystemNode dependency) {
            dependencies.add(dependency);
            dependency.dependents.add(this);
        }

        @Override
        public String toString() {
            return "SystemNode{id='" + id + "'}";
        }
    }

    // ========================================================================
    // SYSTEM BUILDER
    // ========================================================================

    /**
     * Fluent builder for specifying system dependencies.
     */
    public static final class SystemBuilder {
        private final SystemNode node;
        private final SystemOrchestrator orchestrator;

        private SystemBuilder(SystemNode node, SystemOrchestrator orchestrator) {
            this.node = node;
            this.orchestrator = orchestrator;
        }

        /**
         * This system runs after the specified system.
         */
        public SystemBuilder after(String systemId) {
            SystemNode dependency = orchestrator.systems.get(systemId);
            if (dependency == null) {
                throw new IllegalArgumentException("System not found: " + systemId);
            }
            node.addDependency(dependency);
            orchestrator.dirty = true;
            return this;
        }

        /**
         * This system runs after the specified system.
         */
        public SystemBuilder after(SnowySystem system) {
            return after(system.getClass().getSimpleName());
        }

        /**
         * This system runs after another system builder's system.
         */
        public SystemBuilder after(SystemBuilder builder) {
            node.addDependency(builder.node);
            orchestrator.dirty = true;
            return this;
        }

        /**
         * This system runs before the specified system.
         */
        public SystemBuilder before(String systemId) {
            SystemNode dependent = orchestrator.systems.get(systemId);
            if (dependent == null) {
                throw new IllegalArgumentException("System not found: " + systemId);
            }
            dependent.addDependency(node);
            orchestrator.dirty = true;
            return this;
        }

        /**
         * Complete the builder and return the system.
         */
        public SnowySystem build() {
            return node.system;
        }
    }

    // ========================================================================
    // EXECUTION STATISTICS
    // ========================================================================

    private static final class ExecutionStats {
        private final ConcurrentHashMap<String, SystemStats> systemStats = new ConcurrentHashMap<>();
        private long frameCount = 0;
        private long totalFrameTime = 0;

        void recordSystemExecution(String systemId, long durationNanos) {
            systemStats.computeIfAbsent(systemId, k -> new SystemStats())
                .record(durationNanos);
        }

        void recordFrameExecution(long durationNanos) {
            frameCount++;
            totalFrameTime += durationNanos;
        }

        double getAverageFrameTime() {
            return frameCount > 0 ? (double) totalFrameTime / frameCount : 0.0;
        }
    }

    private static final class SystemStats {
        private long executionCount = 0;
        private long totalTime = 0;
        private long minTime = Long.MAX_VALUE;
        private long maxTime = 0;

        void record(long durationNanos) {
            executionCount++;
            totalTime += durationNanos;
            minTime = Math.min(minTime, durationNanos);
            maxTime = Math.max(maxTime, durationNanos);
        }

        double getAverageTime() {
            return executionCount > 0 ? (double) totalTime / executionCount : 0.0;
        }
    }

    // ========================================================================
    // QUERY API
    // ========================================================================

    /**
     * Get execution order as list of system IDs.
     */
    public List<String> getExecutionOrder() {
        if (dirty) {
            rebuildExecutionOrder();
        }
        return executionOrder.stream()
            .map(node -> node.id)
            .toList();
    }

    /**
     * Get all system IDs.
     */
    public Set<String> getSystemIds() {
        return Set.copyOf(systems.keySet());
    }

    /**
     * Get dependencies for a system.
     */
    public Set<String> getDependencies(String systemId) {
        SystemNode node = systems.get(systemId);
        if (node == null) return Set.of();
        
        return node.dependencies.stream()
            .map(dep -> dep.id)
            .collect(Collectors.toSet());
    }

    /**
     * Get systems that depend on this system.
     */
    public Set<String> getDependents(String systemId) {
        SystemNode node = systems.get(systemId);
        if (node == null) return Set.of();
        
        return node.dependents.stream()
            .map(dep -> dep.id)
            .collect(Collectors.toSet());
    }

    /**
     * Visualize the DAG as a text graph.
     */
    public String visualizeDAG() {
        if (dirty) {
            rebuildExecutionOrder();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("════════════════════════════════════════\n");
        sb.append(" System Execution DAG\n");
        sb.append("════════════════════════════════════════\n\n");

        for (SystemNode node : executionOrder) {
            sb.append(node.id);
            
            if (!node.dependencies.isEmpty()) {
                sb.append(" ← depends on: ");
                sb.append(node.dependencies.stream()
                    .map(dep -> dep.id)
                    .collect(Collectors.joining(", ")));
            }
            
            sb.append("\n");
        }

        sb.append("\n════════════════════════════════════════\n");
        return sb.toString();
    }

    /**
     * Get execution statistics.
     */
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("════════════════════════════════════════\n");
        sb.append(" Execution Statistics\n");
        sb.append("════════════════════════════════════════\n");
        sb.append(String.format("Total Frames: %d\n", stats.frameCount));
        sb.append(String.format("Avg Frame Time: %.2f ms\n", stats.getAverageFrameTime() / 1_000_000.0));
        sb.append("\nPer-System Stats:\n");

        for (Map.Entry<String, SystemStats> entry : stats.systemStats.entrySet()) {
            SystemStats s = entry.getValue();
            sb.append(String.format("  %s: avg=%.2f ms, min=%.2f ms, max=%.2f ms\n",
                entry.getKey(),
                s.getAverageTime() / 1_000_000.0,
                s.minTime / 1_000_000.0,
                s.maxTime / 1_000_000.0));
        }

        sb.append("════════════════════════════════════════\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("SystemOrchestrator[systems=%d, dirty=%s]", 
            systems.size(), dirty);
    }
}
