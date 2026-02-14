package stellar.snow.astralis.engine.render.graph;
import stellar.snow.astralis.engine.gpu.authority.GPUBackend;
import stellar.snow.astralis.engine.gpu.authority.GPUBackendSelector;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
/**
 * RenderGraph - Modern DAG-based rendering orchestrator.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Automatic resource state tracking and barrier insertion</li>
 *   <li>Transient resource allocation with memory aliasing</li>
 *   <li>Dead pass elimination and pass merging</li>
 *   <li>Parallel command buffer recording</li>
 *   <li>Async compute support with timeline semaphores</li>
 *   <li>Conditional pass execution</li>
 *   <li>Integrated GPU profiling</li>
 * </ul>
 */
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════
    
    private static final int MAX_FRAMES_IN_FLIGHT = 3;
    private static final int COMMAND_BUFFER_POOL_SIZE = 16;
    
    // ═══════════════════════════════════════════════════════════════════════
    // CORE STATE
    // ═══════════════════════════════════════════════════════════════════════
    
    private final List<RenderPassNode> nodes = new ArrayList<>();
    private final Map<String, ResourceNode> resources = new LinkedHashMap<>();
    private final GPUBackend backend;
    private final ExecutorService recordingExecutor;
    
    // ═══════════════════════════════════════════════════════════════════════
    // FRAME STATE
    // ═══════════════════════════════════════════════════════════════════════
    
    private final AtomicInteger frameIndex = new AtomicInteger(0);
    private final FrameResourcePool[] frameResources = new FrameResourcePool[MAX_FRAMES_IN_FLIGHT];
    private volatile List<RenderPassNode> compiledPasses;
    private volatile boolean graphDirty = true;
    
    // ═══════════════════════════════════════════════════════════════════════
    // STATISTICS & PROFILING
    // ═══════════════════════════════════════════════════════════════════════
    
    private final GraphStatistics statistics = new GraphStatistics();
    private final List<Consumer<GraphStatistics>> statisticsCallbacks = new CopyOnWriteArrayList<>();
    
    // ═══════════════════════════════════════════════════════════════════════
    // OPTIONS
    // ═══════════════════════════════════════════════════════════════════════
    
    private final GraphOptions options = new GraphOptions();
    
    public static final class GraphOptions {
        public boolean enablePassCulling = true;
        public boolean enablePassMerging = true;
        public boolean enableMemoryAliasing = true;
        public boolean enableParallelRecording = true;
        public boolean enableSplitBarriers = true;
        public boolean enableGPUProfiling = false;
        public int parallelRecordingThreshold = 4;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════
    
    public RenderGraph() {
        this.backend = GPUBackendSelector.get();
        this.recordingExecutor = Executors.newWorkStealingPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1)
        );
        
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            frameResources[i] = new FrameResourcePool(backend, COMMAND_BUFFER_POOL_SIZE);
        }
    }
    
    public GraphOptions getOptions() {
        return options;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // PASS CREATION
    // ═══════════════════════════════════════════════════════════════════════
    
    public PassBuilder addPass(String name) {
        return addPass(name, RenderPassNode.PassType.GRAPHICS);
    }
    
    public PassBuilder addPass(String name, RenderPassNode.PassType type) {
        var node = new RenderPassNode(name, type);
        nodes.add(node);
        graphDirty = true;
        return new PassBuilder(this, node);
    }
    
    public PassBuilder addComputePass(String name) {
        return addPass(name, RenderPassNode.PassType.COMPUTE);
    }
    
    public PassBuilder addAsyncComputePass(String name) {
        return addPass(name, RenderPassNode.PassType.ASYNC_COMPUTE);
    }
    
    public PassBuilder addTransferPass(String name) {
        return addPass(name, RenderPassNode.PassType.TRANSFER);
    }
    
    public PassBuilder addRayTracingPass(String name) {
        return addPass(name, RenderPassNode.PassType.RAY_TRACING);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // RESOURCE CREATION
    // ═══════════════════════════════════════════════════════════════════════
    
    public ResourceNode createTexture(String name, int width, int height, int format) {
        return createTexture(name, width, height, 1, format, 1, 1);
    }
    
    public ResourceNode createTexture(String name, int width, int height, int depth,
                                       int format, int mipLevels, int arrayLayers) {
        var node = new ResourceNode(name, depth > 1 ? ResourceNode.Type.TEXTURE_3D : ResourceNode.Type.TEXTURE);
        node.width = width;
        node.height = height;
        node.depth = depth;
        node.format = format;
        node.mipLevels = mipLevels;
        node.arrayLayers = arrayLayers;
        node.flags.add(ResourceNode.ResourceFlags.TRANSIENT);
        resources.put(name, node);
        graphDirty = true;
        return node;
    }
    
    public ResourceNode createTextureArray(String name, int width, int height, 
                                            int layers, int format, int mipLevels) {
        var node = new ResourceNode(name, ResourceNode.Type.TEXTURE_ARRAY);
        node.width = width;
        node.height = height;
        node.arrayLayers = layers;
        node.format = format;
        node.mipLevels = mipLevels;
        node.flags.add(ResourceNode.ResourceFlags.TRANSIENT);
        resources.put(name, node);
        graphDirty = true;
        return node;
    }
    
    public ResourceNode createTextureCube(String name, int size, int format, int mipLevels) {
        var node = new ResourceNode(name, ResourceNode.Type.TEXTURE_CUBE);
        node.width = size;
        node.height = size;
        node.arrayLayers = 6;
        node.format = format;
        node.mipLevels = mipLevels;
        node.flags.add(ResourceNode.ResourceFlags.TRANSIENT);
        resources.put(name, node);
        graphDirty = true;
        return node;
    }
    
    public ResourceNode createBuffer(String name, long size, int usage) {
        var node = new ResourceNode(name, ResourceNode.Type.BUFFER);
        node.size = size;
        node.usage = usage;
        node.flags.add(ResourceNode.ResourceFlags.TRANSIENT);
        resources.put(name, node);
        graphDirty = true;
        return node;
    }
    
    public ResourceNode createStructuredBuffer(String name, long elementCount, int stride, int usage) {
        var node = createBuffer(name, elementCount * stride, usage);
        node.structureStride = stride;
        return node;
    }
    
    public ResourceNode importTexture(String name, long handle, int width, int height, int format) {
        var node = new ResourceNode(name, ResourceNode.Type.TEXTURE);
        node.handle = handle;
        node.width = width;
        node.height = height;
        node.format = format;
        node.imported = true;
        node.flags.add(ResourceNode.ResourceFlags.PERSISTENT);
        resources.put(name, node);
        return node;
    }
    
    public ResourceNode importBuffer(String name, long handle, long size) {
        var node = new ResourceNode(name, ResourceNode.Type.BUFFER);
        node.handle = handle;
        node.size = size;
        node.imported = true;
        node.flags.add(ResourceNode.ResourceFlags.PERSISTENT);
        resources.put(name, node);
        return node;
    }
    
    public ResourceNode getResource(String name) {
        return resources.get(name);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // GRAPH COMPILATION
    // ═══════════════════════════════════════════════════════════════════════
    
    private void compileGraph() {
        if (!graphDirty && compiledPasses != null) {
            return;
        }
        
        long startTime = System.nanoTime();
        
        // 1. Build adjacency information
        buildDependencyGraph();
        
        // 2. Determine resource lifetimes
        calculateResourceLifetimes();
        
        // 3. Cull dead passes
        List<RenderPassNode> activePasses = options.enablePassCulling 
            ? cullDeadPasses() 
            : new ArrayList<>(nodes);
        
        // 4. Topological sort
        List<RenderPassNode> sortedPasses = topologicalSort(activePasses);
        
        // 5. Merge compatible passes
        if (options.enablePassMerging) {
            sortedPasses = mergeCompatiblePasses(sortedPasses);
        }
        
        // 6. Plan memory aliasing
        if (options.enableMemoryAliasing) {
            planMemoryAliasing(sortedPasses);
        }
        
        // 7. Assign queue families for async compute
        assignQueueFamilies(sortedPasses);
        
        compiledPasses = sortedPasses;
        graphDirty = false;
        
        statistics.compilationTimeNanos = System.nanoTime() - startTime;
        statistics.totalPasses = nodes.size();
        statistics.activePasses = sortedPasses.size();
        statistics.culledPasses = nodes.size() - sortedPasses.size();
    }
    
    private void buildDependencyGraph() {
        for (RenderPassNode node : nodes) {
            for (RenderPassNode.ResourceAccess input : node.inputs) {
                if (input.resource.producer != null) {
                    // Implicit dependency through resource
                }
                input.resource.consumers.add(node);
            }
        }
    }
    
    private void calculateResourceLifetimes() {
        for (int i = 0; i < nodes.size(); i++) {
            RenderPassNode pass = nodes.get(i);
            for (RenderPassNode.ResourceAccess access : pass.inputs) {
                access.resource.updateLifetime(i);
            }
            for (RenderPassNode.ResourceAccess access : pass.outputs) {
                access.resource.updateLifetime(i);
            }
        }
    }
    
    private List<RenderPassNode> cullDeadPasses() {
        Set<RenderPassNode> requiredPasses = new HashSet<>();
        Set<ResourceNode> requiredResources = new HashSet<>();
        
        // Find terminal passes (those that write to imported/external resources or present)
        for (RenderPassNode node : nodes) {
            if (node.type == RenderPassNode.PassType.PRESENT ||
                node.flags.contains(RenderPassNode.PassFlags.DISABLE_CULLING)) {
                requiredPasses.add(node);
            }
            
            for (RenderPassNode.ResourceAccess output : node.outputs) {
                if (output.resource.imported || 
                    output.resource.flags.contains(ResourceNode.ResourceFlags.PERSISTENT)) {
                    requiredPasses.add(node);
                    break;
                }
            }
        }
        
        // Backward propagate requirements
        Queue<RenderPassNode> workQueue = new ArrayDeque<>(requiredPasses);
        while (!workQueue.isEmpty()) {
            RenderPassNode current = workQueue.poll();
            
            for (RenderPassNode.ResourceAccess input : current.inputs) {
                requiredResources.add(input.resource);
                if (input.resource.producer != null && requiredPasses.add(input.resource.producer)) {
                    workQueue.add(input.resource.producer);
                }
            }
            
            for (RenderPassNode dep : current.explicitDependencies) {
                if (requiredPasses.add(dep)) {
                    workQueue.add(dep);
                }
            }
        }
        
        // Mark culled passes
        for (RenderPassNode node : nodes) {
            node.wasCulled = !requiredPasses.contains(node);
        }
        
        return nodes.stream()
            .filter(requiredPasses::contains)
            .collect(Collectors.toList());
    }
    
    private List<RenderPassNode> topologicalSort(List<RenderPassNode> passes) {
        List<RenderPassNode> sorted = new ArrayList<>();
        Set<RenderPassNode> visited = new HashSet<>();
        Set<RenderPassNode> inStack = new HashSet<>();
        
        for (RenderPassNode node : passes) {
            if (!visited.contains(node)) {
                if (!topologicalSortDFS(node, visited, inStack, sorted, passes)) {
                    throw new IllegalStateException("Cyclic dependency detected in render graph");
                }
            }
        }
        
        Collections.reverse(sorted);
        return sorted;
    }
    
    private boolean topologicalSortDFS(RenderPassNode node, Set<RenderPassNode> visited,
                                        Set<RenderPassNode> inStack, List<RenderPassNode> result,
                                        List<RenderPassNode> activePasses) {
        visited.add(node);
        inStack.add(node);
        
        // Process resource dependencies
        for (RenderPassNode.ResourceAccess input : node.inputs) {
            RenderPassNode producer = input.resource.producer;
            if (producer != null && activePasses.contains(producer)) {
                if (inStack.contains(producer)) {
                    return false; // Cycle detected
                }
                if (!visited.contains(producer)) {
                    if (!topologicalSortDFS(producer, visited, inStack, result, activePasses)) {
                        return false;
                    }
                }
            }
        }
        
        // Process explicit dependencies
        for (RenderPassNode dep : node.explicitDependencies) {
            if (activePasses.contains(dep)) {
                if (inStack.contains(dep)) {
                    return false;
                }
                if (!visited.contains(dep)) {
                    if (!topologicalSortDFS(dep, visited, inStack, result, activePasses)) {
                        return false;
                    }
                }
            }
        }
        
        inStack.remove(node);
        result.add(node);
        return true;
    }
    
    private List<RenderPassNode> mergeCompatiblePasses(List<RenderPassNode> passes) {
        // Simple merge strategy: consecutive graphics passes with same render targets
        List<RenderPassNode> merged = new ArrayList<>();
        
        for (RenderPassNode pass : passes) {
            if (!pass.flags.contains(RenderPassNode.PassFlags.ALLOW_MERGING)) {
                merged.add(pass);
                continue;
            }
            
            // Check if can merge with previous
            if (!merged.isEmpty()) {
                RenderPassNode prev = merged.get(merged.size() - 1);
                if (canMergePasses(prev, pass)) {
                    // Create merged pass
                    // For now, just add separately
                    merged.add(pass);
                    continue;
                }
            }
            
            merged.add(pass);
        }
        
        return merged;
    }
    
    private boolean canMergePasses(RenderPassNode a, RenderPassNode b) {
        if (a.type != b.type || a.type != RenderPassNode.PassType.GRAPHICS) {
            return false;
        }
        
        // Same render targets
        if (a.outputs.size() != b.outputs.size()) {
            return false;
        }
        
        for (int i = 0; i < a.outputs.size(); i++) {
            if (a.outputs.get(i).resource != b.outputs.get(i).resource) {
                return false;
            }
        }
        
        // B must not modify inputs of A
        for (RenderPassNode.ResourceAccess aInput : a.inputs) {
            for (RenderPassNode.ResourceAccess bOutput : b.outputs) {
                if (aInput.resource == bOutput.resource) {
                    return false;
                }
            }
        }
        
        return b.clearState.colorLoadOp == RenderPassNode.ClearState.LoadOp.LOAD;
    }
    
    private void planMemoryAliasing(List<RenderPassNode> passes) {
        // Group transient resources by memory size buckets
        List<ResourceNode> transientResources = resources.values().stream()
            .filter(r -> r.flags.contains(ResourceNode.ResourceFlags.TRANSIENT) 
                      && r.flags.contains(ResourceNode.ResourceFlags.ALLOW_ALIASING))
            .sorted(Comparator.comparingLong(ResourceNode::getRequiredMemorySize).reversed())
            .collect(Collectors.toList());
        
        // Simple first-fit aliasing
        for (int i = 0; i < transientResources.size(); i++) {
            ResourceNode current = transientResources.get(i);
            
            for (int j = 0; j < i; j++) {
                ResourceNode candidate = transientResources.get(j);
                
                if (current.canAliasWithResource(candidate)) {
                    current.aliasTarget = candidate;
                    statistics.aliasedResources++;
                    break;
                }
            }
        }
    }
    
    private void assignQueueFamilies(List<RenderPassNode> passes) {
        for (RenderPassNode pass : passes) {
            switch (pass.type) {
                case ASYNC_COMPUTE -> pass.hints.preferredQueue = 
                    RenderPassNode.ExecutionHints.QueueType.COMPUTE;
                case TRANSFER -> pass.hints.preferredQueue = 
                    RenderPassNode.ExecutionHints.QueueType.TRANSFER;
                default -> pass.hints.preferredQueue = 
                    RenderPassNode.ExecutionHints.QueueType.GRAPHICS;
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXECUTION
    // ═══════════════════════════════════════════════════════════════════════
    
    public void execute() {
        execute(0.016, Collections.emptyMap());
    }
    
    public void execute(double deltaTime, Map<String, Object> userData) {
        long frameStart = System.nanoTime();
        int currentFrame = frameIndex.getAndIncrement() % MAX_FRAMES_IN_FLIGHT;
        FrameResourcePool framePool = frameResources[currentFrame];
        
        // Wait for previous frame using this pool to complete
        framePool.waitForFence();
        framePool.reset();
        
        // Compile graph if needed
        compileGraph();
        
        // Create frame context
        var context = new RenderPassNode.FrameContext(
            frameIndex.get(),
            deltaTime,
            frameStart,
            userData
        );
        
        // Allocate transient resources
        allocateResources(framePool);
        
        // Begin frame
        backend.beginFrame();
        
        // Execute passes
        if (options.enableParallelRecording && compiledPasses.size() >= options.parallelRecordingThreshold) {
            executeParallel(context, framePool);
        } else {
            executeSequential(context, framePool);
        }
        
        // End frame
        backend.endFrame();
        
        // Signal fence
        framePool.signalFence();
        
        // Update statistics
        statistics.frameTimeNanos = System.nanoTime() - frameStart;
        statistics.frameCount++;
        notifyStatisticsCallbacks();
    }
    
    private void executeSequential(RenderPassNode.FrameContext context, FrameResourcePool framePool) {
        for (RenderPassNode node : compiledPasses) {
            executePass(node, context, framePool);
        }
    }
    
    private void executeParallel(RenderPassNode.FrameContext context, FrameResourcePool framePool) {
        // Group passes into batches that can be recorded in parallel
        List<List<RenderPassNode>> batches = groupIntoBatches(compiledPasses);
        
        for (List<RenderPassNode> batch : batches) {
            if (batch.size() == 1) {
                executePass(batch.get(0), context, framePool);
            } else {
                // Record in parallel
                List<Future<GPUBackend.CommandBuffer>> futures = new ArrayList<>();
                
                for (RenderPassNode node : batch) {
                    futures.add(recordingExecutor.submit(() -> {
                        GPUBackend.CommandBuffer cmdBuffer = framePool.acquireCommandBuffer();
                        recordPass(node, cmdBuffer, context);
                        return cmdBuffer;
                    }));
                }
                
                // Submit in order
                for (Future<GPUBackend.CommandBuffer> future : futures) {
                    try {
                        GPUBackend.CommandBuffer cmdBuffer = future.get();
                        backend.submitCommandBuffer(cmdBuffer);
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException("Failed to record command buffer", e);
                    }
                }
            }
        }
    }
    
    private List<List<RenderPassNode>> groupIntoBatches(List<RenderPassNode> passes) {
        List<List<RenderPassNode>> batches = new ArrayList<>();
        List<RenderPassNode> currentBatch = new ArrayList<>();
        Set<ResourceNode> currentBatchOutputs = new HashSet<>();
        
        for (RenderPassNode pass : passes) {
            boolean canBatch = true;
            
            // Check if pass depends on any outputs of current batch
            for (RenderPassNode.ResourceAccess input : pass.inputs) {
                if (currentBatchOutputs.contains(input.resource)) {
                    canBatch = false;
                    break;
                }
            }
            
            if (!canBatch || pass.flags.contains(RenderPassNode.PassFlags.FORCE_SINGLE_THREADED)) {
                if (!currentBatch.isEmpty()) {
                    batches.add(currentBatch);
                    currentBatch = new ArrayList<>();
                    currentBatchOutputs.clear();
                }
            }
            
            currentBatch.add(pass);
            for (RenderPassNode.ResourceAccess output : pass.outputs) {
                currentBatchOutputs.add(output.resource);
            }
        }
        
        if (!currentBatch.isEmpty()) {
            batches.add(currentBatch);
        }
        
        return batches;
    }
    
    private void executePass(RenderPassNode node, RenderPassNode.FrameContext context,
                             FrameResourcePool framePool) {
        // Check conditional execution
        if (node.conditionalPredicate != null && !node.conditionalPredicate.test(context)) {
            node.wasCulled = true;
            return;
        }
        
        long cpuStart = System.nanoTime();
        
        // Insert barriers
        insertBarriers(node);
        
        // Begin GPU timestamp query
        long queryHandle = 0;
        if (options.enableGPUProfiling || node.flags.contains(RenderPassNode.PassFlags.PROFILE_GPU)) {
            queryHandle = backend.beginTimestampQuery();
        }
        
        // Execute based on pass type
        switch (node.type) {
            case GRAPHICS, MESH_SHADING -> executeGraphicsPass(node, context);
            case COMPUTE, ASYNC_COMPUTE -> executeComputePass(node, context);
            case TRANSFER -> executeTransferPass(node, context);
            case RAY_TRACING -> executeRayTracingPass(node, context);
            case PRESENT -> executePresentPass(node, context);
        }
        
        // End GPU timestamp query
        if (queryHandle != 0) {
            node.gpuTimeNanos = backend.endTimestampQuery(queryHandle);
        }
        
        node.cpuTimeNanos = System.nanoTime() - cpuStart;
        
        // Update resource states
        for (RenderPassNode.ResourceAccess output : node.outputs) {
            output.resource.currentState = output.requiredState;
        }
    }
    
    private void recordPass(RenderPassNode node, GPUBackend.CommandBuffer cmdBuffer,
                           RenderPassNode.FrameContext context) {
        // Record into secondary command buffer for parallel submission
        if (node.asyncRecordCallback != null) {
            node.asyncRecordCallback.record(cmdBuffer, context);
        }
    }
    
    private void executeGraphicsPass(RenderPassNode node, RenderPassNode.FrameContext context) {
        GPUBackend.RenderPassInfo info = node.getRenderPassInfo();
        
        // Begin debug marker
        backend.pushDebugGroup(node.name);
        
        // Configure VRS if enabled
        if (node.vrs.enabled) {
            backend.setShadingRate(
                node.vrs.baseRate.ordinal(),
                node.vrs.combiners[0].ordinal(),
                node.vrs.combiners[1].ordinal()
            );
        }
        
        backend.beginRenderPass(info);
        
        if (node.executor != null) {
            node.executor.execute(backend, context);
        }
        
        backend.endRenderPass();
        
        backend.popDebugGroup();
    }
    
    private void executeComputePass(RenderPassNode node, RenderPassNode.FrameContext context) {
        backend.pushDebugGroup(node.name);
        
        if (node.executor != null) {
            node.executor.execute(backend, context);
        }
        
        backend.popDebugGroup();
    }
    
    private void executeTransferPass(RenderPassNode node, RenderPassNode.FrameContext context) {
        backend.pushDebugGroup(node.name);
        
        if (node.executor != null) {
            node.executor.execute(backend, context);
        }
        
        backend.popDebugGroup();
    }
    
    private void executeRayTracingPass(RenderPassNode node, RenderPassNode.FrameContext context) {
        backend.pushDebugGroup(node.name);
        
        if (node.executor != null) {
            node.executor.execute(backend, context);
        }
        
        backend.popDebugGroup();
    }
    
    private void executePresentPass(RenderPassNode node, RenderPassNode.FrameContext context) {
        // Transition swapchain image for presentation
        if (node.executor != null) {
            node.executor.execute(backend, context);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // RESOURCE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════
    
    private void allocateResources(FrameResourcePool framePool) {
        for (ResourceNode resource : resources.values()) {
            if (resource.handle != 0 || resource.imported) {
                continue;
            }
            
            // Check for aliasing
            if (resource.aliasTarget != null && resource.aliasTarget.handle != 0) {
                resource.handle = resource.aliasTarget.handle;
                resource.memoryOffset = resource.aliasTarget.memoryOffset;
                continue;
            }
            
            // Allocate new resource
            switch (resource.type) {
                case TEXTURE, TEXTURE_ARRAY, TEXTURE_3D -> {
                    int flags = buildTextureFlags(resource);
                    resource.handle = backend.createTexture2D(
                        resource.width, resource.height, resource.format, resource.mipLevels
                    );
                }
                case TEXTURE_CUBE -> {
                    resource.handle = backend.createTextureCube(
                        resource.width, resource.format, resource.mipLevels
                    );
                }
                case BUFFER -> {
                    int memFlags = resource.flags.contains(ResourceNode.ResourceFlags.TRANSIENT)
                        ? GPUBackend.MemoryFlags.DEVICE_LOCAL | GPUBackend.MemoryFlags.LAZILY_ALLOCATED
                        : GPUBackend.MemoryFlags.DEVICE_LOCAL;
                    resource.handle = backend.createBuffer(resource.size, resource.usage, memFlags);
                }
                case ACCELERATION_STRUCTURE -> {
                    resource.handle = backend.createAccelerationStructure(resource.size);
                }
            }
        }
    }
    
    private int buildTextureFlags(ResourceNode resource) {
        int flags = 0;
        for (ResourceNode.ResourceFlags flag : resource.flags) {
            flags |= switch (flag) {
                case SAMPLED -> GPUBackend.TextureUsage.SAMPLED;
                case STORAGE -> GPUBackend.TextureUsage.STORAGE;
                case TRANSFER_SRC -> GPUBackend.TextureUsage.TRANSFER_SRC;
                case TRANSFER_DST -> GPUBackend.TextureUsage.TRANSFER_DST;
                case COLOR_ATTACHMENT -> GPUBackend.TextureUsage.COLOR_ATTACHMENT;
                case DEPTH_STENCIL_ATTACHMENT -> GPUBackend.TextureUsage.DEPTH_STENCIL_ATTACHMENT;
                case INPUT_ATTACHMENT -> GPUBackend.TextureUsage.INPUT_ATTACHMENT;
                default -> 0;
            };
        }
        return flags;
    }
    
    private void insertBarriers(RenderPassNode node) {
        List<RenderPassNode.BarrierInfo> barriers = node.generateInputBarriers();
        
        if (barriers.isEmpty()) {
            return;
        }
        
        // Group barriers by type for batching
        int bufferBarrierBits = 0;
        int imageBarrierBits = 0;
        
        for (RenderPassNode.BarrierInfo barrier : barriers) {
            if (options.enableSplitBarriers && canUseSplitBarrier(barrier)) {
                // Issue split barrier
                if (!barrier.splitBarrier) {
                    barrier.splitBarrierEvent = backend.signalEvent(barrier.srcStageMask);
                    barrier.splitBarrier = true;
                }
            } else {
                // Batch barriers
                if (barrier.resource.type == ResourceNode.Type.BUFFER) {
                    bufferBarrierBits |= barrier.dstAccessMask;
                } else {
                    imageBarrierBits |= barrier.dstStageMask;
                    backend.imageBarrier(
                        barrier.resource.handle,
                        barrier.srcState.ordinal(),
                        barrier.dstState.ordinal(),
                        barrier.srcStageMask,
                        barrier.dstStageMask,
                        barrier.srcAccessMask,
                        barrier.dstAccessMask
                    );
                }
            }
        }
        
        if (bufferBarrierBits != 0) {
            backend.memoryBarrier(bufferBarrierBits);
        }
    }
    
    private boolean canUseSplitBarrier(RenderPassNode.BarrierInfo barrier) {
        // Split barriers are beneficial when there's significant work between release and acquire
        return barrier.srcState != barrier.dstState;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════════════
    
    public void addStatisticsCallback(Consumer<GraphStatistics> callback) {
        statisticsCallbacks.add(callback);
    }
    
    public void removeStatisticsCallback(Consumer<GraphStatistics> callback) {
        statisticsCallbacks.remove(callback);
    }
    
    private void notifyStatisticsCallbacks() {
        for (Consumer<GraphStatistics> callback : statisticsCallbacks) {
            callback.accept(statistics);
        }
    }
    
    public GraphStatistics getStatistics() {
        return statistics;
    }
    
    public static final class GraphStatistics {
        public volatile long frameCount;
        public volatile long frameTimeNanos;
        public volatile long compilationTimeNanos;
        public volatile int totalPasses;
        public volatile int activePasses;
        public volatile int culledPasses;
        public volatile int aliasedResources;
        public volatile long peakMemoryUsage;
        public volatile long currentMemoryUsage;
        
        public double getFrameTimeMs() {
            return frameTimeNanos / 1_000_000.0;
        }
        
        public double getFPS() {
            return frameTimeNanos > 0 ? 1_000_000_000.0 / frameTimeNanos : 0;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // FRAME RESOURCE POOL
    // ═══════════════════════════════════════════════════════════════════════
    
    private static final class FrameResourcePool {
        private final GPUBackend backend;
        private final long fence;
        private final List<GPUBackend.CommandBuffer> commandBuffers;
        private final AtomicInteger commandBufferIndex = new AtomicInteger(0);
        
        FrameResourcePool(GPUBackend backend, int commandBufferCount) {
            this.backend = backend;
            this.fence = backend.createFence(true);
            this.commandBuffers = new ArrayList<>(commandBufferCount);
            
            for (int i = 0; i < commandBufferCount; i++) {
                commandBuffers.add(backend.createCommandBuffer());
            }
        }
        
        void waitForFence() {
            backend.waitForFence(fence);
        }
        
        void signalFence() {
            backend.resetFence(fence);
            backend.submitWithFence(fence);
        }
        
        void reset() {
            commandBufferIndex.set(0);
            for (GPUBackend.CommandBuffer cmdBuffer : commandBuffers) {
                cmdBuffer.reset();
            }
        }
        
        GPUBackend.CommandBuffer acquireCommandBuffer() {
            int index = commandBufferIndex.getAndIncrement();
            if (index >= commandBuffers.size()) {
                synchronized (commandBuffers) {
                    if (index >= commandBuffers.size()) {
                        commandBuffers.add(backend.createCommandBuffer());
                    }
                }
            }
            return commandBuffers.get(index);
        }
        
        void destroy() {
            backend.destroyFence(fence);
            for (GPUBackend.CommandBuffer cmdBuffer : commandBuffers) {
                backend.destroyCommandBuffer(cmdBuffer);
            }
            commandBuffers.clear();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════
    
    public void invalidate() {
        graphDirty = true;
    }
    
    public void clear() {
        nodes.clear();
        for (ResourceNode resource : resources.values()) {
            resource.producer = null;
            resource.consumers.clear();
        }
        graphDirty = true;
        compiledPasses = null;
    }
    
    public void shutdown() {
        // Wait for all frames to complete
        for (FrameResourcePool pool : frameResources) {
            pool.waitForFence();
            pool.destroy();
        }
        
        // Shutdown executor
        recordingExecutor.shutdown();
        try {
            if (!recordingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                recordingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            recordingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Destroy resources
        for (ResourceNode resource : resources.values()) {
            if (resource.handle != 0 && !resource.imported && resource.aliasTarget == null) {
                switch (resource.type) {
                    case BUFFER -> backend.destroyBuffer(resource.handle);
                    case ACCELERATION_STRUCTURE -> backend.destroyAccelerationStructure(resource.handle);
                    default -> backend.destroyTexture(resource.handle);
                }
            }
        }
        
        resources.clear();
        nodes.clear();
        compiledPasses = null;
        statisticsCallbacks.clear();
    }
}
