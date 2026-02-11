package com.llamalad7.mixinextras.versions;

import com.llamalad7.mixinextras.utils.MixinInternals_v0_8_6;
import com.llamalad7.mixinextras.versions.MixinVersion;
import com.llamalad7.mixinextras.versions.MixinVersionImpl_v0_8_4;
import java.util.Collection;
import java.util.stream.Collectors;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

public class MixinVersionImpl_v0_8_6
extends MixinVersionImpl_v0_8_4 {
    @Override
    public Collection<Target> getTargets(InjectionInfo info) {
        MixinTargetContext mixin = (MixinTargetContext)MixinVersion.getInstance().getMixin(info);
        return MixinInternals_v0_8_6.getTargets(info).stream().map(mixin::getTargetMethod).collect(Collectors.toList());
    }
}

