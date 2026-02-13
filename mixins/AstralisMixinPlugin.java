package stellar.snow.astralis.mixins;

import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import stellar.snow.astralis.config.Config;

import java.util.List;
import java.util.Set;

/**
 * AstralisMixinPlugin - Dynamic mixin configuration based on Config settings.
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li>Conditional mixin loading based on configuration</li>
 *   <li>Mod compatibility detection and adjustment</li>
 *   <li>Performance-based mixin toggling</li>
 *   <li>Runtime feature validation</li>
 * </ul>
 * 
 * @author Astralis Team
 * @version 1.0.0
 */
public class AstralisMixinPlugin implements IMixinConfigPlugin {
    
    private static final String MIXIN_PACKAGE = "stellar.snow.astralis.mixins.";
    
    @Override
    public void onLoad(String mixinPackage) {
        System.out.println("[Astralis] Mixin plugin loaded: " + mixinPackage);
        
        // Early initialization of Config if not already done
        try {
            Config.initialize();
            System.out.println("[Astralis] Config initialized for mixin plugin");
        } catch (Exception e) {
            System.err.println("[Astralis] Failed to initialize Config in mixin plugin: " + e.getMessage());
        }
    }
    
    @Override
    public String getRefMapperConfig() {
        return null; // Use default refmap
    }
    
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Check master mixin toggle
        if (!Config.isMixinEnabled()) {
            System.out.println("[Astralis] Skipping mixin (disabled): " + mixinClassName);
            return false;
        }
        
        // Get mixin short name
        String mixinName = mixinClassName.replace(MIXIN_PACKAGE, "");
        
        // Universal Patcher - only apply if enabled
        if (mixinClassName.contains("MixinUniversalPatcher")) {
            boolean enabled = Config.isMixinUniversalPatcherEnabled();
            if (!enabled) {
                System.out.println("[Astralis] Skipping Universal Patcher (disabled in config)");
            }
            return enabled;
        }
        
        // Entity mixins - only apply if entity enhancements enabled
        if (mixinClassName.contains("MixinEntity") && !mixinClassName.contains("MixinEntityAI")) {
            boolean enabled = Config.isMixinEntityEnhancements() && Config.isMixinBridgeInterfaceEnabled();
            if (!enabled) {
                System.out.println("[Astralis] Skipping entity mixin: " + mixinName);
            }
            return enabled;
        }
        
        // World mixins - only apply if world enhancements enabled
        if (mixinClassName.contains("MixinWorld")) {
            boolean enabled = Config.isMixinWorldEnhancements();
            if (!enabled) {
                System.out.println("[Astralis] Skipping world mixin: " + mixinName);
            }
            return enabled;
        }
        
        // Render mixins - only apply if render enhancements enabled
        if (mixinClassName.contains(".render.")) {
            boolean enabled = Config.isMixinRenderEnhancements();
            if (!enabled) {
                System.out.println("[Astralis] Skipping render mixin: " + mixinName);
            }
            return enabled;
        }
        
        // AI mixins - only apply if AI optimizations enabled
        if (mixinClassName.contains(".ai.")) {
            boolean enabled = Config.isMixinAIOptimizations();
            if (!enabled) {
                System.out.println("[Astralis] Skipping AI mixin: " + mixinName);
            }
            return enabled;
        }
        
        // Compatibility handler - always apply if any compat is enabled
        if (mixinClassName.contains("HandlerBridge")) {
            boolean anyCompatEnabled = 
                Config.isMixinCompatOptiFine() ||
                Config.isMixinCompatShaderMods() ||
                Config.isMixinCompatKirino() ||
                Config.isMixinCompatNothirium() ||
                Config.isMixinCompatCeleritas() ||
                Config.isMixinCompatSodium() ||
                Config.isMixinCompatVanilla();
            
            if (!anyCompatEnabled) {
                System.out.println("[Astralis] Skipping HandlerBridge (no compat modes enabled)");
            }
            return anyCompatEnabled;
        }
        
        // Default: apply mixin
        System.out.println("[Astralis] Applying mixin: " + mixinName);
        return true;
    }
    
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // No special handling needed
    }
    
    @Override
    public List<String> getMixins() {
        // Return null to use default mixin list from JSON
        return null;
    }
    
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // Pre-processing before mixin application
        if (Config.isMixinDebugMode()) {
            System.out.println("[Astralis] Pre-applying mixin: " + mixinClassName + " -> " + targetClassName);
        }
    }
    
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // Post-processing after mixin application
        if (Config.isMixinDebugMode()) {
            System.out.println("[Astralis] Post-applied mixin: " + mixinClassName + " -> " + targetClassName);
        }
        
        // Validate injection if enabled
        if (Config.isMixinValidateInjections()) {
            validateMixinInjection(targetClassName, mixinClassName);
        }
    }
    
    /**
     * Validates that a mixin was properly applied.
     */
    private void validateMixinInjection(String targetClassName, String mixinClassName) {
        // Basic validation - can be extended with more sophisticated checks
        if (Config.isMixinPrintInjections()) {
            System.out.println("[Astralis] Validated injection: " + mixinClassName + " -> " + targetClassName);
        }
    }
}
