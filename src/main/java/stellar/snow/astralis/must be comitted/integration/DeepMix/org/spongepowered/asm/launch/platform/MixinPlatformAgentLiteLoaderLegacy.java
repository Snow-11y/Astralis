package org.spongepowered.asm.launch.platform;

import java.util.Collection;
import net.minecraft.launchwrapper.Launch;
import org.spongepowered.asm.launch.platform.IMixinPlatformAgent;
import org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent;
import org.spongepowered.asm.launch.platform.MixinPlatformAgentAbstract;
import org.spongepowered.asm.launch.platform.MixinPlatformManager;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;

public class MixinPlatformAgentLiteLoaderLegacy
extends MixinPlatformAgentAbstract
implements IMixinPlatformServiceAgent {
    @Override
    public IMixinPlatformAgent.AcceptResult accept(MixinPlatformManager manager, IContainerHandle handle) {
        return IMixinPlatformAgent.AcceptResult.REJECTED;
    }

    @Override
    public String getSideName() {
        return MixinPlatformAgentAbstract.invokeStringMethod((ClassLoader)Launch.classLoader, "com.mumfrey.liteloader.launch.LiteLoaderTweaker", "getEnvironmentType");
    }

    @Override
    public void init() {
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return null;
    }
}

