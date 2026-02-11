package com.llamalad7.mixinextras.expression.impl.flow;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import java.util.function.Function;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

public class ComputedFlowValue
extends FlowValue {
    private final int size;
    private final Function<FlowValue[], Type> computer;

    public ComputedFlowValue(int size, Function<FlowValue[], Type> computer, AbstractInsnNode insn, FlowValue ... parents) {
        super(null, insn, parents);
        this.size = size;
        this.computer = computer;
    }

    @Override
    public int getSize() {
        return this.size;
    }

    @Override
    public Type getType() {
        return this.computer.apply(this.parents);
    }
}

