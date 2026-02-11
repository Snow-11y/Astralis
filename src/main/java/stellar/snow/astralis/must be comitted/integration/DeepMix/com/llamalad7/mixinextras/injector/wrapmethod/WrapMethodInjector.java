package com.llamalad7.mixinextras.injector.wrapmethod;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethodApplicatorExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.sugar.impl.ShareInfo;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Annotations;

public class WrapMethodInjector
extends Injector {
    private final Type operationType;
    private final List<ShareInfo> shares;

    public WrapMethodInjector(InjectionInfo info) {
        super(info, "@WrapMethod");
        this.operationType = MixinExtrasService.getInstance().changePackage(Operation.class, Type.getType((String)CompatibilityHelper.getAnnotation((InjectionInfo)this.info).desc), WrapMethod.class);
        this.shares = new ArrayList<ShareInfo>();
    }

    @Override
    protected void inject(Target target, InjectionNodes.InjectionNode node) {
        this.checkTargetModifiers(target, true);
        this.checkSignature(target);
        this.info.addCallbackInvocation(this.methodNode);
        WrapMethodApplicatorExtension.offerWrapper(target, this.methodNode, this.operationType, this.shares);
    }

    private void checkSignature(Target target) {
        int argIndex;
        Injector.InjectorData handler = new Injector.InjectorData(target, "method wrapper");
        String description = String.format("%s %s %s from %s", this.annotationType, handler, this, CompatibilityHelper.getMixin(this.info));
        if (target.method.name.endsWith("init>")) {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info, String.format("%s tried to target %s but targeting initializer methods is not supported!", description, target));
        }
        handler.coerceReturnType = this.checkCoerce(-1, target.returnType, description, true);
        for (argIndex = 0; argIndex < target.arguments.length; ++argIndex) {
            Type theirType = target.arguments[argIndex];
            if (argIndex >= this.methodArgs.length) {
                throw CompatibilityHelper.makeInvalidInjectionException(this.info, String.format("%s targeting %s doesn't have enough parameters!", description, target));
            }
            this.checkCoerce(argIndex, theirType, description, true);
        }
        if (argIndex >= this.methodArgs.length || !this.methodArgs[argIndex++].equals((Object)this.operationType)) {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info, String.format("%s targeting %s is missing Operation parameter!", description, target));
        }
        List sugars = (List)Annotations.getValue(CompatibilityHelper.getAnnotation(this.info), "sugars");
        if (sugars != null) {
            for (int i = 0; i < argIndex; ++i) {
                AnnotationNode sugar = (AnnotationNode)sugars.get(i);
                if (!MixinExtrasService.getInstance().isClassOwned(Type.getType((String)sugar.desc).getClassName())) continue;
                throw CompatibilityHelper.makeInvalidInjectionException(this.info, String.format("%s targeting %s has sugar on a non-trailing param which is not allowed!", description, target));
            }
        }
        while (argIndex < this.methodArgs.length) {
            ShareInfo share;
            if (sugars == null || (share = ShareInfo.getOrCreate(target, (AnnotationNode)sugars.get(argIndex), this.methodArgs[argIndex], CompatibilityHelper.getMixin(this.info).getMixin(), null)) == null) {
                throw CompatibilityHelper.makeInvalidInjectionException(this.info, String.format("%s targeting %s has an excess parameter at index %s!", description, target, argIndex));
            }
            this.shares.add(share);
            ++argIndex;
        }
    }
}

