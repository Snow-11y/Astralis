package stellar.snow.astralis.engine.render.graph;

import stellar.snow.astralis.api.vulkan.memory.MemoryAllocation;

import java.util.*;

/**
 * Represents a GPU resource (Texture/Buffer) in the Render Graph.
 * 
 * Features:
 * - Full texture/buffer metadata
 * - Transient vs persistent resources
 * - Memory aliasing support
 * - State tracking for barriers
 * - Version tracking for temporal resources
 * - Debug labeling
 */
public final class ResourceNode {
    
    // ═══════════════════════════════════════════════════════════════════════
    // ENUMS
    // ═══════════════════════════════════════════════════════════════════════
    
    public enum Type {
        TEXTURE_2D,
        TEXTURE_3D,
        TEXTURE_CUBE,
        TEXTURE_2D_ARRAY,
        TEXTURE_CUBE_ARRAY,
        BUFFER,
        ACCELERATION_STRUCTURE
    }
    
    public enum Lifetime {
        /** Created and destroyed within a single frame */
        TRANSIENT,
        /** Persists across frames */
        PERSISTENT,
        /** Imported from external source (swapchain, etc.) */
        IMPORTED,
        /** History buffer (previous frame's version) */
        HISTORY
    }
    
    public enum ResourceState {
        UNDEFINED,
        GENERAL,
        COLOR_ATTACHMENT,
        DEPTH_STENCIL_ATTACHMENT,
        DEPTH_STENCIL_READ_ONLY,
        SHADER_READ_ONLY,
        TRANSFER_SRC,
        TRANSFER_DST,
        PRESENT,
        STORAGE_READ,
        STORAGE_WRITE,
        STORAGE_READ_WRITE
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // USAGE FLAGS
    // ═══════════════════════════════════════════════════════════════════════
    
    public static final int USAGE_SAMPLED = 1 << 0;
    public static final int USAGE_STORAGE = 1 << 1;
    public static final int USAGE_COLOR_ATTACHMENT = 1 << 2;
    public static final int USAGE_DEPTH_STENCIL_ATTACHMENT = 1 << 3;
    public static final int USAGE_INPUT_ATTACHMENT = 1 << 4;
    public static final int USAGE_TRANSFER_SRC = 1 << 5;
    public static final int USAGE_TRANSFER_DST = 1 << 6;
    public static final int USAGE_VERTEX_BUFFER = 1 << 7;
    public static final int USAGE_INDEX_BUFFER = 1 << 8;
    public static final int USAGE_UNIFORM_BUFFER = 1 << 9;
    public static final int USAGE_STORAGE_BUFFER = 1 << 10;
    public static final int USAGE_INDIRECT_BUFFER = 1 << 11;
    public static final int USAGE_ACCELERATION_STRUCTURE = 1 << 12;
    
    // ═══════════════════════════════════════════════════════════════════════
    // CORE PROPERTIES
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Unique identifier within the render graph */
    public final String name;
    
    /** Resource type */
    public final Type type;
    
    /** Lifetime/persistence */
    public Lifetime lifetime = Lifetime.TRANSIENT;
    
    /** Version for temporal resources (incremented each frame) */
    public int version = 0;
    
    // ═══════════════════════════════════════════════════════════════════════
    // HANDLES
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Vulkan image/buffer handle */
    public long handle = 0;
    
    /** Image view handle (for textures) */
    public long viewHandle = 0;
    
    /** Memory allocation backing this resource */
    public MemoryAllocation allocation;
    
    /** Device address (for bindless buffers) */
    public long deviceAddress = 0;
    
    // ═══════════════════════════════════════════════════════════════════════
    // TEXTURE PROPERTIES
    // ═══════════════════════════════════════════════════════════════════════
    
    public int width = 0;
    public int height = 0;
    public int depth = 1;
    public int mipLevels = 1;
    public int arrayLayers = 1;
    public int samples = 1;           // MSAA sample count
    public int format = 0;            // VkFormat
    public int aspectMask = 0;        // VkImageAspectFlags
    
    // ═══════════════════════════════════════════════════════════════════════
    // BUFFER PROPERTIES
    // ═══════════════════════════════════════════════════════════════════════
    
    public long size = 0;
    public long offset = 0;           // For buffer views/sub-ranges
    public int stride = 0;            // Element stride for structured buffers
    
    // ═══════════════════════════════════════════════════════════════════════
    // USAGE & STATE
    // ═══════════════════════════════════════════════════════════════════════
    
    public int usageFlags = 0;
    public ResourceState currentState = ResourceState.UNDEFINED;
    public int currentQueueFamily = -1;
    
    // ═══════════════════════════════════════════════════════════════════════
    // DEPENDENCIES
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Pass that creates/writes this resource */
    public RenderPassNode producer;
    
    /** Passes that read this resource */
    public final List<RenderPassNode> consumers = new ArrayList<>();
    
    /** Last pass that wrote to this resource (for barrier insertion) */
    public RenderPassNode lastWriter;
    
    /** First pass that reads after a write (for barrier insertion) */
    public RenderPassNode firstReaderAfterWrite;
    
