package com.llamalad7.mixinextras.wrapper.factory;

import com.llamalad7.mixinextras.wrapper.WrapperInjectionInfo;
import com.llamalad7.mixinextras.wrapper.factory.FactoryRedirectWrapper;
import com.llamalad7.mixinextras.wrapper.factory.FactoryRedirectWrapperImpl;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

@InjectionInfo.AnnotationType(value=FactoryRedirectWrapper.class)
@InjectionInfo.HandlerPrefix(value="factoryWrapper")
public class FactoryRedirectWrapperInjectionInfo
extends WrapperInjectionInfo {
    public FactoryRedirectWrapperInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(FactoryRedirectWrapperImpl::new, mixin, method, annotation);
    }
}

