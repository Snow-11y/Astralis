package com.llamalad7.mixinextras.expression.impl.flow.expansion;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.expansion.InsnExpander;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.FlowPostProcessor;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;

public class UnaryComparisonExpander
extends InsnExpander {
    @Override
    public void process(FlowValue node, FlowPostProcessor.OutputSink sink) {
        AbstractInsnNode insn = node.getInsn();
        int cstOpcode = this.getCstOpcode(insn);
        if (cstOpcode == -1) {
            return;
        }
        JumpInsnNode jump = (JumpInsnNode)insn;
        int jumpOpcode = this.getExpandedJumpOpcode(insn);
        if (this.isComplexComparison(node.getInput(0))) {
            node.getInput(0).decorate("complexComparisonJump", node);
            sink.markAsSynthetic(node);
            return;
        }
        InsnNode cstInsn = new InsnNode(cstOpcode);
        FlowValue cst = new FlowValue(ExpressionASMUtils.getNewType((AbstractInsnNode)cstInsn), (AbstractInsnNode)cstInsn, new FlowValue[0]);
        this.registerComponent(cst, Component.CST, (AbstractInsnNode)jump);
        node.setInsn((AbstractInsnNode)new JumpInsnNode(jumpOpcode, jump.label));
        node.setParents(node.getInput(0), cst);
        this.registerComponent(node, Component.JUMP, (AbstractInsnNode)jump);
        sink.registerFlow(cst);
    }

    @Override
    public void expand(Target target, InjectionNodes.InjectionNode node, InsnExpander.Expansion expansion) {
        if (node.isReplaced()) {
            AbstractInsnNode next = node.getCurrentTarget().getNext();
            if (!(next instanceof JumpInsnNode)) {
                throw new IllegalStateException("Could not find jump for expanded @ModifyConstant comparison! Please inform LlamaLad7!");
            }
            JumpInsnNode jump = (JumpInsnNode)next;
            expansion.registerInsn(Component.CST, node.getCurrentTarget());
            expansion.registerInsn(Component.JUMP, (AbstractInsnNode)jump);
            return;
        }
        AbstractInsnNode insn = node.getCurrentTarget();
        int cstOpcode = this.getCstOpcode(insn);
        if (cstOpcode == -1) {
            return;
        }
        JumpInsnNode jump = (JumpInsnNode)insn;
        int jumpOpcode = this.getExpandedJumpOpcode(insn);
        ++target.method.maxStack;
        this.expandInsn(target, node, expansion.registerInsn(Component.CST, (AbstractInsnNode)new InsnNode(cstOpcode)), expansion.registerInsn(Component.JUMP, (AbstractInsnNode)new JumpInsnNode(jumpOpcode, jump.label)));
    }

    private int getCstOpcode(AbstractInsnNode insn) {
        if (153 <= insn.getOpcode() && insn.getOpcode() <= 158) {
            return 3;
        }
        if (insn.getOpcode() == 198 || insn.getOpcode() == 199) {
            return 1;
        }
        return -1;
    }

    private int getExpandedJumpOpcode(AbstractInsnNode insn) {
        if (153 <= insn.getOpcode() && insn.getOpcode() <= 158) {
            return insn.getOpcode() + 6;
        }
        if (insn.getOpcode() == 198 || insn.getOpcode() == 199) {
            return insn.getOpcode() - 33;
        }
        return -1;
    }

    private boolean isComplexComparison(FlowValue node) {
        if (node.isComplex()) {
            return false;
        }
        AbstractInsnNode insn = node.getInsn();
        return 148 <= insn.getOpcode() && insn.getOpcode() <= 152;
    }

    private static enum Component implements InsnExpander.InsnComponent
    {
        CST,
        JUMP;

    }
}

