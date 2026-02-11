package com.llamalad7.mixinextras.expression.impl.flow.expansion;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.expansion.InsnExpander;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.FlowPostProcessor;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;

public class IincExpander
extends InsnExpander {
    @Override
    public void process(FlowValue node, FlowPostProcessor.OutputSink sink) {
        if (node.getInsn().getOpcode() != 132) {
            return;
        }
        IincInsnNode iinc = (IincInsnNode)node.getInsn();
        FlowValue load = new FlowValue(Type.INT_TYPE, (AbstractInsnNode)new VarInsnNode(21, iinc.var), new FlowValue[0]);
        this.registerComponent(load, Component.LOAD, (AbstractInsnNode)iinc);
        FlowValue cst = new FlowValue(Type.INT_TYPE, ExpressionASMUtils.pushInt(iinc.incr), new FlowValue[0]);
        this.registerComponent(cst, Component.CST, (AbstractInsnNode)iinc);
        FlowValue add = new FlowValue(Type.INT_TYPE, (AbstractInsnNode)new InsnNode(96), load, cst);
        this.registerComponent(add, Component.ADD, (AbstractInsnNode)iinc);
        node.setInsn((AbstractInsnNode)new VarInsnNode(54, iinc.var));
        node.setParents(add);
        this.registerComponent(node, Component.STORE, (AbstractInsnNode)iinc);
        sink.registerFlow(load, cst, add);
    }

    @Override
    public void expand(Target target, InjectionNodes.InjectionNode node, InsnExpander.Expansion expansion) {
        IincInsnNode iinc = (IincInsnNode)node.getCurrentTarget();
        target.method.maxStack += 2;
        this.expandInsn(target, node, expansion.registerInsn(Component.LOAD, (AbstractInsnNode)new VarInsnNode(21, iinc.var)), expansion.registerInsn(Component.CST, ExpressionASMUtils.pushInt(iinc.incr)), expansion.registerInsn(Component.ADD, (AbstractInsnNode)new InsnNode(96)), expansion.registerInsn(Component.STORE, (AbstractInsnNode)new VarInsnNode(54, iinc.var)));
    }

    private static enum Component implements InsnExpander.InsnComponent
    {
        LOAD,
        CST,
        ADD,
        STORE;

    }
}

