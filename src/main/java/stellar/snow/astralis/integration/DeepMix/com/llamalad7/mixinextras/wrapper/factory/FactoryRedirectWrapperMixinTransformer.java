package com.llamalad7.mixinextras.wrapper.factory;

import com.llamalad7.mixinextras.transformer.MixinTransformer;
import com.llamalad7.mixinextras.wrapper.factory.FactoryRedirectWrapper;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.util.Annotations;

public class FactoryRedirectWrapperMixinTransformer
implements MixinTransformer {
    @Override
    public void transform(IMixinInfo mixinInfo, ClassNode mixinNode) {
        for (MethodNode method : mixinNode.methods) {
            String value;
            AnnotationNode at;
            AnnotationNode redirect = Annotations.getVisible(method, Redirect.class);
            if (redirect == null || (at = (AnnotationNode)Annotations.getValue(redirect, "at")) == null || !"NEW".equals(value = (String)Annotations.getValue(at))) continue;
            this.wrapInjectorAnnotation(method, redirect);
        }
    }

    private void wrapInjectorAnnotation(MethodNode method, AnnotationNode redirect) {
        AnnotationNode wrapped = new AnnotationNode(Type.getDescriptor(FactoryRedirectWrapper.class));
        wrapped.visit("original", (Object)redirect);
        method.visibleAnnotations.remove(redirect);
        method.visibleAnnotations.add(wrapped);
    }
}

