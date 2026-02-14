package stellar.snow.astralis.engine.render.shader;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.regex.*;

/**
 * DependencyGraph - Shader Include Dependency Tracking
 * 
 * Tracks full graph of #include dependencies for shaders.
 * When a small utility file changes, knows exactly which shaders use it
 * and recompiles only those.
 * 
 * Features:
 * - Recursive include resolution
 * - Circular dependency detection
 * - Incremental recompilation
 * - File watch integration
 * - Dependency visualization
 * - Cache invalidation
 */
public final class DependencyGraph {
    
    /**
     * Shader file node in the dependency graph
     */
    public static class ShaderNode {
        private final Path filePath;
        private final Set<ShaderNode> dependencies = ConcurrentHashMap.newKeySet(); // Files this depends on
        private final Set<ShaderNode> dependents = ConcurrentHashMap.newKeySet();   // Files that depend on this
        private long lastModified;
        private String contentHash;
        private boolean compiled = false;
        private boolean needsRecompilation = false;
        
        public ShaderNode(Path filePath) {
            this.filePath = filePath;
            updateTimestamp();
        }
        
        public Path getFilePath() {
            return filePath;
        }
        
        public Set<ShaderNode> getDependencies() {
            return Collections.unmodifiableSet(dependencies);
        }
        
        public Set<ShaderNode> getDependents() {
            return Collections.unmodifiableSet(dependents);
        }
        
        public void addDependency(ShaderNode node) {
            dependencies.add(node);
            node.dependents.add(this);
        }
        
        public void removeDependency(ShaderNode node) {
            dependencies.remove(node);
            node.dependents.remove(this);
        }
        
        public void updateTimestamp() {
            try {
                lastModified = Files.getLastModifiedTime(filePath).toMillis();
            } catch (Exception e) {
                lastModified = 0;
            }
        }
        
        public boolean hasChanged() {
            try {
                long currentModified = Files.getLastModifiedTime(filePath).toMillis();
                return currentModified != lastModified;
            } catch (Exception e) {
                return false;
            }
        }
        
        public void markForRecompilation() {
            needsRecompilation = true;
        }
        
        public boolean needsRecompilation() {
            return needsRecompilation;
        }
        
        public void markCompiled() {
            compiled = true;
            needsRecompilation = false;
        }
        
