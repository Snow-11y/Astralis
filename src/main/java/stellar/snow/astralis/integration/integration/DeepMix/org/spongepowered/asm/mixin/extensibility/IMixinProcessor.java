package org.spongepowered.asm.mixin.extensibility;

import java.util.List;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.service.IMixinService;

public interface IMixinProcessor {
    public IMixinService getMixinService();

    public List<IMixinConfig> getMixinConfigs();

    public List<IMixinConfig> getPendingMixinConfigs();
}

