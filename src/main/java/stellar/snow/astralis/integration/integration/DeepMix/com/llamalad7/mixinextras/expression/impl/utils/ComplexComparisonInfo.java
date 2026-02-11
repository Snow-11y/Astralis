package com.llamalad7.mixinextras.expression.impl.utils;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.utils.InsnReference;
import com.llamalad7.mixinextras.expression.impl.utils.ComparisonInfo;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.spongepowered.asm.mixin.injection.struct.Target;

public class ComplexComparisonInfo
extends ComparisonInfo {
    private final InsnReference jumpInsn;
    private final int jumpOpcode;

    public ComplexComparisonInfo(int comparison, FlowValue node, Type input, FlowValue jump, boolean jumpOnTrue) {
        super(comparison, node, input, jumpOnTrue);
        this.jumpInsn = new InsnReference(jump);
        this.jumpOpcode = jump.getInsn().getOpcode();
    }

    @Override
    public int copyJump(InsnList insns) {
        insns.add((AbstractInsnNode)new InsnNode(this.comparison));
        return this.jumpOpcode;
    }

    @Override
    public JumpInsnNode getJumpInsn(Target target) {
        return (JumpInsnNode)this.jumpInsn.getNode(target).getCurrentTarget();
    }

    @Override
    public void cleanup(Target target) {
        target.replaceNode((AbstractInsnNode)this.getJumpInsn(target), (AbstractInsnNode)new InsnNode(0));
    }
}

