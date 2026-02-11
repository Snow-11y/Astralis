package com.llamalad7.mixinextras.expression.impl.wrapper;

import com.llamalad7.mixinextras.expression.impl.wrapper.ExpressionInjectorWrapper;
import com.llamalad7.mixinextras.expression.impl.wrapper.ExpressionInjectorWrapperImpl;
import com.llamalad7.mixinextras.wrapper.WrapperInjectionInfo;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

@InjectionInfo.AnnotationType(value=ExpressionInjectorWrapper.class)
@InjectionInfo.HandlerPrefix(value="expressionWrapper")
public class ExpressionInjectorWrapperInjectionInfo
extends WrapperInjectionInfo {
    public ExpressionInjectorWrapperInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(ExpressionInjectorWrapperImpl::new, mixin, method, annotation);
    }
}

