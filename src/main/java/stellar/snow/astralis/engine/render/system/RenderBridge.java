package stellar.snow.astralis.engine.render.system;
import stellar.snow.astralis.Astralis;
import static org.lwjgl.opengl.GL.*;
import stellar.snow.astralis.engine.render.pipeline.*;
import stellar.snow.astralis.api.opengl.buffer.*;
import stellar.snow.astralis.nexus.vulkan.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.LongBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import static org.lwjgl.vulkan.KHRPushDescriptor.*;
/**
 * RenderBridge - The singleton orchestrator for the Vulkan rendering backend.
 * 
 * <p>This class manages:
 * <ul>
 *   <li>Vulkan context initialization and lifecycle</li>
 *   <li>Buffer operations backend selection</li>
 *   <li>Render state management</li>
 *   <li>Pipeline creation and caching</li>
 *   <li>Frame synchronization</li>
 *   <li>Command buffer management</li>
 * </ul>
 * </p>
 */
    
    private static final RenderBridge INSTANCE = new RenderBridge();
    
    // Core Subsystems
    private VulkanContext context;
    private BufferOps bufferOps;
    private RenderState renderState;
    private VulkanPipelineProvider pipelineProvider;
    private MatrixStack matrixStack;
    
    // Vulkan Resources
    private long pipelineLayout;
    private long[] descriptorSetLayouts;
    private long descriptorPool;
    private long commandPool;
    private VkCommandBuffer[] commandBuffers;
    private long[] imageAvailableSemaphores;
    private long[] renderFinishedSemaphores;
    private long[] inFlightFences;
    
    // Frame Management
    private static final int MAX_FRAMES_IN_FLIGHT = 2;
    private final AtomicInteger currentFrame = new AtomicInteger(0);
    private final ReentrantLock frameLock = new ReentrantLock();
    
    // State
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean vulkanActive = new AtomicBoolean(false);
    private volatile boolean frameInProgress = false;
    
    // Feature Flags
    private boolean supportsPushDescriptors = false;
    private boolean supportsDynamicRendering = false;
    private boolean supportsBindless = false;
    public static RenderBridge getInstance() { 
        return INSTANCE; 
    }
    /**
     * Initializes the render bridge with the given window handle.
     * 
     * @param windowHandle GLFW window handle
     */
    public void initialize(long windowHandle) {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        
        Astralis.LOGGER.info("[RenderBridge] Initializing...");
        
        try {
            // 1. Initialize Vulkan Context
            context = new VulkanContext();
            if (!context.initialize(windowHandle)) {
                Astralis.LOGGER.error("[RenderBridge] Vulkan initialization failed, falling back to OpenGL");
                fallbackToOpenGL();
                return;
            }
            
            // 2. Detect features
            detectFeatures();
            
            // 3. Create Vulkan resources
            createDescriptorSetLayouts();
            createPipelineLayout();
            createDescriptorPool();
            createCommandPool();
            createCommandBuffers();
            createSyncObjects();
            
            // 4. Select best BufferOps implementation
            selectBufferOps();
            
            // 5. Initialize state tracking
            renderState = new RenderState();
            matrixStack = new MatrixStack();
            
            // 6. Initialize Pipeline Provider
            pipelineProvider = new VulkanPipelineProvider(context, pipelineLayout, descriptorSetLayouts);
            
            vulkanActive.set(true);
            Astralis.LOGGER.info("[RenderBridge] Initialization complete. Backend: Vulkan {}", 
                formatVulkanVersion(context.vulkanVersion));
            
        } catch (Exception e) {
            Astralis.LOGGER.error("[RenderBridge] Initialization failed", e);
            cleanup();
            fallbackToOpenGL();
        }
    }
    
    private void detectFeatures() {
        int version = context.vulkanVersion;
        
        // Check for push descriptors
        supportsPushDescriptors = context.hasExtension(VK_KHR_PUSH_DESCRIPTOR_EXTENSION_NAME);
        
        // Check for dynamic rendering (Vulkan 1.3+ or extension)
        supportsDynamicRendering = version >= VK_API_VERSION_1_3 || 
                                   context.hasExtension("VK_KHR_dynamic_rendering");
        
        // Check for bindless/descriptor indexing (Vulkan 1.2+ or extension)
        supportsBindless = version >= VK_API_VERSION_1_2 || 
                          context.hasExtension("VK_EXT_descriptor_indexing");
        
        Astralis.LOGGER.info("[RenderBridge] Features: PushDescriptors={}, DynamicRendering={}, Bindless={}",
            supportsPushDescriptors, supportsDynamicRendering, supportsBindless);
    }
    
    private void selectBufferOps() {
        int version = context.vulkanVersion;
        
        // Select based on version and features
        if (version >= VK_API_VERSION_1_3 && supportsPushDescriptors && supportsDynamicRendering) {
            if (version >= 0x00401004) { // 1.4.x
                Astralis.LOGGER.info("[RenderBridge] Selected: VulkanBufferOps14 (Streaming/PushDescriptors)");
                bufferOps = new VulkanBufferOps14();
            } else {
                Astralis.LOGGER.info("[RenderBridge] Selected: VulkanBufferOps13 (Dynamic Rendering)");
                bufferOps = new VulkanBufferOps13();
            }
        } else if (version >= VK_API_VERSION_1_2 && supportsBindless) {
            Astralis.LOGGER.info("[RenderBridge] Selected: VulkanBufferOps12 (Bindless)");
            bufferOps = new VulkanBufferOps12();
        } else if (version >= VK_API_VERSION_1_1) {
            Astralis.LOGGER.info("[RenderBridge] Selected: VulkanBufferOps11");
            bufferOps = new VulkanBufferOps11();
        } else {
            Astralis.LOGGER.info("[RenderBridge] Selected: VulkanBufferOps10 (Base)");
            bufferOps = new VulkanBufferOps10();
        }
        
        // Initialize the selected ops
        if (bufferOps instanceof VulkanBufferOps10 vkOps) {
            vkOps.initialize(Astralis.DEBUG_MODE);
        }
    }
    
    private void createDescriptorSetLayouts() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Layout 0: Uniforms (Push Constants alternative for older Vulkan)
            // Layout 1: Textures (Sampler array for bindless or individual samplers)
            
            descriptorSetLayouts = new long[2];
            
            // Uniform buffer layout
            VkDescriptorSetLayoutBinding.Buffer uniformBinding = VkDescriptorSetLayoutBinding.calloc(1, stack)
                .binding(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC)
                .descriptorCount(1)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT);
            
            VkDescriptorSetLayoutCreateInfo uniformLayoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                .pBindings(uniformBinding);
            
            LongBuffer pLayout = stack.mallocLong(1);
            vkCreateDescriptorSetLayout(context.device, uniformLayoutInfo, null, pLayout);
            descriptorSetLayouts[0] = pLayout.get(0);
            
            // Texture sampler layout
            int textureCount = supportsBindless ? 1024 : 16; // Bindless vs fixed
            VkDescriptorSetLayoutBinding.Buffer textureBinding = VkDescriptorSetLayoutBinding.calloc(1, stack)
                .binding(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .descriptorCount(textureCount)
                .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
            
            VkDescriptorSetLayoutCreateInfo textureLayoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                .pBindings(textureBinding);
            
            if (supportsBindless) {
                // Add bindless flags via pNext chain (simplified)
            }
            
            vkCreateDescriptorSetLayout(context.device, textureLayoutInfo, null, pLayout);
            descriptorSetLayouts[1] = pLayout.get(0);
        }
    }
    
    private void createPipelineLayout() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Push constant range for matrices and basic uniforms
            VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.calloc(1, stack)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT)
                .offset(0)
                .size(256); // 4 matrices + misc uniforms
            
            VkPipelineLayoutCreateInfo layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                .pSetLayouts(stack.longs(descriptorSetLayouts))
                .pPushConstantRanges(pushConstantRange);
            
            LongBuffer pLayout = stack.mallocLong(1);
            if (vkCreatePipelineLayout(context.device, layoutInfo, null, pLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }
            pipelineLayout = pLayout.get(0);
        }
    }
    
    private void createDescriptorPool() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int maxSets = 1000;
            
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(2, stack);
            poolSizes.get(0)
                .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC)
                .descriptorCount(maxSets);
            poolSizes.get(1)
                .type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .descriptorCount(maxSets * 16);
            
            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                .pPoolSizes(poolSizes)
                .maxSets(maxSets)
                .flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT);
            
            LongBuffer pPool = stack.mallocLong(1);
            vkCreateDescriptorPool(context.device, poolInfo, null, pPool);
            descriptorPool = pPool.get(0);
        }
    }
    
    private void createCommandPool() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                .queueFamilyIndex(context.graphicsQueueFamily);
            
            LongBuffer pPool = stack.mallocLong(1);
            vkCreateCommandPool(context.device, poolInfo, null, pPool);
            commandPool = pPool.get(0);
        }
    }
    
    private void createCommandBuffers() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(commandPool)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(MAX_FRAMES_IN_FLIGHT);
            
            PointerBuffer pCommandBuffers = stack.mallocPointer(MAX_FRAMES_IN_FLIGHT);
            vkAllocateCommandBuffers(context.device, allocInfo, pCommandBuffers);
            
            commandBuffers = new VkCommandBuffer[MAX_FRAMES_IN_FLIGHT];
            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
                commandBuffers[i] = new VkCommandBuffer(pCommandBuffers.get(i), context.device);
            }
        }
    }
    
    private void createSyncObjects() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            
            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                .flags(VK_FENCE_CREATE_SIGNALED_BIT);
            
            imageAvailableSemaphores = new long[MAX_FRAMES_IN_FLIGHT];
            renderFinishedSemaphores = new long[MAX_FRAMES_IN_FLIGHT];
            inFlightFences = new long[MAX_FRAMES_IN_FLIGHT];
            
            LongBuffer pSemaphore = stack.mallocLong(1);
            LongBuffer pFence = stack.mallocLong(1);
            
            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
                vkCreateSemaphore(context.device, semaphoreInfo, null, pSemaphore);
                imageAvailableSemaphores[i] = pSemaphore.get(0);
                
                vkCreateSemaphore(context.device, semaphoreInfo, null, pSemaphore);
                renderFinishedSemaphores[i] = pSemaphore.get(0);
                
                vkCreateFence(context.device, fenceInfo, null, pFence);
                inFlightFences[i] = pFence.get(0);
            }
        }
    }
    private void fallbackToOpenGL() {
        Astralis.LOGGER.info("[RenderBridge] Selecting OpenGL fallback...");
        
        // Detect OpenGL version and select appropriate ops
        int glVersion = detectOpenGLVersion();
        
        if (glVersion >= 460) {
            Astralis.LOGGER.info("[RenderBridge] Selected: GLBufferOps46 (DSA + Persistent Mapping)");
            bufferOps = new GLBufferOps46();
        } else if (glVersion >= 450) {
            Astralis.LOGGER.info("[RenderBridge] Selected: GLBufferOps45 (DSA)");
            bufferOps = new GLBufferOps45();
        } else if (glVersion >= 440) {
            Astralis.LOGGER.info("[RenderBridge] Selected: GLBufferOps44");
            bufferOps = new GLBufferOps44();
        } else if (glVersion >= 430) {
            Astralis.LOGGER.info("[RenderBridge] Selected: GLBufferOps43");
            bufferOps = new GLBufferOps43();
        } else if (glVersion >= 330) {
            Astralis.LOGGER.info("[RenderBridge] Selected: GLBufferOps33 (Core Profile Baseline)");
            bufferOps = new GLBufferOps33();
        } else if (glVersion >= 210) {
            Astralis.LOGGER.info("[RenderBridge] Selected: GLBufferOps21");
            bufferOps = new GLBufferOps21();
        } else {
            Astralis.LOGGER.info("[RenderBridge] Selected: GLBufferOps15 (Legacy)");
            bufferOps = new GLBufferOps15();
        }
        
        // Initialize state tracking (still useful for GL)
        renderState = new RenderState();
        matrixStack = new MatrixStack();
        
        vulkanActive.set(false);
    }
    
    private int detectOpenGLVersion() {
        try {
            String version = org.lwjgl.opengl.GL.glGetString(org.lwjgl.opengl.GL.GL_VERSION);
            if (version != null && version.length() >= 3) {
                int major = version.charAt(0) - '0';
                int minor = version.charAt(2) - '0';
                return major * 100 + minor * 10;
            }
        } catch (Exception e) {
            Astralis.LOGGER.warn("[RenderBridge] Failed to detect OpenGL version: {}", e.getMessage());
        }
        return 210; // Assume minimum
    }
    
    private String formatVulkanVersion(int version) {
        int major = VK_API_VERSION_MAJOR(version);
        int minor = VK_API_VERSION_MINOR(version);
        int patch = VK_API_VERSION_PATCH(version);
        return String.format("%d.%d.%d", major, minor, patch);
    }
    // ========================================================================
    // FRAME MANAGEMENT
    // ========================================================================
    
    /**
     * Begins a new frame. Must be called before any rendering.
     * 
     * @return true if frame started successfully, false if should skip
     */
    public boolean beginFrame() {
        if (!vulkanActive.get()) {
            return true; // GL doesn't need frame management
        }
        
        frameLock.lock();
        try {
            int frame = currentFrame.get();
            
            // Wait for previous frame to complete
            vkWaitForFences(context.device, inFlightFences[frame], true, Long.MAX_VALUE);
            
            // Acquire swapchain image
            try (MemoryStack stack = MemoryStack.stackPush()) {
                int[] imageIndex = new int[1];
                int result = context.acquireNextImage(imageAvailableSemaphores[frame], imageIndex);
                
                if (result == VK_ERROR_OUT_OF_DATE_KHR) {
                    recreateSwapchain();
                    return false;
                } else if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
                    throw new RuntimeException("Failed to acquire swapchain image: " + result);
                }
            }
            
            // Reset fence
            vkResetFences(context.device, inFlightFences[frame]);
            
            // Reset and begin command buffer
            VkCommandBuffer cmd = commandBuffers[frame];
            vkResetCommandBuffer(cmd, 0);
            
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            
            vkBeginCommandBuffer(cmd, beginInfo);
            beginInfo.free();
            
            frameInProgress = true;
            return true;
            
        } finally {
            frameLock.unlock();
        }
    }
    
    /**
     * Ends the current frame and presents.
     */
    public void endFrame() {
        if (!vulkanActive.get() || !frameInProgress) {
            return;
        }
        
        frameLock.lock();
        try {
            int frame = currentFrame.get();
            VkCommandBuffer cmd = commandBuffers[frame];
            
            // End command buffer
            vkEndCommandBuffer(cmd);
            
            // Submit
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pWaitSemaphores(stack.longs(imageAvailableSemaphores[frame]))
                    .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                    .pCommandBuffers(stack.pointers(cmd))
                    .pSignalSemaphores(stack.longs(renderFinishedSemaphores[frame]));
                
                vkQueueSubmit(context.graphicsQueue, submitInfo, inFlightFences[frame]);
            }
            
            // Present
            int result = context.present(renderFinishedSemaphores[frame]);
            if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR) {
                recreateSwapchain();
            }
            
            // Advance frame
            currentFrame.set((frame + 1) % MAX_FRAMES_IN_FLIGHT);
            frameInProgress = false;
            
        } finally {
            frameLock.unlock();
        }
    }
    
    private void recreateSwapchain() {
        vkDeviceWaitIdle(context.device);
        context.recreateSwapchain();
        // May need to recreate framebuffers, etc.
    }
    // ========================================================================
    // PUBLIC API
    // ========================================================================
    
    public BufferOps getBufferOps() {
        return bufferOps;
    }
    
    public RenderState getRenderState() {
        return renderState;
    }
    
    public MatrixStack getMatrixStack() {
        return matrixStack;
    }
    
    public VulkanPipelineProvider getPipelineProvider() {
        return pipelineProvider;
    }
    
    public VulkanContext getContext() {
        return context;
    }
    
    public VkCommandBuffer getCurrentCommandBuffer() {
        return vulkanActive.get() ? commandBuffers[currentFrame.get()] : null;
    }
    
    public long getPipelineLayout() {
        return pipelineLayout;
    }
    
    public long getDescriptorPool() {
        return descriptorPool;
    }
    
    public boolean isVulkan() {
        return vulkanActive.get();
    }
    
    public boolean isInitialized() {
        return initialized.get();
    }
    
    public boolean supportsPushDescriptors() {
        return supportsPushDescriptors;
    }
    
    public boolean supportsDynamicRendering() {
        return supportsDynamicRendering;
    }
    
    public boolean supportsBindless() {
        return supportsBindless;
    }
    // ========================================================================
    // CLEANUP
    // ========================================================================
    
    public void cleanup() {
        if (context != null && context.device != null) {
            vkDeviceWaitIdle(context.device);
        }
        
        if (pipelineProvider != null) {
            pipelineProvider.cleanup();
        }
        
        if (renderState != null) {
            renderState.close();
        }
        
        // Destroy sync objects
        if (imageAvailableSemaphores != null) {
            for (long sem : imageAvailableSemaphores) {
                if (sem != VK_NULL_HANDLE) vkDestroySemaphore(context.device, sem, null);
            }
        }
        if (renderFinishedSemaphores != null) {
            for (long sem : renderFinishedSemaphores) {
                if (sem != VK_NULL_HANDLE) vkDestroySemaphore(context.device, sem, null);
            }
        }
        if (inFlightFences != null) {
            for (long fence : inFlightFences) {
                if (fence != VK_NULL_HANDLE) vkDestroyFence(context.device, fence, null);
            }
        }
        
        // Destroy command pool (frees command buffers)
        if (commandPool != VK_NULL_HANDLE) {
            vkDestroyCommandPool(context.device, commandPool, null);
        }
        
        // Destroy descriptor pool
        if (descriptorPool != VK_NULL_HANDLE) {
            vkDestroyDescriptorPool(context.device, descriptorPool, null);
        }
        
        // Destroy layouts
        if (pipelineLayout != VK_NULL_HANDLE) {
            vkDestroyPipelineLayout(context.device, pipelineLayout, null);
        }
        if (descriptorSetLayouts != null) {
            for (long layout : descriptorSetLayouts) {
                if (layout != VK_NULL_HANDLE) vkDestroyDescriptorSetLayout(context.device, layout, null);
            }
        }
        
        // Cleanup context
        if (context != null) {
            context.cleanup();
        }
        
        Astralis.LOGGER.info("[RenderBridge] Cleanup complete");
    }
}
