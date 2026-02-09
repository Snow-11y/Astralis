package org.spongepowered.asm.mixin;

import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;

public final class ModUtil {
    public static final String OWNER_DECORATOR = "mixinOwner";
    public static final String UNKNOWN_OWNER = "unknown-owner";

    public static String owner(IMixinConfig config) {
        return ModUtil.owner(config, UNKNOWN_OWNER);
    }

    public static String owner(IMixinConfig config, String defaultValue) {
        return FabricUtil.getDecoration(config, OWNER_DECORATOR, defaultValue);
    }

    public static String owner(ISelectorContext context) {
        return FabricUtil.getDecoration(FabricUtil.getConfig(context), OWNER_DECORATOR, UNKNOWN_OWNER);
    }

    private ModUtil() {
    }
}

