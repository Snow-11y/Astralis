package stellar.snow.astralis;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.Name("AstralisCore")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class AstralisCore implements IFMLLoadingPlugin, IEarlyMixinLoader {
    
    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.astralis.json");
    }
    
    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }
    
    @Override
    public String getModContainerClass() {
        return null;
    }
    
    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }
    
    @Override
    public void injectData(Map<String, Object> data) {
        // Can access MC version, runtimeDeobf flag, etc. if needed later
    }
    
    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// IN AstralisCore.java - Main mod initialization
// ═══════════════════════════════════════════════════════════════════════════

@Mod.EventHandler
public void init(FMLInitializationEvent event) {
    // Apply all patches automatically
    UniversalPatcher.applyAllPatches();
}

// ═══════════════════════════════════════════════════════════════════════════
// IN MixinMinecraft.java - When window becomes available
// ═══════════════════════════════════════════════════════════════════════════

@Inject(method = "startGame", at = @At("TAIL"))
private void onGameStart(CallbackInfo ci) {
    long handle = UniversalPatcher.captureWindowHandle();
    UniversalPatcher.applyDeferredPatches(handle);
}

// ═══════════════════════════════════════════════════════════════════════════
// IN ShaderPermutationManager.java - Loading shaders
// ═══════════════════════════════════════════════════════════════════════════

public void loadShaders() {
    // OLD (broken in JAR):
    // String source = Files.readString(Path.of("shaders/terrain.vert"));
    
    // NEW (works everywhere):
    String source = UniversalPatcher.loadShader("terrain.vert");
    byte[] spirv = UniversalPatcher.loadShaderBinary("terrain.vert.spv");
}
