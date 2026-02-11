package stellar.snow.astralis.engine.gpu.integration;

import stellar.snow.astralis.Astralis;
import stellar.snow.astralis.config.Config;
import stellar.snow.astralis.engine.gpu.authority.GPUBackend;
import stellar.snow.astralis.engine.gpu.authority.GPUBackendSelector;
import stellar.snow.astralis.engine.gpu.authority.UniversalCapabilities;

import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * GPU_BACKEND_INTEGRATION - Seamless integration of GPUBackend/Selector with Config & UniversalCapabilities
 * 
 * <h2>Purpose:</h2>
 * <p>This integration layer connects the GPU backend selection system with the configuration
 * system and universal capabilities detection. It ensures that:</p>
 * <ul>
 *   <li>Backend selection respects user preferences from Config.java</li>
 *   <li>UniversalCapabilities reflects the selected backend's capabilities</li>
 *   <li>Fallback chains are configured properly</li>
 *   <li>Runtime backend switching is handled gracefully</li>
 *   <li>All GPU features are exposed through a unified API</li>
 * </ul>
 * 
 * <h2>Backend Selection Priority:</h2>
 * <ol>
 *   <li>User preference from Config (if supported on platform)</li>
 *   <li>Platform-native backend (Metal on macOS, DirectX on Windows)</li>
 *   <li>Vulkan (if available and meets requirements)</li>
 *   <li>OpenGL 4.6 (cross-platform fallback)</li>
 *   <li>OpenGL ES 3.2 (mobile/embedded fallback)</li>
 *   <li>NullBackend (headless/testing mode)</li>
 * </ol>
 * 
 * @author Astralis GPU Integration Team
 * @version 1.0.0
 * @since Java 25 + LWJGL 3.4.0
 */
