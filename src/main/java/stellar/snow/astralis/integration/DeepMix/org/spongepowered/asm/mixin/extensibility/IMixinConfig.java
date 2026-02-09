package org.spongepowered.asm.mixin.extensibility;

import java.util.Set;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;

public interface IMixinConfig {
    public static final int DEFAULT_PRIORITY = 1000;

    public MixinEnvironment getEnvironment();

    public String getName();

    public IMixinConfigSource getSource();

    public String getCleanSourceId();

    public String getMixinPackage();

    public int getPriority();

    public IMixinConfigPlugin getPlugin();

    public boolean isRequired();

    public Set<String> getTargets();

    public <V> void decorate(String var1, V var2);

    public boolean hasDecoration(String var1);

    public <V> V getDecoration(String var1);
}

