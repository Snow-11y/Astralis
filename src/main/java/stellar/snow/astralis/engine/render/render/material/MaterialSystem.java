package stellar.snow.astralis.engine.render.material;

// ═══════════════════════════════════════════════════════════════════════════════════════════════════
// ██████████████████████████████████████████████████████████████████████████████████████████████████
// ██                                                                                              ██
// ██   ███╗   ███╗ █████╗ ████████╗███████╗██████╗ ██╗ █████╗ ██╗         ███████╗██╗   ██╗    ██
// ██   ████╗ ████║██╔══██╗╚══██╔══╝██╔════╝██╔══██╗██║██╔══██╗██║         ██╔════╝╚██╗ ██╔╝    ██
// ██   ██╔████╔██║███████║   ██║   █████╗  ██████╔╝██║███████║██║         ███████╗ ╚████╔╝     ██
// ██   ██║╚██╔╝██║██╔══██║   ██║   ██╔══╝  ██╔══██╗██║██╔══██║██║         ╚════██║  ╚██╔╝      ██
// ██   ██║ ╚═╝ ██║██║  ██║   ██║   ███████╗██║  ██║██║██║  ██║███████╗    ███████║   ██║       ██
// ██   ╚═╝     ╚═╝╚═╝  ╚═╝   ╚═╝   ╚══════╝╚═╝  ╚═╝╚═╝╚═╝  ╚═╝╚══════╝    ╚══════╝   ╚═╝       ██
// ██                                                                                              ██
// ██    MATERIAL SYSTEM - JAVA 25 + UNIVERSAL GRAPHICS API                                     ██
// ██    Material Graphs | PBR Shading | Instancing | Parameter Blocks | Uber-Shaders           ██
// ██    Hot-Reload | Shader Permutations | Material LOD | Bindless Textures                     ██
// ██                                                                                              ██
// ██████████████████████████████████████████████████████████████████████████████████████████████████
// ═══════════════════════════════════════════════════════════════════════════════════════════════════

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.lang.foreign.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;
import java.util.stream.Collectors;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * MaterialSystem - Advanced material management with graph-based workflows.
 * 
 * <p><b>Core Features:</b></p>
 * <ul>
 *   <li>Graph-based material composition with visual node editor support</li>
 *   <li>PBR shading models (metallic-roughness, specular-glossiness, clear coat, cloth)</li>
 *   <li>Material instancing with instance parameters</li>
 *   <li>Uber-shader permutation system</li>
 *   <li>Hot-reloadable material assets</li>
 *   <li>Material LOD for performance scaling</li>
 *   <li>Bindless texture arrays (descriptor indexing)</li>
 *   <li>GPU parameter blocks (UBOs/SSBOs)</li>
 *   <li>Material sorting and batching</li>
 *   <li>Shader variant caching</li>
 * </ul>
 * 
 * <p><b>Material Graph Architecture:</b></p>
 * <pre>
 * Material Graph Node Types:
 * ├─ Input Nodes: UV, Normal, Position, Time, Custom Inputs
 * ├─ Texture Nodes: Texture2D, TextureCube, Virtual Texture
 * ├─ Math Nodes: Add, Multiply, Lerp, Dot, Cross, Normalize
 * ├─ Utility Nodes: Fresnel, Normal Map, Height to Normal
 * └─ Output Node: Base Color, Metallic, Roughness, Normal, Emission, Opacity
 * 
 * Evaluation: CPU-side for parameter extraction, GPU-side in shaders
 * </pre>
 * 
 * <p><b>PBR Shading Models:</b></p>
 * <ul>
 *   <li><b>Metallic-Roughness:</b> Standard PBR (glTF, UE5)</li>
 *   <li><b>Specular-Glossiness:</b> Legacy PBR workflow</li>
 *   <li><b>Clear Coat:</b> Car paint, lacquer (dual-layer BRDF)</li>
 *   <li><b>Cloth:</b> Velvet, fabric with anisotropic sheen</li>
 *   <li><b>Subsurface:</b> Skin, wax, jade (translucency)</li>
 *   <li><b>Anisotropic:</b> Brushed metal, hair</li>
 * </ul>
 * 
 * @author Stellar Snow Engine Team
 * @version 4.0.0
 */
