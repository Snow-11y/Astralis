package stellar.snow.astralis.api.common;

import stellar.snow.astralis.Astralis;
import stellar.snow.astralis.api.common.BufferOps;

/**
 * BackendCoordinator - Central authority for backend selection
 * 
 * Manages the interaction between:
 * - OpenGLManager (desktop GL)
 * - OpenGLESManager (mobile ES)
 * - VulkanManager (modern explicit API)
 * 
 * Selection priority (configurable):
 * 1. Vulkan (if enabled in config AND supported)
 * 2. OpenGL ES (if native mobile)
 * 3. OpenGL Desktop (default)
 */
public class BackendCoordinator {
    
    public enum BackendType {
        OPENGL_DESKTOP,
        OPENGL_ES,
        VULKAN,
        NONE
    }
    
    private static BackendType activeType = BackendType.NONE;
    private static BufferOps activeBackend = null;
    private static boolean initialized = false;
    
    // Config
    private static boolean preferVulkan = false; // Disabled by default
    private static boolean preferES = true; // Prefer ES on mobile
    
    public static void initialize() {
        if (initialized) return;
        
        Astralis.LOGGER.info("[BackendCoordinator] Selecting optimal graphics backend...");
        
        // Detect all capabilities first
        GLCapabilities.detect();
        VulkanCapabilities.detect();
        
        // Select backend based on priority + config
        selectBackend();
        
        if (activeBackend != null) {
            Astralis.LOGGER.info("[BackendCoordinator] Active: {} ({})", activeType, activeBackend.getName());
        } else {
            Astralis.LOGGER.error("[BackendCoordinator] No compatible backend found!");
        }
        
        initialized = true;
    }
    
    private static void selectBackend() {
        // Priority 1: Vulkan (if explicitly enabled)
        if (preferVulkan && VulkanCapabilities.isSupported) {
            VulkanManager.enable();
            VulkanManager.initialize();
            
            if (VulkanManager.isEnabled()) {
                activeBackend = VulkanManager.getBackend();
                activeType = BackendType.VULKAN;
                return;
            }
        }
        
        // Priority 2: OpenGL ES (if native mobile)
        if (preferES && isLikelyMobileEnvironment()) {
            OpenGLESManager.initialize();
            
            if (OpenGLESManager.isEnabled()) {
                activeBackend = OpenGLESManager.getBackend();
                activeType = BackendType.OPENGL_ES;
                return;
            }
        }
        
        // Priority 3: Desktop OpenGL (default fallback)
        OpenGLManager.initialize();
        
        if (OpenGLManager.isEnabled()) {
            activeBackend = OpenGLManager.getBackend();
            activeType = BackendType.OPENGL_DESKTOP;
            return;
        }
        
        // Nothing worked
        activeType = BackendType.NONE;
        activeBackend = null;
    }
    
    private static boolean isLikelyMobileEnvironment() {
        // Heuristics to detect mobile
        
        String renderer = GLCapabilities.glRenderer.toLowerCase();
        String vendor = GLCapabilities.glVendor.toLowerCase();
        
        // ARM GPUs = mobile
        if (renderer.contains("mali") || 
            renderer.contains("adreno") ||
            renderer.contains("powervr") ||
            renderer.contains("videocore")) {
            return true;
        }
        
        // Android vendor
        if (vendor.contains("qualcomm") || 
            vendor.contains("arm") ||
            vendor.contains("imagination")) {
            return true;
        }
        
        // gl4es wrapper = likely Android
        if (GLCapabilities.wrapperName.equals("gl4es")) {
            return true;
        }
        
        return false;
    }
    
    // Public API
    
    public static BufferOps getActiveBackend() {
        if (!initialized) initialize();
        return activeBackend;
    }
    
    public static BackendType getActiveType() {
        if (!initialized) initialize();
        return activeType;
    }
    
    public static boolean isEnabled() {
        if (!initialized) initialize();
        return activeBackend != null;
    }
    
    public static void setPreferVulkan(boolean prefer) {
        preferVulkan = prefer;
    }
    
    public static void setPreferES(boolean prefer) {
        preferES = prefer;
    }
    
    public static String getDetailedReport() {
        if (!initialized) initialize();
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== Backend Coordinator Status ===\n");
        sb.append("Active Backend: ").append(activeType).append("\n");
        
        if (activeBackend != null) {
            sb.append("Implementation: ").append(activeBackend.getName()).append("\n");
        }
        
        sb.append("\nAvailable Backends:\n");
        sb.append("  OpenGL Desktop: ").append(OpenGLManager.isEnabled()).append("\n");
        sb.append("  OpenGL ES: ").append(OpenGLESManager.isEnabled()).append("\n");
        sb.append("  Vulkan: ").append(VulkanManager.isEnabled()).append("\n");
        
        sb.append("\nConfiguration:\n");
        sb.append("  Prefer Vulkan: ").append(preferVulkan).append("\n");
        sb.append("  Prefer ES: ").append(preferES).append("\n");
        
        return sb.toString();
    }
}
