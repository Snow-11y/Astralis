package com.llamalad7.mixinextras.versions;

import com.llamalad7.mixinextras.versions.MixinVersion;
import java.lang.invoke.LambdaMetafactory;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;

public class MixinVersionImpl_v0_8
extends MixinVersion {
    @Override
    public RuntimeException makeInvalidInjectionException(InjectionInfo info, String message) {
        return new InvalidInjectionException(info, message);
    }

    @Override
    public IMixinContext getMixin(InjectionInfo info) {
        return info.getContext();
    }

    @Override
    public LocalVariableDiscriminator.Context makeLvtContext(InjectionInfo info, Type returnType, boolean argsOnly, Target target, AbstractInsnNode node) {
        return new LocalVariableDiscriminator.Context(returnType, argsOnly, target, node);
    }

    @Override
    public void preInject(InjectionInfo info) {
        throw new AssertionError((Object)"Cannot preInject until 0.8.3");
    }

    @Override
    public AnnotationNode getAnnotation(InjectionInfo info) {
        return info.getAnnotation();
    }

    @Override
    public int getOrder(InjectionInfo info) {
        throw new AssertionError((Object)"Cannot getOrder until 0.8.7");
    }

    @Override
    public Collection<Target> getTargets(InjectionInfo info) {
        IMixinContext mixin = MixinVersion.getInstance().getMixin(info);
        return info.getTargets().stream().map((Function<MethodNode, Target>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Ljava/lang/Object;, getTargetMethod(org.objectweb.asm.tree.MethodNode ), (Lorg/objectweb/asm/tree/MethodNode;)Lorg/spongepowered/asm/mixin/injection/struct/Target;)((IMixinContext)mixin)).collect(Collectors.toList());
    }

    @Override
    public MemberInfo parseMemberInfo(String targetSelector, InjectionInfo info) {
        return MemberInfo.parse((String)targetSelector, (IReferenceMapper)info.getContext().getReferenceMapper(), (String)info.getContext().getClassRef());
    }
}

