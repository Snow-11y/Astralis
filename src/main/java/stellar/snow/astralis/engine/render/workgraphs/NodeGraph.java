package stellar.snow.astralis.engine.render.workgraphs;

import java.util.*;

/**
 * GPU Work Graphs node graph structure
 * Represents dependencies between GPU compute tasks for dynamic dispatch
 * Based on DX12 Work Graphs (GPU-driven task parallelism)
 */
public final class NodeGraph {
    
    public static class Node {
        String name;
        int nodeIndex;
        
        // Dependencies
        List<Node> dependencies = new ArrayList<>();
        List<Node> dependents = new ArrayList<>();
        
        // GPU dispatch parameters
        int[] threadGroupSize = {1, 1, 1};  // Local workgroup size
        int[] dispatchSize = {1, 1, 1};      // Number of workgroups
        
        // Shader program
        long shaderModule;
        long pipelineLayout;
        
        // Input/output records (GPU-side data)
        List<ResourceBinding> inputs = new ArrayList<>();
        List<ResourceBinding> outputs = new ArrayList<>();
        
        // Execution state
        boolean isReady = false;
        boolean isExecuted = false;
        int inDegree = 0;  // For topological sort
        
        public void addDependency(Node node) {
            if (!dependencies.contains(node)) {
                dependencies.add(node);
                node.dependents.add(this);
                inDegree++;
            }
        }
        
        public void setThreadGroupSize(int x, int y, int z) {
            threadGroupSize[0] = x;
            threadGroupSize[1] = y;
            threadGroupSize[2] = z;
        }
        
        public void setDispatchSize(int x, int y, int z) {
            dispatchSize[0] = x;
            dispatchSize[1] = y;
            dispatchSize[2] = z;
        }
        
        public void addInput(String name, long buffer, int binding) {
            inputs.add(new ResourceBinding(name, buffer, binding, false));
        }
        
        public void addOutput(String name, long buffer, int binding) {
            outputs.add(new ResourceBinding(name, buffer, binding, true));
        }
        
        public boolean canExecute() {
            return isReady && !isExecuted && dependencies.stream().allMatch(d -> d.isExecuted);
        }
    }
    
    public static class ResourceBinding {
        String name;
        long buffer;
        int binding;
        boolean isOutput;
        
        public ResourceBinding(String name, long buffer, int binding, boolean isOutput) {
            this.name = name;
            this.buffer = buffer;
            this.binding = binding;
            this.isOutput = isOutput;
        }
    }
    
    private final List<Node> nodes = new ArrayList<>();
    private final Map<String, Node> nodeMap = new HashMap<>();
    
    /**
     * Add new node to the graph
     */
    public Node addNode(String name) {
        Node node = new Node();
        node.name = name;
        node.nodeIndex = nodes.size();
        nodes.add(node);
        nodeMap.put(name, node);
        return node;
    }
    
    /**
     * Get node by name
     */
    public Node getNode(String name) {
        return nodeMap.get(name);
    }
    
    /**
     * Get all nodes
     */
    public List<Node> getNodes() {
        return nodes;
    }
    
    /**
     * Topological sort using Kahn's algorithm
     * Returns nodes in execution order
     */
    public List<Node> topologicalSort() {
        List<Node> result = new ArrayList<>();
        Queue<Node> queue = new ArrayDeque<>();
        
        // Reset in-degrees
        for (Node node : nodes) {
            node.inDegree = node.dependencies.size();
            if (node.inDegree == 0) {
                queue.add(node);
            }
        }
        
        // Process nodes in topological order
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            result.add(node);
            
            // Reduce in-degree of dependents
            for (Node dependent : node.dependents) {
                dependent.inDegree--;
                if (dependent.inDegree == 0) {
                    queue.add(dependent);
                }
            }
        }
        
        // Check for cycles
        if (result.size() != nodes.size()) {
            throw new IllegalStateException("Work graph contains cycles!");
        }
        
        return result;
    }
    
    /**
     * Find nodes that can execute in parallel (same depth level)
     */
    public List<List<Node>> findParallelBatches() {
        List<List<Node>> batches = new ArrayList<>();
        Set<Node> executed = new HashSet<>();
        
        while (executed.size() < nodes.size()) {
            List<Node> batch = new ArrayList<>();
            
            // Find all nodes ready to execute
            for (Node node : nodes) {
                if (!executed.contains(node)) {
                    boolean canRun = true;
                    for (Node dep : node.dependencies) {
                        if (!executed.contains(dep)) {
                            canRun = false;
                            break;
                        }
                    }
                    if (canRun) {
                        batch.add(node);
                    }
                }
            }
            
            if (batch.isEmpty()) {
                throw new IllegalStateException("Deadlock in work graph!");
            }
            
            batches.add(batch);
            executed.addAll(batch);
        }
        
        return batches;
    }
    
    /**
     * Validate graph structure
     */
    public boolean validate() {
        // Check for cycles
        try {
            topologicalSort();
        } catch (IllegalStateException e) {
            return false;
        }
        
        // Check for disconnected nodes
        Set<Node> reachable = new HashSet<>();
        Queue<Node> queue = new ArrayDeque<>();
        
        // Find root nodes (no dependencies)
        for (Node node : nodes) {
            if (node.dependencies.isEmpty()) {
                queue.add(node);
            }
        }
        
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            reachable.add(node);
            queue.addAll(node.dependents);
        }
        
        return reachable.size() == nodes.size();
    }
    
    /**
     * Optimize graph by merging compatible nodes
     */
    public void optimize() {
        // Merge consecutive nodes with same shader and small workload
        // This reduces dispatch overhead
        
        List<Node> sorted = topologicalSort();
        
        for (int i = 0; i < sorted.size() - 1; i++) {
            Node current = sorted.get(i);
            Node next = sorted.get(i + 1);
            
            // Check if nodes can be merged
            if (canMerge(current, next)) {
                mergeNodes(current, next);
            }
        }
    }
    
    private boolean canMerge(Node a, Node b) {
        // Can merge if:
        // - Same shader
        // - B only depends on A
        // - No other dependencies
        return a.shaderModule == b.shaderModule &&
               b.dependencies.size() == 1 &&
               b.dependencies.get(0) == a;
    }
    
    private void mergeNodes(Node a, Node b) {
        // Combine dispatch sizes
        a.dispatchSize[0] += b.dispatchSize[0];
        
        // Transfer b's dependents to a
        a.dependents.remove(b);
        a.dependents.addAll(b.dependents);
        
        // Remove b from graph
        nodes.remove(b);
        nodeMap.remove(b.name);
    }
    
    /**
     * Reset execution state for re-execution
     */
    public void reset() {
        for (Node node : nodes) {
            node.isExecuted = false;
            node.isReady = node.dependencies.isEmpty();
        }
    }
    
    /**
     * Get statistics about the graph
     */
    public String getStatistics() {
        int totalNodes = nodes.size();
        int maxDepth = findParallelBatches().size();
        long totalWorkgroups = nodes.stream()
            .mapToLong(n -> (long)n.dispatchSize[0] * n.dispatchSize[1] * n.dispatchSize[2])
            .sum();
        
        return String.format(
            "Work Graph Statistics:\n" +
            "  Nodes: %d\n" +
            "  Max Depth: %d\n" +
            "  Total Workgroups: %d\n",
            totalNodes, maxDepth, totalWorkgroups
        );
    }
}