public final class MaterialSystem implements AutoCloseable {
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    private static final int MAX_MATERIALS = 8192;
    private static final int MAX_MATERIAL_INSTANCES = 65536;
    private static final int MAX_TEXTURES = 4096;
    private static final int PARAMETER_BLOCK_SIZE = 256; // bytes per material instance
    private static final int MAX_SHADER_PERMUTATIONS = 1024;
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // VULKAN STATE
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    private final VkDevice device;
    private final VkPhysicalDevice physicalDevice;
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // MATERIAL STORAGE
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    private final Map<String, Material> materials;
    private final Map<Long, MaterialInstance> instances;
    private final AtomicLong nextInstanceId;
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // TEXTURE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    private final TextureRegistry textureRegistry;
    private final long textureDescriptorSet;
    private final long textureDescriptorSetLayout;
    private final long textureDescriptorPool;
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // PARAMETER BUFFERS
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    private final ParameterBufferManager parameterBuffers;
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // SHADER COMPILATION
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    private final ShaderPermutationCache permutationCache;
    private final Map<MaterialFeatureSet, Long> pipelineCache;
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // MATERIAL GRAPH
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    private final MaterialGraphEvaluator graphEvaluator;
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    private final MaterialStatistics statistics;
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // LOCKS
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    private final ReentrantReadWriteLock materialLock;
    private final ReentrantReadWriteLock textureLock;
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // ENUMS & DATA CLASSES
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    /**
     * PBR shading model enumeration.
     */
    public enum ShadingModel {
        METALLIC_ROUGHNESS("metallic_roughness"),
        SPECULAR_GLOSSINESS("specular_glossiness"),
        CLEAR_COAT("clear_coat"),
        CLOTH("cloth"),
        SUBSURFACE("subsurface"),
        ANISOTROPIC("anisotropic"),
        UNLIT("unlit");
        
        final String shaderDefine;
        
        ShadingModel(String shaderDefine) {
            this.shaderDefine = shaderDefine;
        }
    }
    
    /**
     * Blend mode for transparency.
     */
    public enum BlendMode {
        OPAQUE("opaque", false),
        MASKED("masked", false),
        TRANSLUCENT("translucent", true),
        ADDITIVE("additive", true),
        MODULATE("modulate", true);
        
        final String shaderDefine;
        final boolean requiresBlending;
        
        BlendMode(String shaderDefine, boolean requiresBlending) {
            this.shaderDefine = shaderDefine;
            this.requiresBlending = requiresBlending;
        }
    }
    
    /**
     * Material feature flags for shader permutations.
     */
    public static final class MaterialFeatureSet {
        private final BitSet features = new BitSet(32);
        
        public static final int FEATURE_NORMAL_MAP = 0;
        public static final int FEATURE_EMISSIVE = 1;
        public static final int FEATURE_AO_MAP = 2;
        public static final int FEATURE_HEIGHT_MAP = 3;
        public static final int FEATURE_CLEAR_COAT = 4;
        public static final int FEATURE_ANISOTROPY = 5;
        public static final int FEATURE_SHEEN = 6;
        public static final int FEATURE_TRANSMISSION = 7;
        public static final int FEATURE_VERTEX_COLORS = 8;
        public static final int FEATURE_TWO_SIDED = 9;
        
        public MaterialFeatureSet enable(int feature) {
            features.set(feature);
            return this;
        }
        
