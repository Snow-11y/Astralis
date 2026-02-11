package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.injector.StackExtension;
import com.llamalad7.mixinextras.lib.apache.commons.ArrayUtils;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import com.llamalad7.mixinextras.utils.InjectorUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;

public class ModifyReceiverInjector
extends Injector {
    public ModifyReceiverInjector(InjectionInfo info) {
        super(info, "@ModifyReceiver");
    }

    @Override
    protected void inject(Target target, InjectionNodes.InjectionNode node) {
        this.checkTargetIsValid(target, node);
        this.checkTargetModifiers(target, false);
        this.modifyReceiverOfTarget(target, node);
    }

    private void checkTargetIsValid(Target target, InjectionNodes.InjectionNode node) {
        AbstractInsnNode insn = node.getOriginalTarget();
        switch (insn.getOpcode()) {
            case 180: 
            case 181: 
            case 182: 
            case 183: 
            case 185: {
                return;
            }
        }
        throw CompatibilityHelper.makeInvalidInjectionException(this.info, String.format("%s annotation is targeting an invalid insn in %s in %s", this.annotationType, target, this));
    }

    private void modifyReceiverOfTarget(Target target, InjectionNodes.InjectionNode node) {
        AbstractInsnNode currentTarget = node.getCurrentTarget();
        Type[] originalArgTypes = this.getEffectiveArgTypes(node.getOriginalTarget());
        Type[] currentArgTypes = this.getEffectiveArgTypes(currentTarget);
        InsnList insns = new InsnList();
        boolean isVirtualRedirect = InjectorUtils.isVirtualRedirect(node);
        this.injectReceiverModifier(target, originalArgTypes, currentArgTypes, isVirtualRedirect, insns);
        target.insertBefore(node, insns);
    }

    private void injectReceiverModifier(Target target, Type[] originalArgTypes, Type[] currentArgTypes, boolean isVirtualRedirect, InsnList insns) {
        Injector.InjectorData handler = new Injector.InjectorData(target, "receiver modifier");
        this.validateParams(handler, originalArgTypes[0], originalArgTypes);
        StackExtension stack = new StackExtension(target);
        int[] argMap = this.storeArgs(target, currentArgTypes, insns, 0);
        int[] handlerArgMap = ArrayUtils.addAll(argMap, target.getArgIndices());
        if (isVirtualRedirect) {
            handlerArgMap = ArrayUtils.remove(handlerArgMap, 0);
            insns.add((AbstractInsnNode)new VarInsnNode(25, 0));
        }
        stack.receiver(this.isStatic);
        stack.capturedArgs(target.arguments, handler.captureTargetArgs);
        this.invokeHandlerWithArgs(this.methodArgs, insns, handlerArgMap);
        InjectorUtils.coerceReturnType(handler, insns, originalArgTypes[0]);
        this.pushArgs(currentArgTypes, insns, argMap, isVirtualRedirect ? 2 : 1, argMap.length);
    }

    private Type[] getEffectiveArgTypes(AbstractInsnNode node) {
        switch (node.getOpcode()) {
            case 182: 
            case 185: {
                MethodInsnNode methodInsnNode = (MethodInsnNode)node;
                return ArrayUtils.addAll(new Type[]{Type.getObjectType((String)methodInsnNode.owner)}, Type.getArgumentTypes((String)methodInsnNode.desc));
            }
            case 183: {
                MethodInsnNode methodInsnNode = (MethodInsnNode)node;
                return ArrayUtils.addAll(new Type[]{Type.getObjectType((String)this.classNode.name)}, Type.getArgumentTypes((String)methodInsnNode.desc));
            }
            case 184: {
                return Type.getArgumentTypes((String)((MethodInsnNode)node).desc);
            }
            case 180: {
                FieldInsnNode fieldInsnNode = (FieldInsnNode)node;
                return new Type[]{Type.getObjectType((String)fieldInsnNode.owner)};
            }
            case 181: {
                FieldInsnNode fieldInsnNode = (FieldInsnNode)node;
                return new Type[]{Type.getObjectType((String)fieldInsnNode.owner), Type.getType((String)fieldInsnNode.desc)};
            }
        }
        throw new UnsupportedOperationException();
    }
}

