package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.sugar.impl.SugarWrapper;
import com.llamalad7.mixinextras.sugar.impl.SugarWrapperImpl;
import com.llamalad7.mixinextras.wrapper.WrapperInjectionInfo;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

@InjectionInfo.AnnotationType(value=SugarWrapper.class)
@InjectionInfo.HandlerPrefix(value="sugarWrapper")
public class SugarWrapperInjectionInfo
extends WrapperInjectionInfo {
    public SugarWrapperInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(SugarWrapperImpl::new, mixin, method, annotation);
    }
}

