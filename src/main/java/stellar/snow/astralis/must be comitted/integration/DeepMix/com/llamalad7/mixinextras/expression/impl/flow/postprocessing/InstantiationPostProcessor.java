package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.FlowPostProcessor;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.InstantiationInfo;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import java.util.stream.IntStream;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.util.Bytecode;

public class InstantiationPostProcessor
implements FlowPostProcessor {
    @Override
    public void process(FlowValue node, FlowPostProcessor.OutputSink sink) {
        AbstractInsnNode insn = node.getInsn();
        if (insn.getOpcode() != 187) {
            return;
        }
        Type newType = Type.getObjectType((String)((TypeInsnNode)insn).desc);
        FlowValue initCall = this.findInitCall(node);
        node.decorate("instantiationInfo", new InstantiationInfo(newType, initCall));
        sink.markAsSynthetic(initCall);
        node.setParents((FlowValue[])IntStream.range(1, initCall.inputCount()).mapToObj(initCall::getInput).toArray(FlowValue[]::new));
    }

    private FlowValue findInitCall(FlowValue newNode) {
        for (Pair<FlowValue, Integer> next : newNode.getNext()) {
            FlowValue nextValue;
            AbstractInsnNode nextInsn;
            if (next.getRight() != 0 || (nextInsn = (nextValue = next.getLeft()).getInsn()).getOpcode() != 183 || !((MethodInsnNode)nextInsn).name.equals("<init>") || nextValue.getInput(0) != newNode) continue;
            return nextValue;
        }
        throw new IllegalStateException("Could not find <init> call for " + Bytecode.describeNode(newNode.getInsn()));
    }
}

