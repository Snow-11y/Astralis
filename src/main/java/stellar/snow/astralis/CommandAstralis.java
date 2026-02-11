package stellar.snow.astralis.commands;

import stellar.snow.astralis.core.InitializationManager;
import stellar.snow.astralis.config.Config;
import stellar.snow.astralis.engine.gpu.compute.CullingManager;
import stellar.snow.astralis.engine.gpu.compute.CullingTier;
import stellar.snow.astralis.api.opengl.managers.GLStateCache;
import stellar.snow.astralis.api.vulkan.backend.VulkanBackend;
import stellar.snow.astralis.api.vulkan.managers.VulkanManager;
import stellar.snow.astralis.api.directx.managers.DirectXManager;
import stellar.snow.astralis.api.opengles.managers.OpenGLESManager;
import stellar.snow.astralis.engine.gpu.authority.UniversalCapabilities;
import stellar.snow.astralis.integration.DeepMix.DeepMix;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import java.util.EnumMap;
import java.util.Map;

/**
 * Astralis debug and info command.
 * 
 * <h2>Available Subcommands:</h2>
 * <ul>
 *   <li><b>status</b> - Show entity culling distribution</li>
 *   <li><b>backend</b> - Show active GPU backend information</li>
 *   <li><b>vulkan</b> - Show Vulkan details (if active)</li>
 *   <li><b>directx</b> - Show DirectX details (if active)</li>
 *   <li><b>metal</b> - Show Metal details (if active)</li>
 *   <li><b>opengl</b> - Show OpenGL details</li>
 *   <li><b>gles</b> - Show OpenGL ES details (if active)</li>
 *   <li><b>glsl</b> - Show GLSL pipeline details</li>
 *   <li><b>msl</b> - Show MSL pipeline details</li>
 *   <li><b>config</b> - Show current configuration</li>
 *   <li><b>capabilities</b> - Show detected GPU capabilities</li>
 *   <li><b>cache</b> - Show GL state cache statistics</li>
 * </ul>
 */
public class CommandAstralis extends CommandBase {
    
    @Override
    public String getName() {
        return "astralis";
    }
    
    @Override
    public String getUsage(ICommandSender sender) {
        return "/astralis <status|backend|vulkan|directx|metal|opengl|gles|glsl|msl|config|capabilities|cache|deepmix|mdr>";
    }
    
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            args = new String[]{"status"};
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "status":
                showEntityStatus(sender);
                break;
                
            case "backend":
            case "gpu":
                showBackendInfo(sender);
                break;
                
            case "vulkan":
            case "vk":
                showVulkanInfo(sender);
                break;
                
            case "directx":
            case "dx":
                showDirectXInfo(sender);
                break;
                
            case "metal":
            case "mtl":
                showMetalInfo(sender);
                break;
                
            case "opengl":
            case "gl":
                showOpenGLInfo(sender);
                break;
                
            case "glsl":
                showGLSLInfo(sender);
                break;
                
            case "msl":
                showMSLInfo(sender);
                break;
                
            case "gles":
            case "opengles":
            case "es":
                showGLESInfo(sender);
                break;
                
            case "config":
            case "cfg":
                showConfigInfo(sender);
                break;
                
            case "capabilities":
            case "caps":
                showCapabilities(sender);
                break;
                
            case "cache":
                showCacheStats(sender);
                break;

            case "deepmix":
            case "dm":
                showDeepMixInfo(sender);
                break;

            case "mdr":
            case "minidirtyroom":
                showMDRInfo(sender);
                break;