        @Override
        public String toString() {
            return filePath.getFileName().toString();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ShaderNode)) return false;
            ShaderNode other = (ShaderNode) obj;
            return filePath.equals(other.filePath);
        }
        
        @Override
        public int hashCode() {
            return filePath.hashCode();
        }
    }
    
    // All shader nodes indexed by file path
    private final Map<Path, ShaderNode> nodes = new ConcurrentHashMap<>();
    
    // Root shader files (entry points, not included by others)
    private final Set<ShaderNode> rootNodes = ConcurrentHashMap.newKeySet();
    
    // Include pattern matcher
    private static final Pattern INCLUDE_PATTERN = Pattern.compile(
        "#include\\s+[\"<]([^\"'>]+)[\">]");
    
    // Base include directories
    private final List<Path> includePaths = new CopyOnWriteArrayList<>();
    
    // Statistics
    private long totalDependenciesResolved = 0;
    private long totalCircularDepsDetected = 0;
    private long totalRecompilations = 0;
    
    // File watcher
    private WatchService watchService;
    private final Map<WatchKey, Path> watchKeys = new ConcurrentHashMap<>();
    private Thread watchThread;
    
    public DependencyGraph(List<Path> includePaths) {
        this.includePaths.addAll(includePaths);
        initializeFileWatcher();
    }
    
    /**
     * Initialize file system watcher
     */
    private void initializeFileWatcher() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            
            // Register all include directories for watching
            for (Path path : includePaths) {
                if (Files.exists(path) && Files.isDirectory(path)) {
                    WatchKey key = path.register(watchService,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE);
                    watchKeys.put(key, path);
                }
            }
            
            // Start watch thread
            watchThread = new Thread(this::watchFiles, "ShaderFileWatcher");
            watchThread.setDaemon(true);
            watchThread.start();
            
        } catch (Exception e) {
            System.err.println("Failed to initialize file watcher: " + e.getMessage());
        }
    }
    
    /**
     * File watching thread
     */
    private void watchFiles() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                WatchKey key = watchService.take();
                Path dir = watchKeys.get(key);
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();
                    Path fullPath = dir.resolve(filename);
                    
                    // Handle file change
                    handleFileChange(fullPath);
                }
                
                key.reset();
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    /**
     * Handle file change event
     */
    private void handleFileChange(Path filePath) {
        ShaderNode node = nodes.get(filePath);
        if (node != null && node.hasChanged()) {
            System.out.println("Shader file changed: " + filePath);
            node.updateTimestamp();
            
            // Mark all dependents for recompilation
            invalidateDependents(node);
        }
    }
    
    /**
     * Add a shader file to the dependency graph
     */
    public ShaderNode addShader(Path shaderPath) {
        ShaderNode node = nodes.computeIfAbsent(shaderPath, ShaderNode::new);
        
        // Parse includes
        try {
            List<String> lines = Files.readAllLines(shaderPath);
            Set<Path> includedFiles = parseIncludes(lines, shaderPath.getParent());
            
            // Clear old dependencies
            for (ShaderNode dep : new ArrayList<>(node.dependencies)) {
                node.removeDependency(dep);
            }
            
            // Add new dependencies
            for (Path includePath : includedFiles) {
                ShaderNode depNode = addShader(includePath); // Recursive
                node.addDependency(depNode);
                totalDependenciesResolved++;
            }
            
            // If this file has no dependents, it's a root
            if (node.dependents.isEmpty()) {
                rootNodes.add(node);
            } else {
                rootNodes.remove(node);
            }
            
            // Check for circular dependencies
            if (hasCircularDependency(node)) {
                totalCircularDepsDetected++;
                System.err.println("CIRCULAR DEPENDENCY DETECTED: " + shaderPath);
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing shader " + shaderPath + ": " + e.getMessage());
        }
        
        return node;
    }
    
    /**
     * Parse #include directives from shader source
     */
    private Set<Path> parseIncludes(List<String> lines, Path baseDir) {
        Set<Path> includes = new HashSet<>();
        
        for (String line : lines) {
            Matcher matcher = INCLUDE_PATTERN.matcher(line);
            if (matcher.find()) {
                String includePath = matcher.group(1);
                
                // Resolve include path
                Path resolved = resolveIncludePath(includePath, baseDir);
                if (resolved != null) {
                    includes.add(resolved);
                }
            }
        }
        
        return includes;
    }
    
    /**
     * Resolve an include path to an absolute path
     */
    private Path resolveIncludePath(String includePath, Path baseDir) {
        // Try relative to current file
        Path relative = baseDir.resolve(includePath);
        if (Files.exists(relative)) {
            return relative.toAbsolutePath().normalize();
        }
        
        // Try include directories
        for (Path includeDir : includePaths) {
            Path inIncludeDir = includeDir.resolve(includePath);
            if (Files.exists(inIncludeDir)) {
                return inIncludeDir.toAbsolutePath().normalize();
            }
        }
        
        System.err.println("Could not resolve include: " + includePath);
        return null;
    }
    
    /**
     * Check for circular dependencies
     */
    private boolean hasCircularDependency(ShaderNode node) {
        return hasCircularDependency(node, new HashSet<>());
    }
    
    private boolean hasCircularDependency(ShaderNode node, Set<ShaderNode> visited) {
        if (visited.contains(node)) {
            return true; // Circular dependency found
        }
        
        visited.add(node);
        
        for (ShaderNode dep : node.dependencies) {
            if (hasCircularDependency(dep, new HashSet<>(visited))) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get all shaders that need recompilation
     */
    public List<ShaderNode> getShadersToRecompile() {
        // Check for file changes
        for (ShaderNode node : nodes.values()) {
            if (node.hasChanged()) {
                invalidateDependents(node);
            }
        }
        
        // Return all nodes marked for recompilation
        return nodes.values().stream()
            .filter(ShaderNode::needsRecompilation)
            .collect(Collectors.toList());
    }
    
    /**
     * Invalidate all shaders that depend on this node
     */
    private void invalidateDependents(ShaderNode node) {
        Set<ShaderNode> visited = new HashSet<>();
        invalidateDependentsRecursive(node, visited);
    }
    
    private void invalidateDependentsRecursive(ShaderNode node, Set<ShaderNode> visited) {
        if (visited.contains(node)) {
            return;
        }
        visited.add(node);
        
        node.markForRecompilation();
        totalRecompilations++;
        
        for (ShaderNode dependent : node.dependents) {
            invalidateDependentsRecursive(dependent, visited);
        }
    }
    
    /**
     * Get all dependencies of a shader (transitive closure)
     */
    public Set<ShaderNode> getAllDependencies(ShaderNode node) {
        Set<ShaderNode> allDeps = new HashSet<>();
        collectDependencies(node, allDeps);
        return allDeps;
    }
    
    private void collectDependencies(ShaderNode node, Set<ShaderNode> collected) {
        for (ShaderNode dep : node.dependencies) {
            if (collected.add(dep)) {
                collectDependencies(dep, collected);
            }
        }
    }
    
    /**
     * Get all dependents of a shader (transitive closure)
     */
    public Set<ShaderNode> getAllDependents(ShaderNode node) {
        Set<ShaderNode> allDeps = new HashSet<>();
        collectDependents(node, allDeps);
        return allDeps;
    }
    
    private void collectDependents(ShaderNode node, Set<ShaderNode> collected) {
        for (ShaderNode dep : node.dependents) {
            if (collected.add(dep)) {
                collectDependents(dep, collected);
            }
        }
    }
    
    /**
     * Get shader node by path
     */
    public ShaderNode getNode(Path path) {
        return nodes.get(path);
    }
    
    /**
     * Get all root shaders
     */
    public Set<ShaderNode> getRootShaders() {
        return Collections.unmodifiableSet(rootNodes);
    }
    
    /**
     * Get total shader count
     */
    public int getShaderCount() {
        return nodes.size();
    }
    
    /**
     * Get total dependency count
     */
    public int getDependencyCount() {
        return nodes.values().stream()
            .mapToInt(node -> node.dependencies.size())
            .sum();
    }
    
    /**
     * Visualize the dependency graph as DOT format
     */
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph ShaderDependencies {\n");
        sb.append("  rankdir=LR;\n");
        sb.append("  node [shape=box];\n\n");
        
        // Nodes
        for (ShaderNode node : nodes.values()) {
            String color = node.needsRecompilation() ? "red" : 
                          rootNodes.contains(node) ? "green" : "lightblue";
            sb.append(String.format("  \"%s\" [fillcolor=%s, style=filled];\n",
                node.getFilePath().getFileName(), color));
        }
        
        sb.append("\n");
        
        // Edges
        for (ShaderNode node : nodes.values()) {
            for (ShaderNode dep : node.dependencies) {
                sb.append(String.format("  \"%s\" -> \"%s\";\n",
                    node.getFilePath().getFileName(),
                    dep.getFilePath().getFileName()));
            }
        }
        
        sb.append("}\n");
        return sb.toString();
    }
    
    /**
     * Print dependency tree
     */
    public String printTree(ShaderNode root) {
        StringBuilder sb = new StringBuilder();
        printTreeRecursive(root, "", true, new HashSet<>(), sb);
        return sb.toString();
    }
    
    private void printTreeRecursive(ShaderNode node, String prefix, boolean isLast,
                                    Set<ShaderNode> visited, StringBuilder sb) {
        sb.append(prefix);
        sb.append(isLast ? "└── " : "├── ");
        sb.append(node.getFilePath().getFileName());
        
        if (visited.contains(node)) {
            sb.append(" (circular reference)");
            sb.append("\n");
            return;
        }
        
        if (node.needsRecompilation()) {
            sb.append(" [NEEDS RECOMPILE]");
        }
        sb.append("\n");
        
        visited.add(node);
        
        List<ShaderNode> deps = new ArrayList<>(node.dependencies);
        for (int i = 0; i < deps.size(); i++) {
            boolean last = (i == deps.size() - 1);
            String newPrefix = prefix + (isLast ? "    " : "│   ");
            printTreeRecursive(deps.get(i), newPrefix, last, new HashSet<>(visited), sb);
        }
    }
    
    /**
     * Get statistics
     */
    public String getStatistics() {
        return String.format("DependencyGraph Stats: shaders=%d, deps=%d, roots=%d, " +
                           "resolved=%d, circular=%d, recompiles=%d",
            getShaderCount(), getDependencyCount(), rootNodes.size(),
            totalDependenciesResolved, totalCircularDepsDetected, totalRecompilations);
    }
    
    /**
     * Shutdown the dependency graph
     */
    public void shutdown() {
        if (watchThread != null) {
            watchThread.interrupt();
        }
        
        if (watchService != null) {
            try {
                watchService.close();
            } catch (Exception e) {
                System.err.println("Error closing watch service: " + e.getMessage());
            }
        }
    }
}
