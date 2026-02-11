package org.spongepowered.asm.service.mojang;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.service.IMixinServiceBootstrap;
import org.spongepowered.asm.service.ServiceInitialisationException;

public class MixinServiceLaunchWrapperBootstrap
implements IMixinServiceBootstrap {
    @Override
    public String getName() {
        return "LaunchWrapper";
    }

    @Override
    public String getServiceClassName() {
        return "org.spongepowered.asm.service.mojang.MixinServiceLaunchWrapper";
    }

    @Override
    public void bootstrap() {
        try {
            LaunchClassLoader launchClassLoader = Launch.classLoader;
        }
        catch (Throwable th) {
            throw new ServiceInitialisationException(this.getName() + " is not available");
        }
    }
}