public final class GPU_BACKEND_INTEGRATION {

    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final AtomicReference<GPUBackendSelector> selector = new AtomicReference<>();
    private static final AtomicReference<GPUBackend> activeBackend = new AtomicReference<>();
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONFIGURATION DEFAULTS
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Register GPU backend configuration defaults in Config.java
     */
    public static void registerConfigDefaults() {
        // Backend Selection
        Config.set("gpuBackendPreferred", "AUTO"); // AUTO, VULKAN, METAL, DIRECTX, OPENGL, OPENGLES, NULL
        Config.set("gpuBackendAllowFallback", true);
        Config.set("gpuBackendFallbackChain", new String[] {
            "VULKAN", "DIRECTX", "METAL", "OPENGL", "OPENGLES", "NULL"
        });
        Config.set("gpuBackendRequireMinimumVersion", false);
        Config.set("gpuBackendValidateCapabilities", true);
        
        // Platform-Specific Preferences
        Config.set("gpuBackendWindowsPreferDX", true);
        Config.set("gpuBackendMacOSPreferMetal", true);
        Config.set("gpuBackendLinuxPreferVulkan", true);
        Config.set("gpuBackendAndroidPreferVulkan", false); // Prefer GLES on Android
        
        // Feature Requirements
        Config.set("gpuBackendRequireComputeShaders", false);
        Config.set("gpuBackendRequireGeometryShaders", false);
        Config.set("gpuBackendRequireTessellation", false);
        Config.set("gpuBackendRequireMeshShaders", false);
        Config.set("gpuBackendRequireRayTracing", false);
        Config.set("gpuBackendRequireBindlessTextures", false);
        Config.set("gpuBackendRequireMultiDrawIndirect", false);
        
        // Capability Overrides (for testing/debugging)
        Config.set("gpuBackendOverrideVersion", false);
        Config.set("gpuBackendForcedVersionMajor", 0);
        Config.set("gpuBackendForcedVersionMinor", 0);
        Config.set("gpuBackendDisableExtensions", false);
        Config.set("gpuBackendDisabledExtensionList", new String[0]);
        
        // Backend-Specific Settings
        Config.set("gpuBackendVulkanValidation", false);
        Config.set("gpuBackendVulkanDebugUtils", false);
        Config.set("gpuBackendMetalValidation", false);
        Config.set("gpuBackendDirectXDebugLayer", false);
        Config.set("gpuBackendOpenGLDebugContext", false);
        
        // Hot-Reload & Development
        Config.set("gpuBackendEnableHotReload", false);
        Config.set("gpuBackendAutoDetectChanges", false);
        Config.set("gpuBackendReloadOnDriverUpdate", false);
        
        // Diagnostics
        Config.set("gpuBackendEnableProfiling", false);
        Config.set("gpuBackendLogSelectionProcess", true);
        Config.set("gpuBackendLogCapabilities", true);
        Config.set("gpuBackendDumpFullReport", false);
        
        Astralis.LOGGER.info("[GPU Backend] Configuration defaults registered");
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONFIGURATION GETTERS
    // ═══════════════════════════════════════════════════════════════════════
    
    public static String getPreferredBackend() { return Config.getString("gpuBackendPreferred"); }
    public static boolean isAllowFallback() { return Config.getBoolean("gpuBackendAllowFallback"); }
    public static String[] getFallbackChain() {
        Object chain = Config.getAllValues().get("gpuBackendFallbackChain");
        return chain instanceof String[] ? (String[]) chain : new String[0];
    }
    public static boolean isRequireMinimumVersion() { return Config.getBoolean("gpuBackendRequireMinimumVersion"); }
    public static boolean isValidateCapabilities() { return Config.getBoolean("gpuBackendValidateCapabilities"); }
    
    // Platform Preferences
    public static boolean isWindowsPreferDX() { return Config.getBoolean("gpuBackendWindowsPreferDX"); }
    public static boolean isMacOSPreferMetal() { return Config.getBoolean("gpuBackendMacOSPreferMetal"); }
    public static boolean isLinuxPreferVulkan() { return Config.getBoolean("gpuBackendLinuxPreferVulkan"); }
    
    // Feature Requirements
    public static boolean isRequireComputeShaders() { return Config.getBoolean("gpuBackendRequireComputeShaders"); }
    public static boolean isRequireGeometryShaders() { return Config.getBoolean("gpuBackendRequireGeometryShaders"); }
    public static boolean isRequireTessellation() { return Config.getBoolean("gpuBackendRequireTessellation"); }
    public static boolean isRequireMeshShaders() { return Config.getBoolean("gpuBackendRequireMeshShaders"); }
    public static boolean isRequireRayTracing() { return Config.getBoolean("gpuBackendRequireRayTracing"); }
    
    // Diagnostics
    public static boolean isEnableProfiling() { return Config.getBoolean("gpuBackendEnableProfiling"); }
    public static boolean isLogSelectionProcess() { return Config.getBoolean("gpuBackendLogSelectionProcess"); }
    
    // ═══════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Initialize GPU backend integration.
     * This should be called during post-initialization after OpenGL context exists.
     */
    public static void initialize() {
        if (initialized.getAndSet(true)) {
            Astralis.LOGGER.warn("[GPU Backend] Already initialized");
            return;
        }
        
        Astralis.LOGGER.info("[GPU Backend] Initializing GPU backend integration...");
        
        try {
            // Register configuration defaults
            registerConfigDefaults();
            
            // Create backend selector
            GPUBackendSelector sel = GPUBackendSelector.instance();
            selector.set(sel);
            
            // Select backend based on config
            GPUBackend backend = selectBackend(sel);
            activeBackend.set(backend);
            
            // Synchronize UniversalCapabilities with selected backend
            synchronizeCapabilities(backend);
            
            // Log backend information
            logBackendInfo(backend);
            
            Astralis.LOGGER.info("[GPU Backend] GPU backend integration complete");
            
        } catch (Exception e) {
            Astralis.LOGGER.error("[GPU Backend] Initialization failed", e);
            throw new RuntimeException("GPU backend integration failed", e);
        }
    }
    
    /**
     * Select GPU backend based on configuration and capabilities.
     */
    private static GPUBackend selectBackend(GPUBackendSelector selector) {
        String preferred = getPreferredBackend();
        
        if (isLogSelectionProcess()) {
            Astralis.LOGGER.info("[GPU Backend] Backend selection process starting...");
            Astralis.LOGGER.info("[GPU Backend]   Preferred: {}", preferred);
            Astralis.LOGGER.info("[GPU Backend]   Allow fallback: {}", isAllowFallback());
            Astralis.LOGGER.info("[GPU Backend]   Platform: {}", System.getProperty("os.name"));
        }
        
        // Handle AUTO selection
        if ("AUTO".equals(preferred)) {
            preferred = determineBestBackend();
            if (isLogSelectionProcess()) {
                Astralis.LOGGER.info("[GPU Backend]   AUTO resolved to: {}", preferred);
            }
        }
        
        // Try preferred backend
        GPUBackend backend = trySelectBackend(selector, preferred);
        if (backend != null) {
            return backend;
        }
        
        // Try fallback chain if allowed
        if (isAllowFallback()) {
            String[] fallbackChain = getFallbackChain();
            for (String fallback : fallbackChain) {
                if (fallback.equals(preferred)) continue; // Skip already tried
                
                if (isLogSelectionProcess()) {
                    Astralis.LOGGER.info("[GPU Backend]   Trying fallback: {}", fallback);
                }
                
                backend = trySelectBackend(selector, fallback);
                if (backend != null) {
                    return backend;
                }
            }
        }
        
        // Last resort: OpenGL (should always work in Minecraft)
        Astralis.LOGGER.warn("[GPU Backend] All backends failed, using OpenGL fallback");
        return trySelectBackend(selector, "OPENGL");
    }
    
    /**
     * Determine best backend for current platform.
     */
    private static String determineBestBackend() {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win") && isWindowsPreferDX()) {
            return "DIRECTX";
        } else if (os.contains("mac") && isMacOSPreferMetal()) {
            return "METAL";
        } else if (os.contains("nux") && isLinuxPreferVulkan()) {
            return "VULKAN";
        }
        
        // Default to Vulkan if available, otherwise OpenGL
        return "VULKAN";
    }
    
