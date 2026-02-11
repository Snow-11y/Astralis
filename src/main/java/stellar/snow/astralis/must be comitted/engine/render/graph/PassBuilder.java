package stellar.snow.astralis.engine.render.graph;

import stellar.snow.astralis.engine.gpu.authority.GPUBackend;

import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * PassBuilder - Fluent API for defining render pass requirements.
 * 
 * <p>Provides a comprehensive builder pattern for configuring:</p>
 * <ul>
 *   <li>Resource read/write dependencies with explicit access types</li>
 *   <li>Clear operations and attachment load/store ops</li>
 *   <li>Variable rate shading configuration</li>
 *   <li>Execution hints and queue preferences</li>
 *   <li>Conditional execution predicates</li>
 *   <li>Profiling and debug options</li>
 * </ul>
 */
public final class PassBuilder {
    
    private final RenderGraph graph;
    private final RenderPassNode node;
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════
    
    public PassBuilder(RenderGraph graph, RenderPassNode node) {
        this.graph = graph;
        this.node = node;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // RESOURCE DEPENDENCIES
    // ═══════════════════════════════════════════════════════════════════════
    
    public PassBuilder read(ResourceNode resource) {
        return read(resource, inferReadState(resource), -1);
    }
    
    public PassBuilder read(ResourceNode resource, int binding) {
        return read(resource, inferReadState(resource), binding);
    }
    
    public PassBuilder read(ResourceNode resource, RenderPassNode.ResourceState state, int binding) {
        var access = new RenderPassNode.ResourceAccess(
            resource,
            RenderPassNode.AccessType.READ,
            state,
            binding
        );
        node.inputs.add(access);
        resource.consumers.add(node);
        return this;
    }
    
    public PassBuilder readSubresource(ResourceNode resource, int baseMip, int mipCount, 
                                        int baseLayer, int layerCount, int binding) {
        var access = new RenderPassNode.ResourceAccess(
            resource,
            RenderPassNode.AccessType.READ,
            inferReadState(resource),
            baseMip,
            mipCount,
            binding
        );
        node.inputs.add(access);
        resource.consumers.add(node);
        return this;
    }
    
    public PassBuilder write(ResourceNode resource) {
        return write(resource, inferWriteState(resource), -1);
    }
    
    public PassBuilder write(ResourceNode resource, int binding) {
        return write(resource, inferWriteState(resource), binding);
    }
    
    public PassBuilder write(ResourceNode resource, RenderPassNode.ResourceState state, int binding) {
        var access = new RenderPassNode.ResourceAccess(
            resource,
            RenderPassNode.AccessType.WRITE,
            state,
            binding
        );
        node.outputs.add(access);
        resource.producer = node;
        return this;
    }
    
    public PassBuilder readWrite(ResourceNode resource) {
        return readWrite(resource, RenderPassNode.ResourceState.GENERAL, -1);
    }
    
    public PassBuilder readWrite(ResourceNode resource, RenderPassNode.ResourceState state, int binding) {
        var access = new RenderPassNode.ResourceAccess(
            resource,
            RenderPassNode.AccessType.READ_WRITE,
            state,
            binding
        );
        node.inputs.add(access);
        node.outputs.add(access);
        resource.consumers.add(node);
        resource.producer = node;
        return this;
    }
    
    public PassBuilder colorAttachment(ResourceNode resource) {
        return colorAttachment(resource, 0);
    }
    
    public PassBuilder colorAttachment(ResourceNode resource, int index) {
        resource.flags.add(ResourceNode.ResourceFlags.COLOR_ATTACHMENT);
        return write(resource, RenderPassNode.ResourceState.COLOR_ATTACHMENT, index);
    }
    
    public PassBuilder depthStencilAttachment(ResourceNode resource) {
        resource.flags.add(ResourceNode.ResourceFlags.DEPTH_STENCIL_ATTACHMENT);
        return write(resource, RenderPassNode.ResourceState.DEPTH_STENCIL_ATTACHMENT, -1);
    }
    
    public PassBuilder depthStencilAttachmentReadOnly(ResourceNode resource) {
        resource.flags.add(ResourceNode.ResourceFlags.DEPTH_STENCIL_ATTACHMENT);
        return read(resource, RenderPassNode.ResourceState.DEPTH_STENCIL_ATTACHMENT, -1);
    }
    
    public PassBuilder sampleTexture(ResourceNode resource, int binding) {
        resource.flags.add(ResourceNode.ResourceFlags.SAMPLED);
        return read(resource, RenderPassNode.ResourceState.SHADER_READ, binding);
    }
    
    public PassBuilder storageTexture(ResourceNode resource, int binding) {
        resource.flags.add(ResourceNode.ResourceFlags.STORAGE);
        return readWrite(resource, RenderPassNode.ResourceState.GENERAL, binding);
    }
    
    public PassBuilder storageBuffer(ResourceNode resource, int binding) {
        return readWrite(resource, RenderPassNode.ResourceState.GENERAL, binding);
    }
    
    public PassBuilder uniformBuffer(ResourceNode resource, int binding) {
        return read(resource, RenderPassNode.ResourceState.SHADER_READ, binding);
    }
    
    public PassBuilder inputAttachment(ResourceNode resource, int index) {
        resource.flags.add(ResourceNode.ResourceFlags.INPUT_ATTACHMENT);
        return read(resource, RenderPassNode.ResourceState.SHADER_READ, index);
    }
    
    public PassBuilder resolveTarget(ResourceNode resource) {
        node.clearState.resolveTarget = resource;
        resource.flags.add(ResourceNode.ResourceFlags.COLOR_ATTACHMENT);
        return write(resource, RenderPassNode.ResourceState.COLOR_ATTACHMENT, -1);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXPLICIT DEPENDENCIES
    // ═══════════════════════════════════════════════════════════════════════
    
    public PassBuilder dependsOn(RenderPassNode otherPass) {
        node.explicitDependencies.add(otherPass);
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CLEAR STATE
    // ═══════════════════════════════════════════════════════════════════════
    
    public PassBuilder setClearColor(float r, float g, float b, float a) {
        node.clearState.clearColor = new float[]{r, g, b, a};
        node.clearState.colorLoadOp = RenderPassNode.ClearState.LoadOp.CLEAR;
        return this;
    }
    
    public PassBuilder setClearDepth(float depth) {
        node.clearState.clearDepth = depth;
        node.clearState.depthLoadOp = RenderPassNode.ClearState.LoadOp.CLEAR;
        return this;
    }
    
    public PassBuilder setClearStencil(int stencil) {
        node.clearState.clearStencil = stencil;
        node.clearState.stencilLoadOp = RenderPassNode.ClearState.LoadOp.CLEAR;
        return this;
    }
    
    public PassBuilder setClearDepthStencil(float depth, int stencil) {
        return setClearDepth(depth).setClearStencil(stencil);
    }
    
    public PassBuilder setColorLoadOp(RenderPassNode.ClearState.LoadOp op) {
        node.clearState.colorLoadOp = op;
        return this;
    }
    
    public PassBuilder setDepthLoadOp(RenderPassNode.ClearState.LoadOp op) {
        node.clearState.depthLoadOp = op;
        return this;
    }
    
    public PassBuilder setStencilLoadOp(RenderPassNode.ClearState.LoadOp op) {
        node.clearState.stencilLoadOp = op;
        return this;
    }
    
    public PassBuilder setColorStoreOp(RenderPassNode.ClearState.StoreOp op) {
        node.clearState.colorStoreOp = op;
        return this;
    }
    
    public PassBuilder setDepthStoreOp(RenderPassNode.ClearState.StoreOp op) {
        node.clearState.depthStoreOp = op;
        return this;
    }
    
    public PassBuilder setStencilStoreOp(RenderPassNode.ClearState.StoreOp op) {
        node.clearState.stencilStoreOp = op;
        return this;
    }
    
    public PassBuilder loadColor() {
        node.clearState.colorLoadOp = RenderPassNode.ClearState.LoadOp.LOAD;
        return this;
    }
    
    public PassBuilder loadDepth() {
        node.clearState.depthLoadOp = RenderPassNode.ClearState.LoadOp.LOAD;
        return this;
    }
    
    public PassBuilder dontCareColor() {
        node.clearState.colorLoadOp = RenderPassNode.ClearState.LoadOp.DONT_CARE;
        return this;
    }
    
    public PassBuilder discardColor() {
        node.clearState.colorStoreOp = RenderPassNode.ClearState.StoreOp.DONT_CARE;
        return this;
    }
    
    public PassBuilder discardDepth() {
        node.clearState.depthStoreOp = RenderPassNode.ClearState.StoreOp.DONT_CARE;
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VIEWPORT & SCISSOR
    // ═══════════════════════════════════════════════════════════════════════
    
    public PassBuilder setViewport(int x, int y, int width, int height) {
        node.viewport.x = x;
        node.viewport.y = y;
        node.viewport.width = width;
        node.viewport.height = height;
        return this;
    }
    
    public PassBuilder setViewportDepthRange(float minDepth, float maxDepth) {
        node.viewport.minDepth = minDepth;
        node.viewport.maxDepth = maxDepth;
        return this;
    }
    
    public PassBuilder setScissors(int... scissors) {
        node.viewport.scissors = scissors;
        return this;
    }
    
    public PassBuilder setLayerRange(int baseLayer, int layerCount) {
        node.viewport.layerBase = baseLayer;
        node.viewport.layerCount = layerCount;
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VARIABLE RATE SHADING
    // ═══════════════════════════════════════════════════════════════════════
    
    public PassBuilder enableVRS() {
        node.vrs.enabled = true;
        return this;
    }
    
    public PassBuilder setVRSRate(RenderPassNode.VariableRateShading.ShadingRate rate) {
        node.vrs.enabled = true;
        node.vrs.baseRate = rate;
        return this;
    }
    
    public PassBuilder setVRSCombiners(RenderPassNode.VariableRateShading.CombinerOp pipelineCombiner,
                                        RenderPassNode.VariableRateShading.CombinerOp imageCombiner) {
        node.vrs.combiners[0] = pipelineCombiner;
        node.vrs.combiners[1] = imageCombiner;
        return this;
    }
    
    public PassBuilder setVRSImage(ResourceNode shadingRateImage) {
        node.vrs.enabled = true;
        node.vrs.shadingRateImage = shadingRateImage;
        read(shadingRateImage, RenderPassNode.ResourceState.SHADER_READ, -1);
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXECUTION HINTS
    // ═══════════════════════════════════════════════════════════════════════
    
    public PassBuilder setPreferredQueue(RenderPassNode.ExecutionHints.QueueType queue) {
        node.hints.preferredQueue = queue;
        return this;
    }
    
    public PassBuilder setThreadAffinity(int mask) {
        node.hints.threadAffinityMask = mask;
        return this;
    }
    
    public PassBuilder disableAsyncRecording() {
        node.hints.allowAsyncRecording = false;
        return this;
    }
    
    public PassBuilder setEstimatedDrawCalls(int count) {
        node.hints.estimatedDrawCalls = count;
        return this;
    }
    
    public PassBuilder setEstimatedDispatches(int count) {
        node.hints.estimatedDispatchCount = count;
        return this;
    }
    
    public PassBuilder setPriority(float priority) {
        node.hints.priority = Math.clamp(priority, 0f, 1f);
        return this;
    }
    
    public PassBuilder highPriority() {
        node.flags.add(RenderPassNode.PassFlags.HIGH_PRIORITY);
        node.hints.priority = 1.0f;
        return this;
    }
    
    public PassBuilder lowPriority() {
        node.flags.add(RenderPassNode.PassFlags.LOW_PRIORITY);
        node.hints.priority = 0.0f;
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // FLAGS
    // ═══════════════════════════════════════════════════════════════════════
    
    public PassBuilder addFlags(RenderPassNode.PassFlags... flags) {
        for (var flag : flags) {
            node.flags.add(flag);
        }
        return this;
    }
    
    public PassBuilder forceSingleThreaded() {
        node.flags.add(RenderPassNode.PassFlags.FORCE_SINGLE_THREADED);
        return this;
    }
    
    public PassBuilder disableCulling() {
        node.flags.add(RenderPassNode.PassFlags.DISABLE_CULLING);
        return this;
    }
    
    public PassBuilder allowMerging() {
        node.flags.add(RenderPassNode.PassFlags.ALLOW_MERGING);
        return this;
    }
    
    public PassBuilder enableIndirectDispatch() {
        node.flags.add(RenderPassNode.PassFlags.INDIRECT_DISPATCH);
        return this;
    }
    
    public PassBuilder enableConditionalRendering() {
        node.flags.add(RenderPassNode.PassFlags.CONDITIONAL_RENDERING);
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // PROFILING
    // ═══════════════════════════════════════════════════════════════════════
    
    public PassBuilder profileGPU() {
        node.flags.add(RenderPassNode.PassFlags.PROFILE_GPU);
        return this;
    }
    
    public PassBuilder profileCPU() {
        node.flags.add(RenderPassNode.PassFlags.PROFILE_CPU);
        return this;
    }
    
    public PassBuilder profile() {
        return profileGPU().profileCPU();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONDITIONAL EXECUTION
    // ═══════════════════════════════════════════════════════════════════════
    
    public PassBuilder setCondition(Predicate<RenderPassNode.FrameContext> predicate) {
        node.conditionalPredicate = predicate;
        return this;
    }
    
    public PassBuilder executeEveryNthFrame(int n) {
        node.conditionalPredicate = ctx -> ctx.frameIndex() % n == 0;
        return this;
    }
    
    public PassBuilder skipFirstNFrames(int n) {
        node.conditionalPredicate = ctx -> ctx.frameIndex() >= n;
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXECUTOR
    // ═══════════════════════════════════════════════════════════════════════
    
    public PassBuilder setExecutor(RenderPassNode.PassExecutor executor) {
        node.executor = executor;
        return this;
    }
    
    public PassBuilder setAsyncRecordCallback(RenderPassNode.AsyncRecordCallback callback) {
        node.asyncRecordCallback = callback;
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // BUILD
    // ═══════════════════════════════════════════════════════════════════════
    
    public RenderPassNode build() {
        return node;
    }
    
    public RenderGraph endPass() {
        return graph;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════════════
    
    private RenderPassNode.ResourceState inferReadState(ResourceNode resource) {
        return switch (resource.type) {
            case BUFFER -> RenderPassNode.ResourceState.SHADER_READ;
            case ACCELERATION_STRUCTURE -> RenderPassNode.ResourceState.RAY_TRACING_ACCELERATION_STRUCTURE;
            default -> RenderPassNode.ResourceState.SHADER_READ;
        };
    }
    
    private RenderPassNode.ResourceState inferWriteState(ResourceNode resource) {
        if (resource.flags.contains(ResourceNode.ResourceFlags.COLOR_ATTACHMENT)) {
            return RenderPassNode.ResourceState.COLOR_ATTACHMENT;
        }
        if (resource.flags.contains(ResourceNode.ResourceFlags.DEPTH_STENCIL_ATTACHMENT)) {
            return RenderPassNode.ResourceState.DEPTH_STENCIL_ATTACHMENT;
        }
        return switch (resource.type) {
            case BUFFER -> RenderPassNode.ResourceState.SHADER_WRITE;
            default -> RenderPassNode.ResourceState.COLOR_ATTACHMENT;
        };
    }
}
