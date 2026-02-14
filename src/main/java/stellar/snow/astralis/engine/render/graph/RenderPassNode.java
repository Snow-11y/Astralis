package stellar.snow.astralis.engine.render.graph;
import stellar.snow.astralis.engine.gpu.authority.GPUBackend;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
/**
 * RenderPassNode - A single atomic operation in the Render Graph.
 * 
 * <p>Supports modern rendering paradigms including:</p>
 * <ul>
 *   <li>Graphics, Compute, Transfer, and Ray Tracing passes</li>
 *   <li>Async compute with queue family hints</li>
 *   <li>Variable Rate Shading (VRS)</li>
 *   <li>Conditional execution</li>
 *   <li>GPU/CPU profiling integration</li>
 *   <li>Split barriers for enhanced parallelism</li>
 * </ul>
 */
    
    private static final AtomicLong ID_GENERATOR = new AtomicLong();
    
    // ═══════════════════════════════════════════════════════════════════════
    // IDENTIFICATION
    // ═══════════════════════════════════════════════════════════════════════
    
    public final long id = ID_GENERATOR.incrementAndGet();
    public final String name;
    public final PassType type;
    public final EnumSet<PassFlags> flags = EnumSet.noneOf(PassFlags.class);
    
    // ═══════════════════════════════════════════════════════════════════════
    // DEPENDENCIES
    // ═══════════════════════════════════════════════════════════════════════
    
    public final List<ResourceAccess> inputs = new ArrayList<>();
    public final List<ResourceAccess> outputs = new ArrayList<>();
    public final List<RenderPassNode> explicitDependencies = new ArrayList<>();
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXECUTOR
    // ═══════════════════════════════════════════════════════════════════════
    
    public PassExecutor executor;
    public AsyncRecordCallback asyncRecordCallback;
    
    // ═══════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════
    
    public final ClearState clearState = new ClearState();
    public final ExecutionHints hints = new ExecutionHints();
    public final ViewportState viewport = new ViewportState();
    public final VariableRateShading vrs = new VariableRateShading();
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONDITIONAL & PROFILING
    // ═══════════════════════════════════════════════════════════════════════
    
    public Predicate<FrameContext> conditionalPredicate;
    public volatile long gpuTimeNanos;
    public volatile long cpuTimeNanos;
    public volatile boolean wasCulled;
    
    // ═══════════════════════════════════════════════════════════════════════
    // ENUMS & RECORDS
    // ═══════════════════════════════════════════════════════════════════════
    
    public enum PassType {
        GRAPHICS,
        COMPUTE,
        ASYNC_COMPUTE,
        TRANSFER,
        RAY_TRACING,
        MESH_SHADING,
        PRESENT
    }
    
    public enum PassFlags {
        FORCE_SINGLE_THREADED,
        DISABLE_CULLING,
        HIGH_PRIORITY,
        LOW_PRIORITY,
        ALLOW_MERGING,
        PROFILE_GPU,
        PROFILE_CPU,
        SPLIT_BARRIER_RELEASE,
        SPLIT_BARRIER_ACQUIRE,
        INDIRECT_DISPATCH,
        CONDITIONAL_RENDERING
    }
    
    public enum AccessType {
        READ,
        WRITE,
        READ_WRITE
    }
    
    public enum ResourceState {
        UNDEFINED,
        COLOR_ATTACHMENT,
        DEPTH_STENCIL_ATTACHMENT,
        SHADER_READ,
        SHADER_WRITE,
        TRANSFER_SRC,
        TRANSFER_DST,
        PRESENT,
        GENERAL,
        RAY_TRACING_ACCELERATION_STRUCTURE
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // NESTED CLASSES
    // ═══════════════════════════════════════════════════════════════════════
    
    public static final class ResourceAccess {
        public final ResourceNode resource;
        public final AccessType accessType;
        public final ResourceState requiredState;
        public final int subresourceBase;
        public final int subresourceCount;
        public final int binding;
        
        public ResourceAccess(ResourceNode resource, AccessType accessType, 
                             ResourceState state, int binding) {
            this(resource, accessType, state, 0, -1, binding);
        }
        
        public ResourceAccess(ResourceNode resource, AccessType accessType,
                             ResourceState state, int subBase, int subCount, int binding) {
            this.resource = resource;
            this.accessType = accessType;
            this.requiredState = state;
            this.subresourceBase = subBase;
            this.subresourceCount = subCount;
            this.binding = binding;
        }
    }
    
    public static final class ClearState {
        public float[] clearColor = {0f, 0f, 0f, 1f};
        public float clearDepth = 1.0f;
        public int clearStencil = 0;
        public LoadOp colorLoadOp = LoadOp.CLEAR;
        public LoadOp depthLoadOp = LoadOp.CLEAR;
        public LoadOp stencilLoadOp = LoadOp.DONT_CARE;
        public StoreOp colorStoreOp = StoreOp.STORE;
        public StoreOp depthStoreOp = StoreOp.STORE;
        public StoreOp stencilStoreOp = StoreOp.DONT_CARE;
        public ResourceNode resolveTarget;
        
        public enum LoadOp { LOAD, CLEAR, DONT_CARE }
        public enum StoreOp { STORE, DONT_CARE, NONE }
    }
    
    public static final class ExecutionHints {
        public QueueType preferredQueue = QueueType.GRAPHICS;
        public int threadAffinityMask = -1;
        public boolean allowAsyncRecording = true;
        public int estimatedDrawCalls = 0;
        public int estimatedDispatchCount = 0;
        public long estimatedMemoryFootprint = 0;
        public float priority = 0.5f;
        
        public enum QueueType { GRAPHICS, COMPUTE, TRANSFER, SPARSE_BINDING }
    }
    
    public static final class ViewportState {
        public int x, y;
        public int width = -1;  // -1 = auto from attachment
        public int height = -1;
        public float minDepth = 0f;
        public float maxDepth = 1f;
        public int[] scissors;  // null = match viewport
        public int layerBase = 0;
        public int layerCount = 1;
    }
    
    public static final class VariableRateShading {
        public boolean enabled = false;
        public ShadingRate baseRate = ShadingRate._1X1;
        public CombinerOp[] combiners = {CombinerOp.KEEP, CombinerOp.KEEP};
        public ResourceNode shadingRateImage;
        
        public enum ShadingRate {
            _1X1, _1X2, _2X1, _2X2, _2X4, _4X2, _4X4
        }
        
        public enum CombinerOp {
            KEEP, REPLACE, MIN, MAX, MUL
        }
    }
    
    public record FrameContext(
        int frameIndex,
        double deltaTime,
        long frameStartNanos,
        Map<String, Object> userData
    ) {}
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTORS
    // ═══════════════════════════════════════════════════════════════════════
    
    public RenderPassNode(String name) {
        this(name, PassType.GRAPHICS);
    }
    
    public RenderPassNode(String name, PassType type) {
        this.name = Objects.requireNonNull(name, "Pass name cannot be null");
        this.type = Objects.requireNonNull(type, "Pass type cannot be null");
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // RENDER PASS INFO GENERATION
    // ═══════════════════════════════════════════════════════════════════════
    
    public GPUBackend.RenderPassInfo getRenderPassInfo() {
        var info = new GPUBackend.RenderPassInfo();
        info.name = this.name;
        info.clearOnLoad = clearState.colorLoadOp == ClearState.LoadOp.CLEAR;
        info.clearColor = clearState.clearColor.clone();
        info.clearDepth = clearState.clearDepth;
        
        var colorAttachments = new ArrayList<GPUBackend.AttachmentInfo>();
        GPUBackend.AttachmentInfo depthAttachment = null;
        
        for (ResourceAccess access : outputs) {
            if (access.resource.type == ResourceNode.Type.TEXTURE) {
                var attachInfo = new GPUBackend.AttachmentInfo();
                attachInfo.handle = access.resource.handle;
                attachInfo.loadOp = mapLoadOp(clearState.colorLoadOp);
                attachInfo.storeOp = mapStoreOp(clearState.colorStoreOp);
                attachInfo.initialState = mapState(access.requiredState);
                attachInfo.finalState = mapState(access.requiredState);
                
                if (isDepthFormat(access.resource.format)) {
                    attachInfo.loadOp = mapLoadOp(clearState.depthLoadOp);
                    attachInfo.storeOp = mapStoreOp(clearState.depthStoreOp);
                    depthAttachment = attachInfo;
                    info.depthAttachment = access.resource.handle;
                } else {
                    colorAttachments.add(attachInfo);
                }
            }
        }
        
        info.colorAttachments = colorAttachments.stream()
            .mapToLong(a -> a.handle)
            .toArray();
        
        // Resolve viewport dimensions
        if (viewport.width > 0 && viewport.height > 0) {
            info.width = viewport.width;
            info.height = viewport.height;
        } else if (!outputs.isEmpty()) {
            info.width = outputs.get(0).resource.width;
            info.height = outputs.get(0).resource.height;
        } else {
            info.width = 1920;
            info.height = 1080;
        }
        
        // VRS attachment
        if (vrs.enabled && vrs.shadingRateImage != null) {
            info.shadingRateAttachment = vrs.shadingRateImage.handle;
            info.shadingRateTexelWidth = getShadingRateTexelSize(vrs.baseRate);
            info.shadingRateTexelHeight = getShadingRateTexelSize(vrs.baseRate);
        }
        
        // Multi-view/Multisampling
        info.viewMask = (1 << viewport.layerCount) - 1;
        info.correlationMask = info.viewMask;
        
        return info;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // BARRIER GENERATION
    // ═══════════════════════════════════════════════════════════════════════
    
    public List<BarrierInfo> generateInputBarriers() {
        var barriers = new ArrayList<BarrierInfo>();
        
        for (ResourceAccess access : inputs) {
            var barrier = new BarrierInfo();
            barrier.resource = access.resource;
            barrier.srcState = access.resource.currentState;
            barrier.dstState = access.requiredState;
            barrier.srcStageMask = inferStageMask(access.resource.currentState);
            barrier.dstStageMask = inferStageMask(access.requiredState);
            barrier.srcAccessMask = inferAccessMask(access.resource.currentState, false);
            barrier.dstAccessMask = inferAccessMask(access.requiredState, 
                access.accessType == AccessType.WRITE || access.accessType == AccessType.READ_WRITE);
            barrier.subresourceBase = access.subresourceBase;
            barrier.subresourceCount = access.subresourceCount;
            
            if (barrier.srcState != barrier.dstState || barrier.srcAccessMask != barrier.dstAccessMask) {
                barriers.add(barrier);
            }
        }
        
        return barriers;
    }
    
    public List<BarrierInfo> generateOutputBarriers() {
        var barriers = new ArrayList<BarrierInfo>();
        
        for (ResourceAccess access : outputs) {
            access.resource.currentState = access.requiredState;
        }
        
        return barriers;
    }
    
    public static final class BarrierInfo {
        public ResourceNode resource;
        public ResourceState srcState;
        public ResourceState dstState;
        public int srcStageMask;
        public int dstStageMask;
        public int srcAccessMask;
        public int dstAccessMask;
        public int subresourceBase;
        public int subresourceCount;
        public boolean splitBarrier;
        public long splitBarrierEvent;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════
    
    private boolean isDepthFormat(int format) {
        return format == GPUBackend.TextureFormat.DEPTH24_STENCIL8 
            || format == GPUBackend.TextureFormat.DEPTH32F
            || format == GPUBackend.TextureFormat.DEPTH16
            || format == GPUBackend.TextureFormat.DEPTH32F_STENCIL8;
    }
    
    private int mapLoadOp(ClearState.LoadOp op) {
        return switch (op) {
            case LOAD -> 0;
            case CLEAR -> 1;
            case DONT_CARE -> 2;
        };
    }
    
    private int mapStoreOp(ClearState.StoreOp op) {
        return switch (op) {
            case STORE -> 0;
            case DONT_CARE -> 1;
            case NONE -> 2;
        };
    }
    
    private int mapState(ResourceState state) {
        return state.ordinal();
    }
    
    private int inferStageMask(ResourceState state) {
        return switch (state) {
            case COLOR_ATTACHMENT -> GPUBackend.PipelineStage.COLOR_ATTACHMENT_OUTPUT;
            case DEPTH_STENCIL_ATTACHMENT -> GPUBackend.PipelineStage.EARLY_FRAGMENT_TESTS 
                                            | GPUBackend.PipelineStage.LATE_FRAGMENT_TESTS;
            case SHADER_READ, SHADER_WRITE -> GPUBackend.PipelineStage.FRAGMENT_SHADER 
                                             | GPUBackend.PipelineStage.COMPUTE_SHADER;
            case TRANSFER_SRC, TRANSFER_DST -> GPUBackend.PipelineStage.TRANSFER;
            case RAY_TRACING_ACCELERATION_STRUCTURE -> GPUBackend.PipelineStage.RAY_TRACING_SHADER;
            case PRESENT -> GPUBackend.PipelineStage.BOTTOM_OF_PIPE;
            default -> GPUBackend.PipelineStage.ALL_COMMANDS;
        };
    }
    
    private int inferAccessMask(ResourceState state, boolean write) {
        return switch (state) {
            case COLOR_ATTACHMENT -> write ? GPUBackend.AccessFlags.COLOR_ATTACHMENT_WRITE 
                                           : GPUBackend.AccessFlags.COLOR_ATTACHMENT_READ;
            case DEPTH_STENCIL_ATTACHMENT -> write ? GPUBackend.AccessFlags.DEPTH_STENCIL_ATTACHMENT_WRITE
                                                   : GPUBackend.AccessFlags.DEPTH_STENCIL_ATTACHMENT_READ;
            case SHADER_READ -> GPUBackend.AccessFlags.SHADER_READ;
            case SHADER_WRITE -> GPUBackend.AccessFlags.SHADER_WRITE;
            case TRANSFER_SRC -> GPUBackend.AccessFlags.TRANSFER_READ;
            case TRANSFER_DST -> GPUBackend.AccessFlags.TRANSFER_WRITE;
            default -> 0;
        };
    }
    
    private int getShadingRateTexelSize(VariableRateShading.ShadingRate rate) {
        return switch (rate) {
            case _1X1 -> 1;
            case _1X2, _2X1, _2X2 -> 2;
            case _2X4, _4X2, _4X4 -> 4;
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // INTERFACES
    // ═══════════════════════════════════════════════════════════════════════
    
    @FunctionalInterface
    public interface PassExecutor {
        void execute(GPUBackend backend, FrameContext context);
    }
    
    @FunctionalInterface
    public interface AsyncRecordCallback {
        void record(GPUBackend.CommandBuffer commandBuffer, FrameContext context);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // OBJECT METHODS
    // ═══════════════════════════════════════════════════════════════════════
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RenderPassNode that)) return false;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
    
    @Override
    public String toString() {
        return "RenderPassNode{id=%d, name='%s', type=%s, inputs=%d, outputs=%d}"
            .formatted(id, name, type, inputs.size(), outputs.size());
    }
}
