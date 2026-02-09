package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.injector.StackExtension;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;

public class ModifyReturnValueInjector
extends Injector {
    public ModifyReturnValueInjector(InjectionInfo info) {
        super(info, "@ModifyReturnValue");
    }

    @Override
    protected void inject(Target target, InjectionNodes.InjectionNode node) {
        int opcode = node.getCurrentTarget().getOpcode();
        if (opcode < 172 || opcode >= 177) {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info, String.format("%s annotation is targeting an invalid insn in %s in %s", this.annotationType, target, this));
        }
        this.checkTargetModifiers(target, false);
        this.injectReturnValueModifier(target, node);
    }

    private void injectReturnValueModifier(Target target, InjectionNodes.InjectionNode node) {
        Injector.InjectorData handler = new Injector.InjectorData(target, "return value modifier");
        StackExtension stack = new StackExtension(target);
        InsnList insns = new InsnList();
        this.validateParams(handler, target.returnType, target.returnType);
        if (!this.isStatic) {
            insns.add((AbstractInsnNode)new VarInsnNode(25, 0));
            if (target.returnType.getSize() == 2) {
                stack.extra(1);
                insns.add((AbstractInsnNode)new InsnNode(91));
                insns.add((AbstractInsnNode)new InsnNode(87));
            } else {
                insns.add((AbstractInsnNode)new InsnNode(95));
            }
        }
        if (handler.captureTargetArgs > 0) {
            this.pushArgs(target.arguments, insns, target.getArgIndices(), 0, handler.captureTargetArgs);
        }
        stack.receiver(this.isStatic);
        stack.capturedArgs(target.arguments, handler.captureTargetArgs);
        this.invokeHandler(insns);
        target.insertBefore(node, insns);
    }
}