            default:
                sender.sendMessage(new TextComponentString(
                    "§cUnknown subcommand. Use: status, backend, vulkan, directx, metal, opengl, gles, glsl, msl, config, capabilities, cache, deepmix, mdr"));
        }
    }
    
    /**
     * Show entity culling tier distribution
     */
    private void showEntityStatus(ICommandSender sender) {
        Map<CullingTier, Integer> counts = new EnumMap<>(CullingTier.class);
        for (CullingTier tier : CullingTier.values()) {
            counts.put(tier, 0);
        }
        
        // Count entities per tier
        for (Entity entity : sender.getEntityWorld().loadedEntityList) {
            if (entity instanceof net.minecraft.entity.EntityLiving) {
                CullingTier tier = CullingManager.getInstance()
                    .calculateTier(entity, sender.getEntityWorld());
                counts.put(tier, counts.get(tier) + 1);
            }
        }
        
        sender.sendMessage(new TextComponentString("§6=== Astralis Entity Distribution ==="));
        sender.sendMessage(new TextComponentString(
            String.format("§aFULL: %d  §eMINIMAL: %d  §6MODERATE: %d  §cAGGRESSIVE: %d",
                counts.get(CullingTier.FULL),
                counts.get(CullingTier.MINIMAL),
                counts.get(CullingTier.MODERATE),
                counts.get(CullingTier.AGGRESSIVE))
        ));
        
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        sender.sendMessage(new TextComponentString(String.format("§7Total entities tracked: %d", total)));
    }
    
    /**
     * Show active GPU backend information
     */
    private void showBackendInfo(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("§6=== Astralis GPU Backend ==="));
        
        if (!InitializationManager.isInitialized()) {
            sender.sendMessage(new TextComponentString("§cAstralis not fully initialized"));
            return;
        }
        
        boolean vulkanActive = InitializationManager.isVulkanActive();
        boolean directxActive = InitializationManager.isDirectXActive();
        boolean glesActive = InitializationManager.isOpenGLESActive();
        
        String backend = "OpenGL";
        String color = "§e";
        if (vulkanActive) {
            backend = "Vulkan";
            color = "§a";
        } else if (directxActive) {
            backend = "DirectX";
            color = "§b";
        } else if (glesActive) {
            backend = "OpenGL ES";
            color = "§d";
        }
        
        sender.sendMessage(new TextComponentString(color + "Active Backend: §f" + backend));
        
        if (vulkanActive) {
            VulkanBackend vk = InitializationManager.getVulkanBackend();
            sender.sendMessage(new TextComponentString("§7Device: §f" + vk.getDeviceName()));
            sender.sendMessage(new TextComponentString("§7API Version: §f" + vk.getApiVersion()));
            sender.sendMessage(new TextComponentString("§7Driver Version: §f" + vk.getDriverVersion()));
            sender.sendMessage(new TextComponentString("§7Use '/astralis vulkan' for more details"));
        } else if (directxActive) {
            DirectXManager dx = InitializationManager.getDirectXManager();
            sender.sendMessage(new TextComponentString("§7API Version: §f" + dx.getCurrentAPI().displayName));
            sender.sendMessage(new TextComponentString("§7Use '/astralis directx' for more details"));
        } else if (glesActive) {
            sender.sendMessage(new TextComponentString("§7GLES Version: §f" + 
                UniversalCapabilities.GLES.majorVersion + "." + UniversalCapabilities.GLES.minorVersion));
            sender.sendMessage(new TextComponentString("§7GLSL ES Version: §f" + 
                UniversalCapabilities.GLSL.esMajorVersion + "." + UniversalCapabilities.GLSL.esMinorVersion));
            sender.sendMessage(new TextComponentString("§7Emulated: §f" + 
                (UniversalCapabilities.GLES.isEmulatedGLES ? "Yes" : "No")));
            sender.sendMessage(new TextComponentString("§7Use '/astralis gles' for more details"));
        } else {
            sender.sendMessage(new TextComponentString("§7OpenGL Version: §f" + 
                UniversalCapabilities.getOpenGLVersion()));
            sender.sendMessage(new TextComponentString("§7GLSL Version: §f" + 
                UniversalCapabilities.getGLSLVersion()));
        }
    }
    
    /**
     * Show detailed Vulkan information
     */
    private void showVulkanInfo(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("§6=== Vulkan Details ==="));
        
        if (!InitializationManager.isVulkanActive()) {
            sender.sendMessage(new TextComponentString("§eVulkan backend not active"));
            sender.sendMessage(new TextComponentString("§7Current backend: OpenGL"));
            
            if (UniversalCapabilities.isVulkanSupported()) {
                sender.sendMessage(new TextComponentString("§7Vulkan is available but not enabled"));
                sender.sendMessage(new TextComponentString("§7Set 'preferredAPI=VULKAN' in config to enable"));
            } else {
                sender.sendMessage(new TextComponentString("§cVulkan not supported on this system"));
            }
            return;
        }
        
        VulkanBackend vk = InitializationManager.getVulkanBackend();
        VulkanManager mgr = InitializationManager.getVulkanManager();
        
        sender.sendMessage(new TextComponentString("§aVulkan Backend Active"));
        sender.sendMessage(new TextComponentString("§7Device: §f" + vk.getDeviceName()));
        sender.sendMessage(new TextComponentString("§7API Version: §f" + vk.getApiVersion()));
        sender.sendMessage(new TextComponentString("§7Driver: §f" + vk.getDriverVersion()));
        
        // Show enabled features from config
        sender.sendMessage(new TextComponentString("§7Enabled Features:"));
        showFeature(sender, "  Timeline Semaphores", Config.isVulkanEnableTimelineSemaphores());
        showFeature(sender, "  Dynamic Rendering", Config.isVulkanEnableDynamicRendering());
        showFeature(sender, "  Synchronization2", Config.isVulkanEnableSynchronization2());
        showFeature(sender, "  Buffer Device Address", Config.isVulkanEnableBufferDeviceAddress());
        showFeature(sender, "  Descriptor Indexing", Config.isVulkanEnableDescriptorIndexing());
        showFeature(sender, "  Mesh Shaders", Config.isVulkanEnableMeshShaders());
        showFeature(sender, "  GPU Culling", Config.isVulkanEnableGPUCulling());
        
        sender.sendMessage(new TextComponentString(
            String.format("§7Frames in Flight: §f%d", Config.getVulkanMaxFramesInFlight())));
        sender.sendMessage(new TextComponentString(
            String.format("§7Descriptor Pool Size: §f%d", Config.getVulkanDescriptorPoolSize())));
    }
    
    /**
     * Show detailed DirectX information
     */
    private void showDirectXInfo(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("§6=== DirectX Details (9.0c - 12.2) ==="));
        
        if (!InitializationManager.isDirectXActive()) {
            sender.sendMessage(new TextComponentString("§eDirectX backend not active"));
            
            boolean vulkanActive = InitializationManager.isVulkanActive();
            String currentBackend = vulkanActive ? "Vulkan" : "OpenGL";
            sender.sendMessage(new TextComponentString("§7Current backend: " + currentBackend));
            
            if (UniversalCapabilities.isDirectXSupported()) {
                sender.sendMessage(new TextComponentString("§7DirectX is available but not enabled"));
                sender.sendMessage(new TextComponentString("§7Set 'preferredAPI=DIRECTX' in config to enable"));
                
                // Show available versions
                sender.sendMessage(new TextComponentString("§7Available versions:"));
                if (UniversalCapabilities.DirectX.supportsDX12) sender.sendMessage(new TextComponentString("  §a✓ DirectX 12"));
                if (UniversalCapabilities.DirectX.supportsDX11) sender.sendMessage(new TextComponentString("  §a✓ DirectX 11"));
                if (UniversalCapabilities.DirectX.supportsDX10) sender.sendMessage(new TextComponentString("  §a✓ DirectX 10"));
                if (UniversalCapabilities.DirectX.supportsDX9) sender.sendMessage(new TextComponentString("  §a✓ DirectX 9"));
            } else {
                sender.sendMessage(new TextComponentString("§cDirectX not supported on this system"));
                sender.sendMessage(new TextComponentString("§7(DirectX requires Windows)"));
            }
            return;
        }
        
        DirectXManager dx = InitializationManager.getDirectXManager();
        DirectXManager.Capabilities caps = dx.getCapabilities();
        DirectXManager.APIVersion activeAPI = dx.getCurrentAPI();
        
        // Active API
        sender.sendMessage(new TextComponentString("§b✓ DirectX Backend Active"));
        sender.sendMessage(new TextComponentString("§7Active API: §f" + activeAPI.displayName));
        sender.sendMessage(new TextComponentString("§7Feature Level: §f" + caps.featureLevel().name));
        
        // GPU Info
        sender.sendMessage(new TextComponentString("§7GPU Vendor: §f" + caps.vendor().name));
        sender.sendMessage(new TextComponentString("§7VRAM: §f" + 
            (caps.dedicatedVideoMemory() / 1024 / 1024) + " MB"));
        
        // Supported versions
        sender.sendMessage(new TextComponentString("§7Supported Versions:"));
        if (UniversalCapabilities.DirectX.supportsDX12_2) sender.sendMessage(new TextComponentString("  §a✓ DX12.2 (Ultimate)"));
        else if (UniversalCapabilities.DirectX.supportsDX12_1) sender.sendMessage(new TextComponentString("  §a✓ DX12.1"));
        else if (UniversalCapabilities.DirectX.supportsDX12) sender.sendMessage(new TextComponentString("  §a✓ DX12.0"));
        
        if (UniversalCapabilities.DirectX.supportsDX11_4) sender.sendMessage(new TextComponentString("  §a✓ DX11.4"));
        else if (UniversalCapabilities.DirectX.supportsDX11) sender.sendMessage(new TextComponentString("  §a✓ DX11.0"));
        
        if (UniversalCapabilities.DirectX.supportsDX10) sender.sendMessage(new TextComponentString("  §a✓ DX10.0"));
        if (UniversalCapabilities.DirectX.supportsDX9) sender.sendMessage(new TextComponentString("  §a✓ DX9.0c"));
        
        // DX12 Ultimate Features
        if (activeAPI.isDX12Family()) {
            sender.sendMessage(new TextComponentString("§7DirectX 12 Features:"));
            showFeature(sender, "  Ray Tracing", caps.supportsRayTracingTier1_0(), 
                caps.supportsRayTracingTier1_1() ? "Tier 1.1" : "Tier 1.0");
            showFeature(sender, "  Mesh Shaders", caps.supportsMeshShaders());
            showFeature(sender, "  Variable Rate Shading", caps.supportsVariableRateShading(),
                "Tier " + caps.variableRateShadingTier());
            showFeature(sender, "  Sampler Feedback", caps.supportsSamplerFeedback(),
                "Tier " + caps.samplerFeedbackTier());
            showFeature(sender, "  Tiled Resources", caps.supportsTiledResources(),
                "Tier " + caps.tiledResourcesTier());
            showFeature(sender, "  Conservative Rasterization", caps.supportsConservativeRasterization(),
                "Tier " + caps.conservativeRasterizationTier());
            showFeature(sender, "  Bindless Resources", caps.supportsBindlessResources(),
                "Tier " + caps.resourceBindingTier());
        }
        
        // Enabled features from config
        sender.sendMessage(new TextComponentString("§7Configured Settings:"));
        showFeature(sender, "  Tiled Resources", Config.isDirectXUseTiledResources());
        showFeature(sender, "  Resource Barriers", Config.isDirectXUseResourceBarriers());
        showFeature(sender, "  Descriptor Heaps", Config.isDirectXUseDescriptorHeaps());
        
        sender.sendMessage(new TextComponentString(
            String.format("§7Max Frame Latency: §f%d", Config.getDirectXMaxFrameLatency())));
        sender.sendMessage(new TextComponentString(
            String.format("§7Descriptor Heap Size: §f%,d", Config.getDirectXDescriptorHeapSize())));
    }
    
    private void showFeature(ICommandSender sender, String name, boolean enabled, String extra) {
        if (enabled) {
            sender.sendMessage(new TextComponentString("  §a✓ " + name + " §7(" + extra + ")"));
        } else {
            sender.sendMessage(new TextComponentString("  §c✗ " + name));
        }
    }
    
    /**
     * Show detailed OpenGL ES information
     */
    private void showGLESInfo(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("§6=== OpenGL ES Details ==="));
        
        if (!InitializationManager.isOpenGLESActive()) {
            sender.sendMessage(new TextComponentString("§eOpenGL ES backend not active"));
            
            boolean vulkanActive = InitializationManager.isVulkanActive();
            boolean directxActive = InitializationManager.isDirectXActive();
            String currentBackend = vulkanActive ? "Vulkan" : (directxActive ? "DirectX" : "OpenGL");
            sender.sendMessage(new TextComponentString("§7Current backend: " + currentBackend));
            
            if (UniversalCapabilities.GLES.isGLESContext || Config.getBoolean("allowGLESEmulation")) {
                sender.sendMessage(new TextComponentString("§7OpenGL ES is available but not enabled"));
                sender.sendMessage(new TextComponentString("§7Set 'preferredAPI=OPENGL_ES' in config to enable"));
            } else {
                sender.sendMessage(new TextComponentString("§cOpenGL ES not supported on this system"));
                sender.sendMessage(new TextComponentString("§7Enable 'allowGLESEmulation=true' to use ANGLE/Zink"));
            }
            return;
        }
        
        OpenGLESManager gles = InitializationManager.getOpenGLESManager();
        
        sender.sendMessage(new TextComponentString("§dOpenGL ES Backend Active"));
        sender.sendMessage(new TextComponentString("§7GLES Version: §f" + 
            UniversalCapabilities.GLES.majorVersion + "." + UniversalCapabilities.GLES.minorVersion));
        sender.sendMessage(new TextComponentString("§7GLSL ES Version: §f" + 
            UniversalCapabilities.GLSL.esMajorVersion + "." + UniversalCapabilities.GLSL.esMinorVersion));
        
        // Show vendor and renderer
        sender.sendMessage(new TextComponentString("§7Vendor: §f" + UniversalCapabilities.GLES.vendorString));
        sender.sendMessage(new TextComponentString("§7Renderer: §f" + UniversalCapabilities.GLES.rendererString));
        sender.sendMessage(new TextComponentString("§7Emulated: §f" + 
            (UniversalCapabilities.GLES.isEmulatedGLES ? "§eYes §7(ANGLE/Zink)" : "§aNo §7(Native)")));
        
        // Show supported versions
        sender.sendMessage(new TextComponentString("§7Supported Versions:"));
        if (UniversalCapabilities.GLES.ES32) {
            sender.sendMessage(new TextComponentString("  §a✓ §7ES 3.2 §f(compute shaders, geometry shaders)"));
        }
        if (UniversalCapabilities.GLES.ES31) {
            sender.sendMessage(new TextComponentString("  §a✓ §7ES 3.1 §f(compute shaders, SSBOs)"));
        }
        if (UniversalCapabilities.GLES.ES30) {
            sender.sendMessage(new TextComponentString("  §a✓ §7ES 3.0 §f(instancing, transform feedback)"));
        }
        if (UniversalCapabilities.GLES.ES20) {
            sender.sendMessage(new TextComponentString("  §a✓ §7ES 2.0 §f(programmable shaders)"));
        }
        
        // Show enabled features from config
        sender.sendMessage(new TextComponentString("§7"));
        sender.sendMessage(new TextComponentString("§7Enabled Features:"));
        showFeature(sender, "  Shader Cache", Config.getBoolean("glesEnableShaderCache"));
        showFeature(sender, "  Program Binary Cache", Config.getBoolean("glesEnableProgramBinaryCache"));
        showFeature(sender, "  Auto Transpile Shaders", Config.getBoolean("glesAutoTranspileShaders"));
        showFeature(sender, "  Compute Shaders", Config.getBoolean("glesEnableComputeShaders"));
        showFeature(sender, "  Persistent Mapping", Config.getBoolean("glesPreferPersistentMapping"));
        showFeature(sender, "  Extensions Auto-Enable", Config.getBoolean("glesEnableExtensions"));
        
        sender.sendMessage(new TextComponentString(
            String.format("§7Command Pool Size: §f%d", Config.getInt("glesCommandPoolSize"))));
        sender.sendMessage(new TextComponentString(
            String.format("§7Max Texture Size: §f%d", Config.getInt("glesMaxTextureSize"))));
        sender.sendMessage(new TextComponentString(
            String.format("§7Optimization Level: §f%d", Config.getInt("glesOptimizationLevel"))));
    }
    
    /**
     * Show current configuration
     */
    private void showConfigInfo(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("§6=== Astralis Configuration ==="));
        
        sender.sendMessage(new TextComponentString("§7Preferred API: §f" + Config.getPreferredAPI()));
        sender.sendMessage(new TextComponentString("§7Shader Engine: §f" + Config.getPreferredShaderEngine()));
        sender.sendMessage(new TextComponentString("§7Performance Profile: §f" + Config.getPerformanceProfile()));
        sender.sendMessage(new TextComponentString("§7Logging Level: §f" + Config.getLoggingLevel()));
        
        sender.sendMessage(new TextComponentString("§7"));
        sender.sendMessage(new TextComponentString("§7Vulkan Settings:"));
        sender.sendMessage(new TextComponentString(
            String.format("§7  Validation: %s", Config.isVulkanValidationLayers() ? "§aON" : "§cOFF")));
        sender.sendMessage(new TextComponentString(
            String.format("§7  Debug Mode: %s", Config.isVulkanDebugMode() ? "§aON" : "§cOFF")));
        sender.sendMessage(new TextComponentString(
            String.format("§7  GPU Driven: %s", Config.isVulkanEnableGPUDrivenRendering() ? "§aON" : "§cOFF")));
        
        sender.sendMessage(new TextComponentString("§7"));
        sender.sendMessage(new TextComponentString("§7Config file: §f" + Config.getConfigPath()));
    }
    
    /**
     * Show detected GPU capabilities
     */
    private void showCapabilities(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("§6=== GPU Capabilities ==="));
        
        sender.sendMessage(new TextComponentString("§7OpenGL: §f" + 
            UniversalCapabilities.getOpenGLVersion()));
        sender.sendMessage(new TextComponentString("§7GLSL: §f" + 
            UniversalCapabilities.getGLSLVersion()));
        
        if (UniversalCapabilities.GLES.isGLESContext || UniversalCapabilities.GLES.majorVersion > 0) {
            sender.sendMessage(new TextComponentString("§dOpenGL ES: Supported §f(v" + 
                UniversalCapabilities.GLES.majorVersion + "." +
                UniversalCapabilities.GLES.minorVersion + ")"));
            sender.sendMessage(new TextComponentString("§7GLSL ES: §f" + 
                UniversalCapabilities.GLSL.esMajorVersion + "." + UniversalCapabilities.GLSL.esMinorVersion));
        } else {
            sender.sendMessage(new TextComponentString("§7OpenGL ES: Not Detected"));
        }
        
        if (UniversalCapabilities.isVulkanSupported()) {
            sender.sendMessage(new TextComponentString("§aVulkan: Supported §f(v" + 
                UniversalCapabilities.getVulkanVersionMajor() + "." +
                UniversalCapabilities.getVulkanVersionMinor() + ")"));
        } else {
            sender.sendMessage(new TextComponentString("§cVulkan: Not Supported"));
        }
        
        if (UniversalCapabilities.isDirectXSupported()) {
            sender.sendMessage(new TextComponentString("§aDirectX: Supported §f(" + 
                (UniversalCapabilities.DirectX.D3D12 ? "D3D12" : "D3D11") + ")"));
        } else {
            sender.sendMessage(new TextComponentString("§cDirectX: Not Supported"));
        }
        
        sender.sendMessage(new TextComponentString("§7"));
        sender.sendMessage(new TextComponentString("§7Features:"));
        showFeature(sender, "  DSA", UniversalCapabilities.Features.DSA);
        showFeature(sender, "  Persistent Mapping", UniversalCapabilities.Features.persistentMapping);
        showFeature(sender, "  Compute Shaders", UniversalCapabilities.Features.computeShaders);
        showFeature(sender, "  Bindless Textures", UniversalCapabilities.Features.bindlessTextures);
        showFeature(sender, "  SPIR-V", UniversalCapabilities.SPIRV.hasGLSPIRV);
    }
    
    /**
     * Show GL state cache statistics
     */
    private void showCacheStats(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("§6=== GL State Cache Statistics ==="));
        sender.sendMessage(new TextComponentString(
            String.format("§7Cache Hit Rate: §a%.1f%%",
                GLStateCache.getSkipPercentage())
        ));
        sender.sendMessage(new TextComponentString(
            "§7This shows how many redundant GL calls were eliminated."
        ));
        sender.sendMessage(new TextComponentString(
            "§7Higher is better (means more optimization)."
        ));
        
        // Reset metrics for next measurement period
        sender.sendMessage(new TextComponentString("§eMetrics reset for next measurement."));
        GLStateCache.resetMetrics();
    }
    
    /**
     * Show Metal backend information
     */
    private void showMetalInfo(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("§6=== Metal Backend Information ==="));
        
        if (InitializationManager.isMetalActive()) {
            var metalMgr = InitializationManager.getMetalManager();
            sender.sendMessage(new TextComponentString("§aStatus: ACTIVE"));
            sender.sendMessage(new TextComponentString("§7Device: §f" + metalMgr.getDeviceName()));
            sender.sendMessage(new TextComponentString("§7Metal Version: §f" + 
                UniversalCapabilities.getMetalVersionMajor() + "." + UniversalCapabilities.getMetalVersionMinor()));
            
            sender.sendMessage(new TextComponentString("§7"));
            sender.sendMessage(new TextComponentString("§7Modern Features:"));
            showFeature(sender, "  Ray Tracing (2.2+)", UniversalCapabilities.Features.metalRayTracing);
            showFeature(sender, "  Mesh Shaders (2.4+)", UniversalCapabilities.Features.metalMeshShaders);
            showFeature(sender, "  Argument Buffers (2.0+)", UniversalCapabilities.Features.metalArgumentBuffers);
            showFeature(sender, "  ICB (2.0+)", UniversalCapabilities.Features.metalICB);
            
            if (InitializationManager.getMSLPipeline() != null) {
                sender.sendMessage(new TextComponentString("§7MSL Pipeline: §aActive (v" + Config.getMSLLanguageVersion() + ")"));
            }
        } else {
            sender.sendMessage(new TextComponentString("§cStatus: NOT ACTIVE"));
            sender.sendMessage(new TextComponentString("§7Metal is only available on macOS/iOS"));
        }
    }
    
    /**
     * Show OpenGL backend information
     */
    private void showOpenGLInfo(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("§6=== OpenGL Backend Information ==="));
        sender.sendMessage(new TextComponentString("§aStatus: ACTIVE (Primary)"));
        
        int glMajor = UniversalCapabilities.getGLVersionMajor();
        int glMinor = UniversalCapabilities.getGLVersionMinor();
        sender.sendMessage(new TextComponentString("§7OpenGL Version: §f" + glMajor + "." + glMinor));
        sender.sendMessage(new TextComponentString("§7Vendor: §f" + UniversalCapabilities.getGLVendor()));
        sender.sendMessage(new TextComponentString("§7Renderer: §f" + UniversalCapabilities.getGLRenderer()));
        
        sender.sendMessage(new TextComponentString("§7"));
        sender.sendMessage(new TextComponentString("§7Modern Features:"));
        showFeature(sender, "  Ray Tracing (extension)", stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper.hasFeatureRayTracing());
        showFeature(sender, "  Mesh Shading (extension)", stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper.hasFeatureMeshShading());
        showFeature(sender, "  Bindless Textures", stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper.hasFeatureBindlessTextures());
        showFeature(sender, "  Compute Shaders (4.3+)", UniversalCapabilities.Features.computeShaders);
        showFeature(sender, "  DSA (4.5+)", UniversalCapabilities.Features.DSA);
        showFeature(sender, "  Persistent Mapping", UniversalCapabilities.Features.persistentMapping);
        
        if (InitializationManager.getOpenGLManager() != null) {
            sender.sendMessage(new TextComponentString("§7OpenGL Manager: §aActive"));
        }
        if (InitializationManager.getGLSLPipeline() != null) {
            sender.sendMessage(new TextComponentString("§7GLSL Pipeline: §aActive (v" + Config.getGLSLLanguageVersion() + ")"));
        }
    }
    
    /**
     * Show GLSL pipeline information
     */
    private void showGLSLInfo(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("§6=== GLSL Pipeline Information ==="));
        
        if (InitializationManager.getGLSLPipeline() != null) {
            sender.sendMessage(new TextComponentString("§aStatus: ACTIVE"));
            sender.sendMessage(new TextComponentString("§7Target Version: §fGLSL " + Config.getGLSLLanguageVersion()));
            sender.sendMessage(new TextComponentString("§7Profile: §f" + (Config.isGLSLCoreProfile() ? "Core" : "Compatibility")));
            sender.sendMessage(new TextComponentString("§7Optimization: §fLevel " + Config.getGLSLOptimizationLevel()));
            
            sender.sendMessage(new TextComponentString("§7"));
            sender.sendMessage(new TextComponentString("§7Shader Features:"));
            showFeature(sender, "  Compute Shaders (4.30+)", Config.isGLSLEnableComputeShaders());
            showFeature(sender, "  Tessellation (4.00+)", Config.isGLSLEnableTessellationShaders());
            showFeature(sender, "  Geometry Shaders (1.50+)", Config.isGLSLEnableGeometryShaders());
            showFeature(sender, "  UBOs (1.40+)", Config.isGLSLEnableUniformBufferObjects());
            showFeature(sender, "  SSBOs (4.30+)", Config.isGLSLEnableShaderStorageBuffers());
            showFeature(sender, "  Binary Cache", Config.isGLSLEnableBinaryCache());
            
            sender.sendMessage(new TextComponentString("§7"));
            sender.sendMessage(new TextComponentString("§7Cross-Compilation:"));
            showFeature(sender, "  HLSL→GLSL", Config.isGLSLEnableHLSLTranslation());
            showFeature(sender, "  SPIR-V→GLSL", Config.isGLSLEnableSPIRVTranslation());
        } else {
            sender.sendMessage(new TextComponentString("§cStatus: NOT ACTIVE"));
            sender.sendMessage(new TextComponentString("§7Enable with glslEnabled=true in config"));
        }
    }
    
    /**
     * Show MSL pipeline information
     */
    private void showMSLInfo(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("§6=== MSL Pipeline Information ==="));
        
        if (InitializationManager.getMSLPipeline() != null) {
            sender.sendMessage(new TextComponentString("§aStatus: ACTIVE"));
            sender.sendMessage(new TextComponentString("§7Target Version: §fMSL " + Config.getMSLLanguageVersion()));
            sender.sendMessage(new TextComponentString("§7Optimization: §fLevel " + Config.getMSLOptimizationLevel()));
            sender.sendMessage(new TextComponentString("§7Fast Math: §f" + (Config.isMSLFastMathEnabled() ? "Enabled" : "Disabled")));
            
            sender.sendMessage(new TextComponentString("§7"));
            sender.sendMessage(new TextComponentString("§7Shader Features:"));
            showFeature(sender, "  Ray Tracing (2.2+)", Config.isMSLEnableRaytracing());
            showFeature(sender, "  Mesh Shaders (2.4+)", Config.isMSLEnableMeshShaders());
            showFeature(sender, "  Argument Buffers (2.0+)", Config.isMSLEnableArgumentBuffers());
            showFeature(sender, "  ICB (2.0+)", Config.isMSLEnableIndirectCommandBuffers());
            showFeature(sender, "  Function Pointers (2.3+)", Config.isMSLEnableFunctionPointers());
            showFeature(sender, "  Binary Cache", Config.isMSLEnableBytecodeCache());
            
            sender.sendMessage(new TextComponentString("§7"));
            sender.sendMessage(new TextComponentString("§7Cross-Compilation:"));
            showFeature(sender, "  GLSL→MSL", Config.isMSLEnableGLSLTranslation());
            showFeature(sender, "  HLSL→MSL", Config.isMSLEnableHLSLTranslation());
            showFeature(sender, "  SPIR-V→MSL", Config.isMSLEnableSPIRVTranslation());
        } else {
            sender.sendMessage(new TextComponentString("§cStatus: NOT ACTIVE"));
            sender.sendMessage(new TextComponentString("§7MSL requires active Metal backend"));
        }
    }
    
    /**
     * Show DeepMix mixin loader status and configuration.
     */
    private void showDeepMixInfo(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("§6=== DeepMix Mixin Loader ==="));

        boolean enabled = stellar.snow.astralis.config.Config.isDeepMixEnabled();

        if (!enabled) {
            sender.sendMessage(new TextComponentString("§cStatus: DISABLED §7(deepmixEnabled=false in config)"));
            sender.sendMessage(new TextComponentString("§7Mixins may still be loaded via legacy FML path."));
            return;
        }

        sender.sendMessage(new TextComponentString("§aStatus: ACTIVE"));
        sender.sendMessage(new TextComponentString("§7Mixin Config: §fmixins.astralis.json"));
        sender.sendMessage(new TextComponentString("§7Priority: §f"
                + stellar.snow.astralis.config.Config.getDeepMixPriority()
                + " §7(Snowium=1125 | Shaders=1000 | Sodium=750 | Default=500)"));

        sender.sendMessage(new TextComponentString("§7"));
        sender.sendMessage(new TextComponentString("§7Loader Modes:"));
        showFeature(sender, "  Early Loader (GL hook window)",
                stellar.snow.astralis.config.Config.isDeepMixUseEarlyLoader());
        showFeature(sender, "  Late Loader (optional compat)",
                stellar.snow.astralis.config.Config.isDeepMixUseLateLoader());

        sender.sendMessage(new TextComponentString("§7"));
        sender.sendMessage(new TextComponentString("§7Compatibility Patches:"));
        showFeature(sender, "  SpongeForgeFixer",
                stellar.snow.astralis.config.Config.isDeepMixAllowSpongeForgePatch());
        showFeature(sender, "  MixinExtrasFixer (ASM 5.0.x)",
                stellar.snow.astralis.config.Config.isDeepMixAllowMixinExtrasFix());
        showFeature(sender, "  AncientModPatch",
                stellar.snow.astralis.config.Config.isDeepMixAllowAncientModPatch());
        showFeature(sender, "  Auto-resolve conflicts",
                stellar.snow.astralis.config.Config.isDeepMixAutoResolveConflicts());

        sender.sendMessage(new TextComponentString("§7"));
        sender.sendMessage(new TextComponentString("§7Logging:"));
        showFeature(sender, "  Log queued configs",
                stellar.snow.astralis.config.Config.isDeepMixLogQueuedConfigs());
        showFeature(sender, "  Log hijacks",
                stellar.snow.astralis.config.Config.isDeepMixLogHijacks());

        String[] lateConfigs = stellar.snow.astralis.config.Config.getDeepMixLateConfigs();
        if (lateConfigs.length > 0) {
            sender.sendMessage(new TextComponentString("§7"));
            sender.sendMessage(new TextComponentString("§7Late-loader configs:"));
            for (String cfg : lateConfigs) {
                sender.sendMessage(new TextComponentString("  §f" + cfg));
            }
        }
    }

    /**
     * Show Mini_DirtyRoom modernization layer status and diagnostics.
     */
    private void showMDRInfo(ICommandSender sender) {
        sender.sendMessage(new TextComponentString("§6=== Mini_DirtyRoom Modernization Layer ==="));

        boolean enabled = stellar.snow.astralis.config.Config.isMDREnabled();

        if (!enabled) {
            sender.sendMessage(new TextComponentString("§cStatus: DISABLED §7(mdrEnabled=false in config)"));
            return;
        }

        boolean bootstrapDone = stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore.isBootstrapComplete();
        sender.sendMessage(new TextComponentString(
                "§" + (bootstrapDone ? "a" : "e") + "Bootstrap: "
                + (bootstrapDone ? "COMPLETE" : "PENDING")));
        sender.sendMessage(new TextComponentString("§7Version: §f"
                + stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore.VERSION
                + " §7| Build: §f"
                + stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore.BUILD_ID));
        sender.sendMessage(new TextComponentString("§7LWJGL Target: §f"
                + stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore.TARGET_LWJGL_VERSION
                + "  §7Java Target: §f"
                + stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore.TARGET_JAVA_VERSION));

        // Redirect rules
        int redirects = stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore.getClassRedirects().size();
        sender.sendMessage(new TextComponentString("§7Redirect Rules: §f" + redirects));

        // Environment
        stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore.EnvironmentInfo env =
                stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore.getEnvironment();
        if (env != null) {
            sender.sendMessage(new TextComponentString("§7"));
            sender.sendMessage(new TextComponentString("§7Environment:"));
            sender.sendMessage(new TextComponentString("  §7Java: §f" + env.javaVersion
                    + " §7(" + env.javaVendor + ")"));
            sender.sendMessage(new TextComponentString("  §7Platform: §f"
                    + (env.isAndroid ? "Android" : env.isWindows ? "Windows"
                       : env.isMacOS ? "macOS" : "Linux")
                    + (env.isARM ? "/ARM" : "") + (env.is64Bit ? " 64-bit" : " 32-bit")));
            sender.sendMessage(new TextComponentString("  §7Mod Loader: §f" + env.detectedLoader));
            sender.sendMessage(new TextComponentString("  §7MC Version: §f" + env.minecraftVersion));
            sender.sendMessage(new TextComponentString("  §7RAM: §f" + env.maxMemoryMB + " MB"));
            sender.sendMessage(new TextComponentString("  §7Relaunched: §f" + stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore.RELAUNCHED.get()));
        }

        // Config flags
        sender.sendMessage(new TextComponentString("§7"));
        sender.sendMessage(new TextComponentString("§7Configuration:"));
        showFeature(sender, "  Auto-upgrade Java",
                stellar.snow.astralis.config.Config.isMDRAutoUpgradeJava());
        showFeature(sender, "  Download JRE",
                stellar.snow.astralis.config.Config.isMDRDownloadJRE());
        showFeature(sender, "  Download LWJGL",
                stellar.snow.astralis.config.Config.isMDRDownloadLWJGL());
        showFeature(sender, "  Verify checksums",
                stellar.snow.astralis.config.Config.isMDRVerifyChecksums());
        showFeature(sender, "  Override files on disk",
                stellar.snow.astralis.config.Config.isMDROverrideFilesOnDisk());
        showFeature(sender, "  Log bootstrap",
                stellar.snow.astralis.config.Config.isMDRLogBootstrap());
        sender.sendMessage(new TextComponentString("  §7Timeout: §f"
                + stellar.snow.astralis.config.Config.getMDRBootstrapTimeoutMs() + " ms"));
        sender.sendMessage(new TextComponentString("  §7Download timeout: §f"
                + stellar.snow.astralis.config.Config.getMDRDownloadTimeoutSecs() + " s"));

        String mirror = stellar.snow.astralis.config.Config.getMDRLwjglMirrorUrl();
        if (!mirror.isEmpty()) {
            sender.sendMessage(new TextComponentString("  §7LWJGL mirror: §f" + mirror));
        }

        // Boot warnings
        java.util.List<String> warnings =
                stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore.getBootWarnings();
        if (!warnings.isEmpty()) {
            sender.sendMessage(new TextComponentString("§7"));
            sender.sendMessage(new TextComponentString("§eWarnings (" + warnings.size() + "):"));
            for (String w : warnings) {
                sender.sendMessage(new TextComponentString("  §e⚠ §7" + w));
            }
        }

        // Fatal error if any
        Throwable fatal = stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore.getFatalError();
        if (fatal != null) {
            sender.sendMessage(new TextComponentString("§7"));
            sender.sendMessage(new TextComponentString("§c✗ FATAL: " + fatal.getMessage()));
        }
    }

    /**
     * Helper to show feature status
     */
    private void showFeature(ICommandSender sender, String name, boolean enabled) {
        String status = enabled ? "§a✓" : "§c✗";
        sender.sendMessage(new TextComponentString(String.format("%s §7%s", status, name)));
    }
    
    @Override
    public int getRequiredPermissionLevel() {
        return 0; // No cheats needed
    }
}