    // ═══════════════════════════════════════════════════════════════════════
    // ALIASING
    // ═══════════════════════════════════════════════════════════════════════
    
    /** If aliased, points to the resource sharing memory */
    public ResourceNode aliasTarget;
    
    /** Resources that alias with this one */
    public final List<ResourceNode> aliases = new ArrayList<>();
    
    /** Whether this resource can be aliased */
    public boolean canAlias = true;
    
    // ═══════════════════════════════════════════════════════════════════════
    // TEMPORAL (History buffers)
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Previous frame's version of this resource */
    public ResourceNode historyResource;
    
    /** Whether this resource needs history preservation */
    public boolean needsHistory = false;
    
    // ═══════════════════════════════════════════════════════════════════════
    // DEBUG
    // ═══════════════════════════════════════════════════════════════════════
    
    public String debugLabel;
    public Object userData;
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTION
    // ═══════════════════════════════════════════════════════════════════════
    
    public ResourceNode(String name, Type type) {
        this.name = name;
        this.type = type;
        this.debugLabel = name;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // BUILDERS
    // ═══════════════════════════════════════════════════════════════════════
    
    public static ResourceNode texture2D(String name) {
        return new ResourceNode(name, Type.TEXTURE_2D);
    }
    
    public static ResourceNode texture3D(String name) {
        return new ResourceNode(name, Type.TEXTURE_3D);
    }
    
    public static ResourceNode textureCube(String name) {
        return new ResourceNode(name, Type.TEXTURE_CUBE);
    }
    
    public static ResourceNode buffer(String name) {
        return new ResourceNode(name, Type.BUFFER);
    }
    
    public static ResourceNode accelerationStructure(String name) {
        return new ResourceNode(name, Type.ACCELERATION_STRUCTURE);
    }
    
    // Fluent setters
    public ResourceNode size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }
    
    public ResourceNode size(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        return this;
    }
    
    public ResourceNode format(int vkFormat) {
        this.format = vkFormat;
        return this;
    }
    
    public ResourceNode mipLevels(int levels) {
        this.mipLevels = levels;
        return this;
    }
    
    public ResourceNode arrayLayers(int layers) {
        this.arrayLayers = layers;
        return this;
    }
    
    public ResourceNode samples(int sampleCount) {
        this.samples = sampleCount;
        return this;
    }
    
    public ResourceNode bufferSize(long sizeBytes) {
        this.size = sizeBytes;
        return this;
    }
    
    public ResourceNode stride(int elementStride) {
        this.stride = elementStride;
        return this;
    }
    
    public ResourceNode usage(int flags) {
        this.usageFlags |= flags;
        return this;
    }
    
    public ResourceNode lifetime(Lifetime lt) {
        this.lifetime = lt;
        return this;
    }
    
    public ResourceNode transient_() {
        this.lifetime = Lifetime.TRANSIENT;
        return this;
    }
    
    public ResourceNode persistent() {
        this.lifetime = Lifetime.PERSISTENT;
        return this;
    }
    
    public ResourceNode withHistory() {
        this.needsHistory = true;
        return this;
    }
    
    public ResourceNode noAlias() {
        this.canAlias = false;
        return this;
    }
    
