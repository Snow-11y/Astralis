package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.injector.MixinExtrasInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValueInjector;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

@InjectionInfo.AnnotationType(value=ModifyReturnValue.class)
@InjectionInfo.HandlerPrefix(value="modifyReturnValue")
public class ModifyReturnValueInjectionInfo
extends MixinExtrasInjectionInfo {
    public ModifyReturnValueInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
    }

    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        return new ModifyReturnValueInjector(this);
    }
}

