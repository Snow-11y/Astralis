package com.llamalad7.mixinextras.injector.wrapmethod;

import com.llamalad7.mixinextras.injector.MixinExtrasInjectionInfo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethodInjectionPoint;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethodInjector;
import java.util.List;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

@InjectionInfo.AnnotationType(value=WrapMethod.class)
@InjectionInfo.HandlerPrefix(value="wrapMethod")
public class WrapMethodInjectionInfo
extends MixinExtrasInjectionInfo {
    public WrapMethodInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
    }

    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        return new WrapMethodInjector(this);
    }

    @Override
    protected void parseInjectionPoints(List<AnnotationNode> ats) {
        this.injectionPoints.add(new WrapMethodInjectionPoint());
    }
}

