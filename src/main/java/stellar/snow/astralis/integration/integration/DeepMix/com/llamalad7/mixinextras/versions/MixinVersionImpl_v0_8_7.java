package com.llamalad7.mixinextras.versions;

import com.llamalad7.mixinextras.versions.MixinVersionImpl_v0_8_6;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;

public class MixinVersionImpl_v0_8_7
extends MixinVersionImpl_v0_8_6 {
    @Override
    public int getOrder(InjectionInfo info) {
        return info.getOrder();
    }
}

