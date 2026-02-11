package com.llamalad7.mixinextras.expression.impl.utils;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.utils.InsnReference;
import java.util.function.BiConsumer;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.spongepowered.asm.mixin.injection.struct.Target;

public class ComparisonInfo {
    protected final int comparison;
    protected final InsnReference node;
    public final Type input;
    public final boolean jumpOnTrue;

    public ComparisonInfo(int comparison, FlowValue node, Type input, boolean jumpOnTrue) {
        this.comparison = comparison;
        this.node = new InsnReference(node);
        this.input = input;
        this.jumpOnTrue = jumpOnTrue;
    }

    public void attach(BiConsumer<String, Object> decorate, BiConsumer<String, Object> decorateInjectorSpecific) {
        decorateInjectorSpecific.accept("mixinextras_comparisonInfo", this);
        decorate.accept("mixinextras_simpleOperationArgs", new Type[]{this.input, this.input});
        decorate.accept("mixinextras_simpleOperationReturnType", Type.BOOLEAN_TYPE);
        decorate.accept("mixinextras_simpleOperationParamNames", new String[]{"left", "right"});
    }

    public int copyJump(InsnList insns) {
        return this.comparison;
    }

    public LabelNode getJumpTarget(Target target) {
        return this.getJumpInsn((Target)target).label;
    }

    public JumpInsnNode getJumpInsn(Target target) {
        return (JumpInsnNode)this.node.getNode(target).getCurrentTarget();
    }

    public void cleanup(Target target) {
    }
}

