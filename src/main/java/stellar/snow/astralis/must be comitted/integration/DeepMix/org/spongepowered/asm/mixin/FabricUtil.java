package org.spongepowered.asm.mixin;

import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;

public final class FabricUtil {
    public static final String KEY_COMPATIBILITY = "fabric-compat";
    public static final int COMPATIBILITY_0_9_2 = 9002;
    public static final int COMPATIBILITY_0_10_0 = 10000;
    public static final int COMPATIBILITY_0_14_0 = 14000;
    public static final int COMPATIBILITY_LATEST = 14000;

    public static int getCompatibility(ISelectorContext context) {
        return FabricUtil.getDecoration(FabricUtil.getConfig(context), KEY_COMPATIBILITY, 14000);
    }

    static IMixinConfig getConfig(ISelectorContext context) {
        return context.getMixin().getMixin().getConfig();
    }

    static <T> T getDecoration(IMixinConfig config, String key, T defaultValue) {
        if (config.hasDecoration(key)) {
            return (T)config.getDecoration(key);
        }
        return defaultValue;
    }

    private FabricUtil() {
    }
}