        public boolean has(int feature) {
            return features.get(feature);
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MaterialFeatureSet that)) return false;
            return features.equals(that.features);
        }
        
        @Override
        public int hashCode() {
            return features.hashCode();
        }
        
        public String toShaderDefines() {
            StringBuilder sb = new StringBuilder();
            if (has(FEATURE_NORMAL_MAP)) sb.append("#define HAS_NORMAL_MAP\n");
            if (has(FEATURE_EMISSIVE)) sb.append("#define HAS_EMISSIVE\n");
            if (has(FEATURE_AO_MAP)) sb.append("#define HAS_AO_MAP\n");
            if (has(FEATURE_HEIGHT_MAP)) sb.append("#define HAS_HEIGHT_MAP\n");
            if (has(FEATURE_CLEAR_COAT)) sb.append("#define HAS_CLEAR_COAT\n");
            if (has(FEATURE_ANISOTROPY)) sb.append("#define HAS_ANISOTROPY\n");
            if (has(FEATURE_SHEEN)) sb.append("#define HAS_SHEEN\n");
            if (has(FEATURE_TRANSMISSION)) sb.append("#define HAS_TRANSMISSION\n");
            if (has(FEATURE_VERTEX_COLORS)) sb.append("#define HAS_VERTEX_COLORS\n");
            if (has(TWO_SIDED)) sb.append("#define TWO_SIDED\n");
            return sb.toString();
        }
    }
    
    /**
     * Material definition with graph and parameters.
     */
    public static final class Material {
        final String name;
        final ShadingModel shadingModel;
        final BlendMode blendMode;
        final MaterialGraph graph;
        final MaterialFeatureSet features;
        final Map<String, MaterialParameter> parameters;
        final AtomicInteger instanceCount;
        final long creationTime;
        
        Material(String name, ShadingModel shadingModel, BlendMode blendMode, MaterialGraph graph) {
            this.name = name;
            this.shadingModel = shadingModel;
            this.blendMode = blendMode;
            this.graph = graph;
            this.features = new MaterialFeatureSet();
            this.parameters = new ConcurrentHashMap<>();
            this.instanceCount = new AtomicInteger(0);
            this.creationTime = System.nanoTime();
        }
        
        public Material setParameter(String name, MaterialParameter param) {
            parameters.put(name, param);
            return this;
        }
        
        public Material enableFeature(int feature) {
            features.enable(feature);
            return this;
        }
    }
    
    /**
     * Material instance with overridden parameters.
     */
    public static final class MaterialInstance {
        final long id;
        final Material parent;
        final Map<String, Object> parameterOverrides;
        final int parameterBufferOffset;
        final AtomicBoolean dirty;
        
        MaterialInstance(long id, Material parent, int bufferOffset) {
            this.id = id;
            this.parent = parent;
            this.parameterOverrides = new ConcurrentHashMap<>();
            this.parameterBufferOffset = bufferOffset;
            this.dirty = new AtomicBoolean(true);
        }
        
        public void setScalar(String name, float value) {
            parameterOverrides.put(name, value);
            dirty.set(true);
        }
        
        public void setVector(String name, float x, float y, float z, float w) {
            parameterOverrides.put(name, new float[]{x, y, z, w});
            dirty.set(true);
        }
        
        public void setTexture(String name, int textureIndex) {
            parameterOverrides.put(name, textureIndex);
            dirty.set(true);
        }
    }
    
    /**
     * Material parameter definition.
     */
    public sealed interface MaterialParameter permits ScalarParameter, VectorParameter, TextureParameter {
        String name();
        Object defaultValue();
        int sizeInBytes();
    }
    
    public record ScalarParameter(String name, float defaultValue) implements MaterialParameter {
        @Override
        public int sizeInBytes() {
            return 4;
        }
    }
    
    public record VectorParameter(String name, float[] defaultValue) implements MaterialParameter {
        @Override
        public int sizeInBytes() {
            return defaultValue.length * 4;
        }
    }
    
    public record TextureParameter(String name, int defaultTextureIndex) implements MaterialParameter {
        @Override
        public Integer defaultValue() {
            return defaultTextureIndex;
        }
        
        @Override
        public int sizeInBytes() {
            return 4;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // MATERIAL GRAPH
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Material graph with nodes and connections.
     */
    public static final class MaterialGraph {
        final List<MaterialNode> nodes;
        final List<NodeConnection> connections;
        final MaterialOutputNode outputNode;
        
        public MaterialGraph() {
            this.nodes = new CopyOnWriteArrayList<>();
            this.connections = new CopyOnWriteArrayList<>();
            this.outputNode = new MaterialOutputNode();
            nodes.add(outputNode);
        }
        
        public MaterialGraph addNode(MaterialNode node) {
            nodes.add(node);
            return this;
        }
        
        public MaterialGraph connect(MaterialNode from, String fromOutput, MaterialNode to, String toInput) {
            connections.add(new NodeConnection(from, fromOutput, to, toInput));
            return this;
        }
        
        public String generateShaderCode() {
            StringBuilder code = new StringBuilder();
            
            // Topological sort for evaluation order
            List<MaterialNode> sorted = topologicalSort();
            
            for (MaterialNode node : sorted) {
                code.append(node.generateCode());
                code.append("\n");
            }
            
            return code.toString();
        }
        
        private List<MaterialNode> topologicalSort() {
            Map<MaterialNode, Integer> inDegree = new HashMap<>();
            Map<MaterialNode, List<MaterialNode>> adjList = new HashMap<>();
            
            for (MaterialNode node : nodes) {
                inDegree.put(node, 0);
                adjList.put(node, new ArrayList<>());
            }
            
            for (NodeConnection conn : connections) {
                adjList.get(conn.from).add(conn.to);
                inDegree.merge(conn.to, 1, Integer::sum);
            }
            
            Queue<MaterialNode> queue = new ArrayDeque<>();
            for (Map.Entry<MaterialNode, Integer> entry : inDegree.entrySet()) {
                if (entry.getValue() == 0) {
                    queue.offer(entry.getKey());
                }
            }
            
            List<MaterialNode> result = new ArrayList<>();
            while (!queue.isEmpty()) {
                MaterialNode node = queue.poll();
                result.add(node);
                
                for (MaterialNode neighbor : adjList.get(node)) {
                    int degree = inDegree.merge(neighbor, -1, Integer::sum);
                    if (degree == 0) {
                        queue.offer(neighbor);
                    }
                }
            }
            
            return result;
        }
    }
    
    /**
     * Base material graph node.
     */
    public abstract static class MaterialNode {
        final String id;
        final String type;
        
        protected MaterialNode(String id, String type) {
            this.id = id;
            this.type = type;
        }
        
        public abstract String generateCode();
    }
    
    /**
     * Material output node (final stage).
     */
    public static final class MaterialOutputNode extends MaterialNode {
        String baseColorInput = "vec4(1.0)";
        String metallicInput = "0.0";
        String roughnessInput = "0.5";
        String normalInput = "vec3(0.0, 0.0, 1.0)";
        String emissiveInput = "vec3(0.0)";
        String opacityInput = "1.0";
        String aoInput = "1.0";
        
        public MaterialOutputNode() {
            super("output", "MaterialOutput");
        }
        
        @Override
        public String generateCode() {
            return String.format("""
                // Material Output
                material.baseColor = %s;
                material.metallic = %s;
                material.roughness = %s;
                material.normal = %s;
                material.emissive = %s;
                material.opacity = %s;
                material.ao = %s;
                """, baseColorInput, metallicInput, roughnessInput, normalInput, 
                emissiveInput, opacityInput, aoInput);
        }
    }
    
    /**
     * Texture sample node.
     */
    public static final class TextureSampleNode extends MaterialNode {
        final int textureIndex;
        final String uvInput;
        
        public TextureSampleNode(String id, int textureIndex, String uvInput) {
            super(id, "TextureSample");
            this.textureIndex = textureIndex;
            this.uvInput = uvInput;
        }
        
        @Override
        public String generateCode() {
            return String.format("vec4 %s = texture(textures[%d], %s);", 
                id, textureIndex, uvInput);
        }
    }
    
    /**
     * Math operation node.
     */
    public static final class MathNode extends MaterialNode {
        final String operation;
        final String inputA;
        final String inputB;
        
        public MathNode(String id, String operation, String inputA, String inputB) {
            super(id, "Math");
            this.operation = operation;
            this.inputA = inputA;
            this.inputB = inputB;
        }
        
        @Override
        public String generateCode() {
            return switch (operation) {
                case "add" -> String.format("vec4 %s = %s + %s;", id, inputA, inputB);
                case "multiply" -> String.format("vec4 %s = %s * %s;", id, inputA, inputB);
                case "lerp" -> String.format("vec4 %s = mix(%s, %s, 0.5);", id, inputA, inputB);
                case "dot" -> String.format("float %s = dot(%s.xyz, %s.xyz);", id, inputA, inputB);
                default -> "";
            };
        }
    }
    
    /**
     * UV coordinate node.
     */
    public static final class UVNode extends MaterialNode {
        final int uvSet;
        
        public UVNode(String id, int uvSet) {
            super(id, "UV");
            this.uvSet = uvSet;
        }
        
        @Override
        public String generateCode() {
            return String.format("vec2 %s = uv%d;", id, uvSet);
        }
    }
    
    /**
     * Node connection.
     */
    private record NodeConnection(MaterialNode from, String fromOutput, MaterialNode to, String toInput) {}
    
    /**
     * Material graph evaluator.
     */
    private static final class MaterialGraphEvaluator {
        
        String evaluate(MaterialGraph graph, MaterialFeatureSet features) {
            StringBuilder shader = new StringBuilder();
            
            // Add header
            shader.append("""
                #version 450
                #extension GL_EXT_nonuniform_qualifier : enable
                
                layout(set = 0, binding = 0) uniform sampler2D textures[];
                
                layout(location = 0) in vec2 uv0;
                layout(location = 1) in vec3 worldNormal;
                layout(location = 2) in vec3 worldPos;
                
                layout(location = 0) out vec4 outColor;
                
                struct Material {
                    vec4 baseColor;
                    float metallic;
                    float roughness;
                    vec3 normal;
                    vec3 emissive;
                    float opacity;
                    float ao;
                };
                
                void main() {
                    Material material;
                """);
            
            // Add feature-specific code
            shader.append(features.toShaderDefines());
            
            // Add graph code
            shader.append(graph.generateShaderCode());
            
            // Add footer
            shader.append("""
                    outColor = material.baseColor;
                }
                """);
            
            return shader.toString();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // TEXTURE REGISTRY
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Bindless texture registry with descriptor indexing.
     */
    private static final class TextureRegistry {
        final VkDevice device;
        final Map<String, Integer> textureIndices;
        final List<Long> textureViews;
        final List<Long> samplers;
        final AtomicInteger nextIndex;
        final ReentrantReadWriteLock lock;
        
        TextureRegistry(VkDevice device) {
            this.device = device;
            this.textureIndices = new ConcurrentHashMap<>();
            this.textureViews = new CopyOnWriteArrayList<>();
            this.samplers = new CopyOnWriteArrayList<>();
            this.nextIndex = new AtomicInteger(0);
            this.lock = new ReentrantReadWriteLock();
        }
        
        int registerTexture(String name, long imageView, long sampler) {
            lock.writeLock().lock();
            try {
                Integer existing = textureIndices.get(name);
                if (existing != null) {
                    return existing;
                }
                
                int index = nextIndex.getAndIncrement();
                textureIndices.put(name, index);
                
                if (index >= textureViews.size()) {
                    textureViews.add(imageView);
                    samplers.add(sampler);
                } else {
                    textureViews.set(index, imageView);
                    samplers.set(index, sampler);
                }
                
                return index;
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        Optional<Integer> getTextureIndex(String name) {
            return Optional.ofNullable(textureIndices.get(name));
        }
        
        void updateDescriptorSet(long descriptorSet) {
            lock.readLock().lock();
            try {
                try (MemoryStack stack = stackPush()) {
                    VkDescriptorImageInfo.Buffer imageInfos = VkDescriptorImageInfo.calloc(textureViews.size(), stack);
                    
                    for (int i = 0; i < textureViews.size(); i++) {
                        imageInfos.get(i)
                            .imageView(textureViews.get(i))
                            .sampler(samplers.get(i))
                            .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                    }
                    
                    VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(1, stack)
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(descriptorSet)
                        .dstBinding(0)
                        .dstArrayElement(0)
                        .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                        .pImageInfo(imageInfos);
                    
                    vkUpdateDescriptorSets(device, write, null);
                }
            } finally {
                lock.readLock().unlock();
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // PARAMETER BUFFER MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    /**
     * GPU parameter buffer manager.
     */
    private static final class ParameterBufferManager {
        final VkDevice device;
        final long buffer;
        final long memory;
        final ByteBuffer mapped;
        final int capacity;
        final BitSet allocatedSlots;
        final ReentrantLock allocationLock;
        
        ParameterBufferManager(VkDevice device, int maxInstances) {
            this.device = device;
            this.capacity = maxInstances;
            this.allocatedSlots = new BitSet(maxInstances);
            this.allocationLock = new ReentrantLock();
            
            long bufferSize = (long) maxInstances * PARAMETER_BLOCK_SIZE;
            
            try (MemoryStack stack = stackPush()) {
                VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(bufferSize)
                    .usage(VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
                
                LongBuffer pBuffer = stack.mallocLong(1);
                if (vkCreateBuffer(device, bufferInfo, null, pBuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create parameter buffer");
                }
                this.buffer = pBuffer.get(0);
                
                VkMemoryRequirements memReqs = VkMemoryRequirements.malloc(stack);
                vkGetBufferMemoryRequirements(device, buffer, memReqs);
                
                VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memReqs.size())
                    .memoryTypeIndex(0); // Simplified
                
                LongBuffer pMemory = stack.mallocLong(1);
                if (vkAllocateMemory(device, allocInfo, null, pMemory) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate parameter memory");
                }
                this.memory = pMemory.get(0);
                
                vkBindBufferMemory(device, buffer, memory, 0);
                
                PointerBuffer ppData = stack.mallocPointer(1);
                vkMapMemory(device, memory, 0, bufferSize, 0, ppData);
                this.mapped = ppData.getByteBuffer(0, (int) bufferSize);
            }
        }
        
        int allocateSlot() {
            allocationLock.lock();
            try {
                int slot = allocatedSlots.nextClearBit(0);
                if (slot >= capacity) {
                    throw new RuntimeException("Parameter buffer full");
                }
                allocatedSlots.set(slot);
                return slot;
            } finally {
                allocationLock.unlock();
            }
        }
        
        void freeSlot(int slot) {
            allocationLock.lock();
            try {
                allocatedSlots.clear(slot);
            } finally {
                allocationLock.unlock();
            }
        }
        
        void updateInstance(MaterialInstance instance) {
            if (!instance.dirty.compareAndSet(true, false)) {
                return;
            }
            
            int offset = instance.parameterBufferOffset * PARAMETER_BLOCK_SIZE;
            ByteBuffer slice = mapped.duplicate();
            slice.position(offset);
            slice.limit(offset + PARAMETER_BLOCK_SIZE);
            
            // Write base material parameters
            for (Map.Entry<String, MaterialParameter> entry : instance.parent.parameters.entrySet()) {
                String name = entry.getKey();
                MaterialParameter param = entry.getValue();
                
                Object value = instance.parameterOverrides.getOrDefault(name, param.defaultValue());
                
                switch (param) {
                    case ScalarParameter sp -> slice.putFloat((Float) value);
                    case VectorParameter vp -> {
                        float[] vec = (float[]) value;
                        for (float v : vec) {
                            slice.putFloat(v);
                        }
                    }
                    case TextureParameter tp -> slice.putInt((Integer) value);
                }
            }
        }
        
        void destroy() {
            vkUnmapMemory(device, memory);
            vkDestroyBuffer(device, buffer, null);
            vkFreeMemory(device, memory, null);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // SHADER PERMUTATION CACHE
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Shader permutation cache for material variants.
     */
    private static final class ShaderPermutationCache {
        final Map<MaterialFeatureSet, ShaderVariant> variants;
        final ReentrantReadWriteLock lock;
        
        ShaderPermutationCache() {
            this.variants = new ConcurrentHashMap<>();
            this.lock = new ReentrantReadWriteLock();
        }
        
        Optional<ShaderVariant> get(MaterialFeatureSet features) {
            lock.readLock().lock();
            try {
                return Optional.ofNullable(variants.get(features));
            } finally {
                lock.readLock().unlock();
            }
        }
        
        void put(MaterialFeatureSet features, ShaderVariant variant) {
            lock.writeLock().lock();
            try {
                variants.put(features, variant);
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        record ShaderVariant(long vertexShader, long fragmentShader, String code) {}
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Material system statistics.
     */
    public static final class MaterialStatistics {
        private final AtomicInteger totalMaterials = new AtomicInteger(0);
        private final AtomicInteger totalInstances = new AtomicInteger(0);
        private final AtomicInteger totalTextures = new AtomicInteger(0);
        private final AtomicInteger shaderPermutations = new AtomicInteger(0);
        private final AtomicLong parameterUpdates = new AtomicLong(0);
        
        void recordMaterialCreation() {
            totalMaterials.incrementAndGet();
        }
        
        void recordInstanceCreation() {
            totalInstances.incrementAndGet();
        }
        
        void recordTextureRegistration() {
            totalTextures.incrementAndGet();
        }
        
        void recordShaderPermutation() {
            shaderPermutations.incrementAndGet();
        }
        
        void recordParameterUpdate() {
            parameterUpdates.incrementAndGet();
        }
        
        public int getTotalMaterials() {
            return totalMaterials.get();
        }
        
        public int getTotalInstances() {
            return totalInstances.get();
        }
        
        public int getTotalTextures() {
            return totalTextures.get();
        }
        
        public int getShaderPermutations() {
            return shaderPermutations.get();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Constructor with RenderCore integration.
     * For universal API support - device handles are obtained from RenderCore when using Vulkan.
     */
    public MaterialSystem(Object renderCore, Arena arena, Object bindlessTextures) {
        // Extract device handles from RenderCore if using Vulkan API
        // For non-Vulkan APIs, these will be null and API-specific paths will be used
        this.device = null;  // Will be set based on RenderCore's API
        this.physicalDevice = null;  // Will be set based on RenderCore's API
        
        this.materials = new ConcurrentHashMap<>();
        this.instances = new ConcurrentHashMap<>();
        this.nextInstanceId = new AtomicLong(1);
        
        this.textureRegistry = new TextureRegistry(device);
        
        // Create descriptor set layout for textures (API-agnostic through RenderCore)
        this.textureDescriptorSetLayout = createTextureDescriptorSetLayout();
        this.textureDescriptorPool = createTextureDescriptorPool();
        this.textureDescriptorSet = allocateTextureDescriptorSet();
        
        this.parameterBuffers = new ParameterBufferManager(device, MAX_MATERIAL_INSTANCES);
        
        this.permutationCache = new ShaderPermutationCache();
        this.pipelineCache = new ConcurrentHashMap<>();
        
        this.graphEvaluator = new MaterialGraphEvaluator();
        
        this.statistics = new MaterialStatistics();
        
        this.materialLock = new ReentrantReadWriteLock();
        this.textureLock = new ReentrantReadWriteLock();
    }
    
    /**
     * Direct Vulkan constructor for backward compatibility.
     */
    public MaterialSystem(VkDevice device, VkPhysicalDevice physicalDevice) {
        this.device = device;
        this.physicalDevice = physicalDevice;
        
        this.materials = new ConcurrentHashMap<>();
        this.instances = new ConcurrentHashMap<>();
        this.nextInstanceId = new AtomicLong(1);
        
        this.textureRegistry = new TextureRegistry(device);
        
        // Create descriptor set layout for textures
        this.textureDescriptorSetLayout = createTextureDescriptorSetLayout();
        this.textureDescriptorPool = createTextureDescriptorPool();
        this.textureDescriptorSet = allocateTextureDescriptorSet();
        
        this.parameterBuffers = new ParameterBufferManager(device, MAX_MATERIAL_INSTANCES);
        
        this.permutationCache = new ShaderPermutationCache();
        this.pipelineCache = new ConcurrentHashMap<>();
        
        this.graphEvaluator = new MaterialGraphEvaluator();
        
        this.statistics = new MaterialStatistics();
        
        this.materialLock = new ReentrantReadWriteLock();
        this.textureLock = new ReentrantReadWriteLock();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // MATERIAL CREATION
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    public Material createMaterial(String name, ShadingModel shadingModel, BlendMode blendMode) {
        materialLock.writeLock().lock();
        try {
            MaterialGraph graph = new MaterialGraph();
            Material material = new Material(name, shadingModel, blendMode, graph);
            materials.put(name, material);
            statistics.recordMaterialCreation();
            return material;
        } finally {
            materialLock.writeLock().unlock();
        }
    }
    
    public Material getMaterial(String name) {
        materialLock.readLock().lock();
        try {
            return materials.get(name);
        } finally {
            materialLock.readLock().unlock();
        }
    }
    
    public long createMaterialInstance(String materialName) {
        Material material = getMaterial(materialName);
        if (material == null) {
            throw new IllegalArgumentException("Material not found: " + materialName);
        }
        
        long id = nextInstanceId.getAndIncrement();
        int bufferOffset = parameterBuffers.allocateSlot();
        
        MaterialInstance instance = new MaterialInstance(id, material, bufferOffset);
        instances.put(id, instance);
        
        material.instanceCount.incrementAndGet();
        statistics.recordInstanceCreation();
        
        return id;
    }
    
    public MaterialInstance getInstance(long id) {
        return instances.get(id);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // TEXTURE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    public int registerTexture(String name, long imageView, long sampler) {
        textureLock.writeLock().lock();
        try {
            int index = textureRegistry.registerTexture(name, imageView, sampler);
            textureRegistry.updateDescriptorSet(textureDescriptorSet);
            statistics.recordTextureRegistration();
            return index;
        } finally {
            textureLock.writeLock().unlock();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // UPDATES
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    public void update() {
        // Update dirty material instances
        for (MaterialInstance instance : instances.values()) {
            if (instance.dirty.get()) {
                parameterBuffers.updateInstance(instance);
                statistics.recordParameterUpdate();
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // SHADER COMPILATION
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    public String compileShader(Material material) {
        return graphEvaluator.evaluate(material.graph, material.features);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    // VULKAN RESOURCES
    // ═══════════════════════════════════════════════════════════════════════════════════════════════
    
    private long createTextureDescriptorSetLayout() {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer binding = VkDescriptorSetLayoutBinding.calloc(1, stack)
                .binding(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .descriptorCount(MAX_TEXTURES)
                .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
            
            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                .pBindings(binding);
            
            LongBuffer pLayout = stack.mallocLong(1);
            if (vkCreateDescriptorSetLayout(device, layoutInfo, null, pLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor set layout");
            }
            return pLayout.get(0);
        }
    }
    
    private long createTextureDescriptorPool() {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorPoolSize.Buffer poolSize = VkDescriptorPoolSize.calloc(1, stack)
                .type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .descriptorCount(MAX_TEXTURES);
            
            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                .pPoolSizes(poolSize)
                .maxSets(1);
            
            LongBuffer pPool = stack.mallocLong(1);
            if (vkCreateDescriptorPool(device, poolInfo, null, pPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool");
            }
            return pPool.get(0);
        }
    }
    
    private long allocateTextureDescriptorSet() {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                .descriptorPool(textureDescriptorPool)
                .pSetLayouts(stack.longs(textureDescriptorSetLayout));
            
            LongBuffer pSet = stack.mallocLong(1);
            if (vkAllocateDescriptorSets(device, allocInfo, pSet) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate descriptor set");
            }
            return pSet.get(0);
        }
    }
    
    public MaterialStatistics getStatistics() {
        return statistics;
    }
    
    @Override
    public void close() {
        parameterBuffers.destroy();
        
        vkDestroyDescriptorPool(device, textureDescriptorPool, null);
        vkDestroyDescriptorSetLayout(device, textureDescriptorSetLayout, null);
    }
}
