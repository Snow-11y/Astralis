package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.injector.StackExtension;
import com.llamalad7.mixinextras.sugar.impl.SugarApplicator;
import com.llamalad7.mixinextras.sugar.impl.SugarParameter;
import com.llamalad7.mixinextras.sugar.impl.SugarPostProcessingExtension;
import com.llamalad7.mixinextras.utils.ASMUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;

class CancellableSugarApplicator
extends SugarApplicator {
    CancellableSugarApplicator(InjectionInfo info, SugarParameter parameter) {
        super(info, parameter);
    }

    @Override
    void validate(Target target, InjectionNodes.InjectionNode node) {
    }

    @Override
    void prepare(Target target, InjectionNodes.InjectionNode node) {
    }

    @Override
    void inject(Target target, InjectionNodes.InjectionNode node, StackExtension stack) {
        Type ciType = Type.getObjectType((String)target.getCallbackInfoClass());
        if (!ciType.equals((Object)this.paramType)) {
            throw new IllegalStateException(String.format("@Cancellable sugar has wrong type! Expected %s but got %s!", ciType.getClassName(), this.paramType.getClassName()));
        }
        int ciIndex = this.getOrCreateCi(target, node, stack, ciType);
        stack.extra(1);
        target.insns.insertBefore(node.getCurrentTarget(), (AbstractInsnNode)new VarInsnNode(25, ciIndex));
    }

    @Override
    int postProcessingPriority() {
        return -1000;
    }

    private int getOrCreateCi(Target target, InjectionNodes.InjectionNode node, StackExtension stack, Type ciType) {
        if (node.hasDecoration("mixinextras_cancellableCiIndex")) {
            return (Integer)node.getDecoration("mixinextras_cancellableCiIndex");
        }
        int ciIndex = target.allocateLocal();
        target.addLocalVariable(ciIndex, "callbackInfo" + ciIndex, ciType.getDescriptor());
        node.decorate("mixinextras_cancellableCiIndex", ciIndex);
        InsnList init = new InsnList();
        init.add((AbstractInsnNode)new TypeInsnNode(187, ciType.getInternalName()));
        init.add((AbstractInsnNode)new InsnNode(89));
        init.add((AbstractInsnNode)new LdcInsnNode((Object)target.method.name));
        init.add((AbstractInsnNode)new InsnNode(4));
        init.add((AbstractInsnNode)new MethodInsnNode(183, ciType.getInternalName(), "<init>", "(Ljava/lang/String;Z)V", false));
        init.add((AbstractInsnNode)new VarInsnNode(58, ciIndex));
        target.insertBefore(node, init);
        stack.extra(4);
        SugarPostProcessingExtension.enqueuePostProcessing(this, () -> {
            InsnList cancellation = new InsnList();
            LabelNode notCancelled = new LabelNode();
            cancellation.add((AbstractInsnNode)new VarInsnNode(25, ciIndex));
            cancellation.add((AbstractInsnNode)new MethodInsnNode(182, ciType.getInternalName(), "isCancelled", "()Z", false));
            cancellation.add((AbstractInsnNode)new JumpInsnNode(153, notCancelled));
            cancellation.add((AbstractInsnNode)new VarInsnNode(25, ciIndex));
            if (target.returnType.equals((Object)Type.VOID_TYPE)) {
                cancellation.add((AbstractInsnNode)new InsnNode(177));
            } else if (ASMUtils.isPrimitive(target.returnType)) {
                cancellation.add((AbstractInsnNode)new MethodInsnNode(182, ciType.getInternalName(), "getReturnValue" + target.returnType.getDescriptor(), "()" + target.returnType.getDescriptor(), false));
                cancellation.add((AbstractInsnNode)new InsnNode(target.returnType.getOpcode(172)));
            } else {
                cancellation.add((AbstractInsnNode)new MethodInsnNode(182, ciType.getInternalName(), "getReturnValue", "()Ljava/lang/Object;", false));
                cancellation.add((AbstractInsnNode)new TypeInsnNode(192, target.returnType.getInternalName()));
                cancellation.add((AbstractInsnNode)new InsnNode(176));
            }
            cancellation.add((AbstractInsnNode)notCancelled);
            target.insns.insert(node.getCurrentTarget(), cancellation);
        });
        return ciIndex;
    }
}

