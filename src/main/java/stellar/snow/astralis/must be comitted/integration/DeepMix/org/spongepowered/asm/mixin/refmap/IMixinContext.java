package org.spongepowered.asm.mixin.refmap;

import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;

public interface IMixinContext {
    public IMixinInfo getMixin();

    public Extensions getExtensions();

    public String getClassName();

    public String getClassRef();

    public String getTargetClassName();

    public String getTargetClassRef();

    public IReferenceMapper getReferenceMapper();

    public boolean getOption(MixinEnvironment.Option var1);

    public int getPriority();
}