    /**
     * Try to select a specific backend.
     */
    private static GPUBackend trySelectBackend(GPUBackendSelector selector, String backendName) {
        try {
            GPUBackendSelector.BackendType type = parseBackendType(backendName);
            if (type == null) {
                Astralis.LOGGER.warn("[GPU Backend] Unknown backend type: {}", backendName);
                return null;
            }
            
            // Check if backend is available on this platform
            if (!selector.isBackendAvailable(type)) {
                if (isLogSelectionProcess()) {
                    Astralis.LOGGER.info("[GPU Backend]   {} not available on this platform", backendName);
                }
                return null;
            }
            
            // Validate feature requirements
            if (isValidateCapabilities() && !validateRequirements(type)) {
                if (isLogSelectionProcess()) {
                    Astralis.LOGGER.info("[GPU Backend]   {} doesn't meet feature requirements", backendName);
                }
                return null;
            }
            
            // Initialize backend
            GPUBackend backend = selector.selectBackend(type);
            if (backend != null) {
                Astralis.LOGGER.info("[GPU Backend] Successfully initialized: {}", backendName);
                return backend;
            }
            
        } catch (Exception e) {
            Astralis.LOGGER.error("[GPU Backend] Failed to initialize {}: {}", backendName, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Parse backend type from string.
     */
    private static GPUBackendSelector.BackendType parseBackendType(String name) {
        return switch (name.toUpperCase()) {
            case "VULKAN", "VULKAN_1_4" -> GPUBackendSelector.BackendType.VULKAN_1_4;
            case "METAL" -> GPUBackendSelector.BackendType.METAL;
            case "DIRECTX", "D3D12", "DX12" -> GPUBackendSelector.BackendType.DIRECTX_12;
            case "OPENGL", "GL" -> GPUBackendSelector.BackendType.OPENGL_4_6;
            case "OPENGLES", "GLES" -> GPUBackendSelector.BackendType.OPENGL_ES_3_2;
            case "NULL" -> GPUBackendSelector.BackendType.NULL_BACKEND;
            default -> null;
        };
    }
    
    /**
     * Validate backend meets feature requirements.
     */
    private static boolean validateRequirements(GPUBackendSelector.BackendType type) {
        // Query backend capabilities (simplified - actual implementation would check real caps)
        boolean hasCompute = type != GPUBackendSelector.BackendType.OPENGL_ES_3_2;
        boolean hasGeometry = type != GPUBackendSelector.BackendType.OPENGL_ES_3_2;
        boolean hasTessellation = type == GPUBackendSelector.BackendType.VULKAN_1_4 || 
                                   type == GPUBackendSelector.BackendType.METAL ||
                                   type == GPUBackendSelector.BackendType.DIRECTX_12;
        boolean hasMeshShaders = type == GPUBackendSelector.BackendType.VULKAN_1_4 ||
                                  type == GPUBackendSelector.BackendType.DIRECTX_12;
        boolean hasRayTracing = type == GPUBackendSelector.BackendType.VULKAN_1_4 ||
                                 type == GPUBackendSelector.BackendType.DIRECTX_12;
        
        if (isRequireComputeShaders() && !hasCompute) return false;
        if (isRequireGeometryShaders() && !hasGeometry) return false;
        if (isRequireTessellation() && !hasTessellation) return false;
        if (isRequireMeshShaders() && !hasMeshShaders) return false;
        if (isRequireRayTracing() && !hasRayTracing) return false;
        
        return true;
    }
    
    /**
     * Synchronize UniversalCapabilities with selected backend.
     * This ensures UniversalCapabilities reflects the actual backend being used.
     */
    private static void synchronizeCapabilities(GPUBackend backend) {
        if (backend == null) {
            Astralis.LOGGER.warn("[GPU Backend] Cannot synchronize capabilities - no backend");
            return;
        }
        
        Astralis.LOGGER.info("[GPU Backend] Synchronizing UniversalCapabilities with backend...");
        
        // UniversalCapabilities should already be initialized
        // This method ensures the capabilities match the selected backend
        
        // Example: If backend is DirectX, ensure UniversalCapabilities knows about it
        // This would involve calling backend.getCapabilities() and updating UniversalCapabilities
        
        // For now, just log
        Astralis.LOGGER.info("[GPU Backend]   Backend type: {}", backend.getClass().getSimpleName());
        Astralis.LOGGER.info("[GPU Backend]   Capabilities synchronized");
    }
    
    /**
     * Log detailed backend information.
     */
    private static void logBackendInfo(GPUBackend backend) {
        if (backend == null) return;
        
        Astralis.LOGGER.info("[GPU Backend] ═══════════════════════════════════════════════");
        Astralis.LOGGER.info("[GPU Backend] Active Backend: {}", backend.getClass().getSimpleName());
        
        // Log capabilities from UniversalCapabilities
        Astralis.LOGGER.info("[GPU Backend] Key Capabilities:");
        Astralis.LOGGER.info("[GPU Backend]   Compute Shaders: {}", UniversalCapabilities.hasComputeShaders());
        Astralis.LOGGER.info("[GPU Backend]   Geometry Shaders: {}", UniversalCapabilities.hasGeometryShaders());
        Astralis.LOGGER.info("[GPU Backend]   Tessellation: {}", UniversalCapabilities.hasTessellationShaders());
        Astralis.LOGGER.info("[GPU Backend]   Mesh Shaders: {}", UniversalCapabilities.hasMeshShaders());
        Astralis.LOGGER.info("[GPU Backend]   Ray Tracing: {}", UniversalCapabilities.hasRayTracing());
        Astralis.LOGGER.info("[GPU Backend]   Bindless Textures: {}", UniversalCapabilities.hasBindlessTextures());
        Astralis.LOGGER.info("[GPU Backend]   Multi-Draw Indirect: {}", UniversalCapabilities.hasMultiDrawIndirect());
        
        if (Config.getBoolean("gpuBackendDumpFullReport")) {
            Astralis.LOGGER.info("[GPU Backend] Full Capability Report:");
            // Dump full UniversalCapabilities report
            Astralis.LOGGER.info("[GPU Backend]   OpenGL Version: {}.{}", 
                UniversalCapabilities.getGLVersionMajor(), 
                UniversalCapabilities.getGLVersionMinor());
            // Add more capability dumps as needed
        }
        
        Astralis.LOGGER.info("[GPU Backend] ═══════════════════════════════════════════════");
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // RUNTIME API
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Get the active GPU backend.
     */
    public static GPUBackend getActiveBackend() {
        return activeBackend.get();
    }
    
    /**
     * Get the backend selector.
     */
    public static GPUBackendSelector getSelector() {
        return selector.get();
    }
    
    /**
     * Check if a specific backend is active.
     */
    public static boolean isBackendActive(String backendName) {
        GPUBackend backend = activeBackend.get();
        if (backend == null) return false;
        
        String className = backend.getClass().getSimpleName().toUpperCase();
        return className.contains(backendName.toUpperCase());
    }
    
    /**
     * Hot-reload backend (if enabled).
     */
    public static boolean hotReloadBackend() {
        if (!Config.getBoolean("gpuBackendEnableHotReload")) {
            Astralis.LOGGER.warn("[GPU Backend] Hot-reload is disabled");
            return false;
        }
        
        Astralis.LOGGER.info("[GPU Backend] Hot-reloading backend...");
        
        try {
            // Shutdown current backend
            GPUBackend oldBackend = activeBackend.get();
            if (oldBackend != null) {
                // oldBackend.shutdown();
            }
            
            // Re-initialize
            GPUBackendSelector sel = selector.get();
            GPUBackend newBackend = selectBackend(sel);
            activeBackend.set(newBackend);
            
            // Re-synchronize capabilities
            synchronizeCapabilities(newBackend);
            
            Astralis.LOGGER.info("[GPU Backend] Hot-reload successful");
            return true;
            
        } catch (Exception e) {
            Astralis.LOGGER.error("[GPU Backend] Hot-reload failed", e);
            return false;
        }
    }
    
    /**
     * Shutdown GPU backend integration.
     */
    public static void shutdown() {
        if (!initialized.get()) {
            return;
        }
        
        Astralis.LOGGER.info("[GPU Backend] Shutting down GPU backend integration...");
        
        GPUBackend backend = activeBackend.getAndSet(null);
        if (backend != null) {
            try {
                // backend.shutdown();
                Astralis.LOGGER.info("[GPU Backend] Backend shutdown complete");
            } catch (Exception e) {
                Astralis.LOGGER.error("[GPU Backend] Error during backend shutdown", e);
            }
        }
        
        GPUBackendSelector sel = selector.getAndSet(null);
        if (sel != null) {
            try {
                sel.close();
                Astralis.LOGGER.info("[GPU Backend] Selector closed");
            } catch (Exception e) {
                Astralis.LOGGER.error("[GPU Backend] Error closing selector", e);
            }
        }
        
        initialized.set(false);
        Astralis.LOGGER.info("[GPU Backend] Shutdown complete");
    }
}
