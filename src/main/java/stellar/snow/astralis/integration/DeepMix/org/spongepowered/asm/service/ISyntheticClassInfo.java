package org.spongepowered.asm.service;

import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public interface ISyntheticClassInfo {
    public IMixinInfo getMixin();

    public String getName();

    public String getClassName();

    public boolean isLoaded();
}

