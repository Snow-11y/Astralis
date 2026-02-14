package stellar.snow.astralis.engine.render.minecraft.common;

import org.lwjgl.vulkan.VkDevice;

public class MinecraftGraphicsContext {
    public String getGPUName() { return "Unknown GPU"; }
    public String getRendererType() { return "Vulkan"; }
    public VkDevice getVulkanDevice() { return null; }
    public long getPhysicalDevice() { return 0L; }
}
