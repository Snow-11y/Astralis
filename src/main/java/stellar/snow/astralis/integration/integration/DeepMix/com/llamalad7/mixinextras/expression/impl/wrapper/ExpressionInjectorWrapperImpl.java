package com.llamalad7.mixinextras.expression.impl.wrapper;

import com.llamalad7.mixinextras.expression.impl.point.ExpressionInjectionPoint;
import com.llamalad7.mixinextras.wrapper.InjectorWrapperImpl;
import com.llamalad7.mixinextras.wrapper.WrapperInjectionInfo;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;

public class ExpressionInjectorWrapperImpl
extends InjectorWrapperImpl {
    private final MixinTargetContext targetContext;
    private final InjectionInfo delegate;
    private final MethodNode handler;

    protected ExpressionInjectorWrapperImpl(InjectionInfo wrapper, MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(wrapper, mixin, method, annotation, false);
        this.targetContext = mixin;
        method.visibleAnnotations.remove(annotation);
        method.visibleAnnotations.add((AnnotationNode)Annotations.getValue(annotation, "original"));
        this.handler = method;
        this.delegate = InjectionInfo.parse(mixin, method);
    }

    @Override
    protected InjectionInfo getDelegate() {
        return this.delegate;
    }

    @Override
    protected MethodNode getHandler() {
        return this.handler;
    }

    @Override
    protected void prepare() {
        InjectionInfo inner = this.delegate;
        while (inner instanceof WrapperInjectionInfo) {
            inner = ((WrapperInjectionInfo)inner).getDelegate();
        }
        ExpressionInjectionPoint.withContext(inner, () -> super.prepare());
    }
}