    public ResourceNode label(String label) {
        this.debugLabel = label;
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════════════════
    
    public boolean isTexture() {
        return type == Type.TEXTURE_2D || type == Type.TEXTURE_3D ||
               type == Type.TEXTURE_CUBE || type == Type.TEXTURE_2D_ARRAY ||
               type == Type.TEXTURE_CUBE_ARRAY;
    }
    
    public boolean isBuffer() {
        return type == Type.BUFFER;
    }
    
    public boolean isDepthStencil() {
        // Check format for depth/stencil
        return (aspectMask & 0x02) != 0 || // VK_IMAGE_ASPECT_DEPTH_BIT
               (aspectMask & 0x04) != 0;   // VK_IMAGE_ASPECT_STENCIL_BIT
    }
    
    public boolean isTransient() {
        return lifetime == Lifetime.TRANSIENT;
    }
    
    public boolean isPersistent() {
        return lifetime == Lifetime.PERSISTENT;
    }
    
    public boolean isImported() {
        return lifetime == Lifetime.IMPORTED;
    }
    
    public boolean isAliased() {
        return aliasTarget != null || !aliases.isEmpty();
    }
    
    public boolean hasValidHandle() {
        return handle != 0;
    }
    
    public long getMemorySize() {
        if (isBuffer()) {
            return size;
        } else {
            // Estimate texture size
            int bpp = getFormatBytesPerPixel(format);
            long baseSize = (long) width * height * depth * bpp;
            
            // Account for mip levels
            if (mipLevels > 1) {
                baseSize = (long) (baseSize * 1.334); // ~4/3 for full mip chain
            }
            
            // Account for array layers and samples
            baseSize *= arrayLayers * samples;
            
            return baseSize;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // STATE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Record a state transition (for barrier generation).
     */
    public StateTransition transitionTo(ResourceState newState) {
        StateTransition transition = new StateTransition(this, currentState, newState);
        currentState = newState;
        return transition;
    }
    
    public record StateTransition(
        ResourceNode resource,
        ResourceState oldState,
        ResourceState newState
    ) {
        public boolean needsBarrier() {
            return oldState != newState;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // ALIASING
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Setup memory aliasing with another resource.
     */
    public void aliasFrom(ResourceNode other) {
        this.aliasTarget = other;
        this.handle = other.handle;
        this.viewHandle = other.viewHandle;
        this.allocation = other.allocation;
        other.aliases.add(this);
    }
    
    /**
     * Check if this resource can alias with another.
     */
    public boolean canAliasWith(ResourceNode other) {
        if (!canAlias || !other.canAlias) return false;
        if (lifetime != Lifetime.TRANSIENT || other.lifetime != Lifetime.TRANSIENT) return false;
        if (needsHistory || other.needsHistory) return false;
        
        // Check memory compatibility
        return getMemorySize() <= other.getMemorySize() &&
               isTexture() == other.isTexture();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // HISTORY (Temporal Resources)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Create history resource for temporal effects.
     */
    public ResourceNode createHistoryResource() {
        ResourceNode history = new ResourceNode(name + "_history", type);
        history.width = width;
        history.height = height;
        history.depth = depth;
        history.format = format;
        history.mipLevels = mipLevels;
        history.arrayLayers = arrayLayers;
        history.samples = samples;
        history.usageFlags = usageFlags;
        history.lifetime = Lifetime.HISTORY;
        history.version = version - 1;
        
        this.historyResource = history;
        return history;
    }
    
    /**
     * Swap with history resource (end of frame).
     */
    public void swapWithHistory() {
        if (historyResource == null) return;
        
        // Swap handles
        long tempHandle = handle;
        long tempView = viewHandle;
        MemoryAllocation tempAlloc = allocation;
        
        handle = historyResource.handle;
        viewHandle = historyResource.viewHandle;
        allocation = historyResource.allocation;
        
        historyResource.handle = tempHandle;
        historyResource.viewHandle = tempView;
        historyResource.allocation = tempAlloc;
        
        // Update versions
        version++;
        historyResource.version = version - 1;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════
    
    private static int getFormatBytesPerPixel(int vkFormat) {
        // Common formats
        return switch (vkFormat) {
            case 37 -> 1;    // R8_UNORM
            case 43 -> 2;    // R8G8_UNORM
            case 44 -> 4;    // R8G8B8A8_UNORM
            case 97 -> 4;    // R16G16_SFLOAT
            case 100 -> 8;   // R16G16B16A16_SFLOAT
            case 109 -> 16;  // R32G32B32A32_SFLOAT
            case 126 -> 4;   // D32_SFLOAT
            case 129 -> 5;   // D24_UNORM_S8_UINT
            case 130 -> 8;   // D32_SFLOAT_S8_UINT
            default -> 4;    // Default assumption
        };
    }
    
    @Override
    public String toString() {
        if (isTexture()) {
            return String.format("ResourceNode{%s, %s, %dx%dx%d, fmt=%d, mips=%d, layers=%d, %s}",
                name, type, width, height, depth, format, mipLevels, arrayLayers, lifetime);
        } else {
            return String.format("ResourceNode{%s, %s, size=%d, stride=%d, %s}",
                name, type, size, stride, lifetime);
        }
    }
    
    /**
     * Detailed debug information.
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ResourceNode: ").append(name).append("\n");
        sb.append("  Type: ").append(type).append("\n");
        sb.append("  Lifetime: ").append(lifetime).append("\n");
        sb.append("  Handle: 0x").append(Long.toHexString(handle)).append("\n");
        
        if (isTexture()) {
            sb.append("  Size: ").append(width).append("x").append(height);
            if (depth > 1) sb.append("x").append(depth);
            sb.append("\n");
            sb.append("  Format: ").append(format).append("\n");
            sb.append("  Mip Levels: ").append(mipLevels).append("\n");
            sb.append("  Array Layers: ").append(arrayLayers).append("\n");
            sb.append("  Samples: ").append(samples).append("\n");
        } else {
            sb.append("  Size: ").append(size).append(" bytes\n");
            if (stride > 0) sb.append("  Stride: ").append(stride).append("\n");
        }
        
        sb.append("  Usage: 0x").append(Integer.toHexString(usageFlags)).append("\n");
        sb.append("  State: ").append(currentState).append("\n");
        sb.append("  Device Address: 0x").append(Long.toHexString(deviceAddress)).append("\n");
        
        if (producer != null) {
            sb.append("  Producer: ").append(producer.name).append("\n");
        }
        if (!consumers.isEmpty()) {
            sb.append("  Consumers: ");
            for (RenderPassNode c : consumers) {
                sb.append(c.name).append(", ");
            }
            sb.append("\n");
        }
        if (isAliased()) {
            sb.append("  Aliased: yes\n");
        }
        if (needsHistory) {
            sb.append("  History: yes (version ").append(version).append(")\n");
        }
        
        return sb.toString();
    }
}
