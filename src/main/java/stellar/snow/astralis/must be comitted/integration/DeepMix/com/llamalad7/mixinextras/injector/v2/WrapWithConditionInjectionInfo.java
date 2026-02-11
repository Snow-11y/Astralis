package com.llamalad7.mixinextras.injector.v2;

import com.llamalad7.mixinextras.injector.MixinExtrasLateInjectionInfo;
import com.llamalad7.mixinextras.injector.WrapWithConditionInjector;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.utils.InjectorUtils;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

@InjectionInfo.AnnotationType(value=WrapWithCondition.class)
@InjectionInfo.HandlerPrefix(value="wrapWithCondition")
public class WrapWithConditionInjectionInfo
extends MixinExtrasLateInjectionInfo {
    public WrapWithConditionInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
    }

    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        return new WrapWithConditionInjector(this);
    }

    @Override
    public void prepare() {
        super.prepare();
        InjectorUtils.checkForImmediatePops(this.targetNodes);
    }

    @Override
    public String getLateInjectionType() {
        return "WrapWithCondition";
    }
}

