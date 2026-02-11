package org.spongepowered.asm.launch.platform;

import java.util.Collection;
import org.spongepowered.asm.launch.platform.IMixinPlatformAgent;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.util.IConsumer;

public interface IMixinPlatformServiceAgent
extends IMixinPlatformAgent {
    public void init();

    public String getSideName();

    public Collection<IContainerHandle> getMixinContainers();

    @Deprecated
    public void wire(MixinEnvironment.Phase var1, IConsumer<MixinEnvironment.Phase> var2);

    @Deprecated
    public void unwire();
}

