package com.llamalad7.mixinextras.wrapper.factory;

import com.llamalad7.mixinextras.utils.MixinInternals;
import com.llamalad7.mixinextras.wrapper.InjectorWrapperImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;

public class FactoryRedirectWrapperImpl
extends InjectorWrapperImpl {
    private final InjectionInfo delegate;
    private final MethodNode handler;

    protected FactoryRedirectWrapperImpl(InjectionInfo wrapper, MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(wrapper, mixin, method, annotation, true);
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
    protected void granularInject(InjectorWrapperImpl.HandlerCallCallback callback) {
        HashMap replacements = new HashMap();
        for (Map.Entry<Target, List<InjectionNodes.InjectionNode>> entry : MixinInternals.getTargets(this.delegate).entrySet()) {
            for (InjectionNodes.InjectionNode source : entry.getValue()) {
                this.findReplacedNodes(entry.getKey(), source, it -> replacements.computeIfAbsent(source, k -> new ArrayList()).add(it));
            }
        }
        super.granularInject((target, sourceNode, call) -> {
            callback.onFound(target, sourceNode, call);
            List replacedNodes = (List)replacements.get(sourceNode);
            if (replacedNodes == null) {
                return;
            }
            for (InjectionNodes.InjectionNode replaced : replacedNodes) {
                replaced.replace((AbstractInsnNode)call);
            }
        });
    }

    private void findReplacedNodes(Target target, InjectionNodes.InjectionNode source, Consumer<InjectionNodes.InjectionNode> sink) {
        if (source.isRemoved() || source.getCurrentTarget().getOpcode() != 187) {
            return;
        }
        sink.accept(source);
        sink.accept(target.addInjectionNode((AbstractInsnNode)target.findInitNodeFor((TypeInsnNode)source.getCurrentTarget())));
    }
}

