package org.spongepowered.asm.launch.platform;

import org.spongepowered.asm.launch.platform.MixinPlatformManager;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;

public interface IMixinPlatformAgent {
    public AcceptResult accept(MixinPlatformManager var1, IContainerHandle var2);

    public String getPhaseProvider();

    public void prepare();

    public void initPrimaryContainer();

    public void inject();

    public static enum AcceptResult {
        ACCEPTED,
        REJECTED,
        INVALID;

    }
}

