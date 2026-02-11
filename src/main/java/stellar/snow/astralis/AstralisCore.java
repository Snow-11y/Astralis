package stellar.snow.astralis;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import stellar.snow.astralis.integration.DeepMix.DeepMix;
import stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Astralis coremod entry point.
 *
 * <p>Implements {@link DeepMix.IEarlyMixinLoader} so DeepMix can register
 * {@code mixins.astralis.json} during the coremod phase — before any game
 * classes are loaded. This is required because Astralis hooks into
 * {@code GlStateManager}, {@code Tessellator}, and {@code Minecraft#runGameLoop},
 * all of which are loaded very early. A late-loader would miss the window.</p>
 *
 * <p>Sorting index 1001 puts Astralis above DeepMix (Integer.MIN_VALUE) and
 * just above shader packs (1000), ensuring our mixins run first.</p>
 */
@IFMLLoadingPlugin.Name("AstralisCore")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
public class AstralisCore implements IFMLLoadingPlugin, DeepMix.IEarlyMixinLoader {

    // ─── IFMLLoadingPlugin ───────────────────────────────────────────────────────────

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
        // Access MC version, runtimeDeobf flag, etc. here if needed later
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    // ─── DeepMix.IEarlyMixinLoader ───────────────────────────────────────────────────

    /**
     * Tells DeepMix which mixin configs to queue during the coremod (early) phase.
     *
     * <p>We only ship one config. DeepMix calls
     * {@link #shouldMixinConfigQueue(DeepMix.Context)} per entry to allow
     * conditional gating — we always pass through here and let
     * {@code AstralisMixinPlugin#shouldApplyMixin} handle per-class decisions.</p>
     */
    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.astralis.json");
    }

    /**
     * Always queue our config — Astralis hooks are unconditionally required.
     *
     * <p>Mod-presence-based conditional loading (e.g. OptiFine compat mixins)
     * is handled inside {@code AstralisMixinPlugin}, not here.</p>
     */
    @Override
    public boolean shouldMixinConfigQueue(DeepMix.Context context) {
        return true;
    }

    /**
     * Confirmation hook — DeepMix has accepted our config into the load queue.
     */
    @Override
    public void onMixinConfigQueued(DeepMix.Context context) {
        System.out.println("[AstralisCore/DeepMix] Queued: "
                + context.mixinConfig()
                + " | dev=" + context.inDev());

        // Wait for Mini_DirtyRoom bootstrap before mixins fire.
        // MDR must finish LWJGL override + transformer registration first —
        // our GL hooks need the redirect table populated before the first
        // GlStateManager class load, or the intercept window is missed.
        boolean mdrReady = Mini_DirtyRoomCore.awaitBootstrap(10_000L);
        if (!mdrReady) {
            System.err.println("[AstralisCore/MDR] Warning: Mini_DirtyRoom bootstrap "
                    + "did not complete within 10 s — proceeding anyway.");
        } else {
            System.out.println("[AstralisCore/MDR] Mini_DirtyRoom ready. "
                    + "LWJGL override: active | redirect rules: "
                    + Mini_DirtyRoomCore.getClassRedirects().size());
        }
    }
}
