package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.FlowPostProcessor;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.InstantiationInfo;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.StringConcatInfo;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class StringConcatPostProcessor
implements FlowPostProcessor {
    private static final String STRING_BUILDER = Type.getInternalName(StringBuilder.class);

    @Override
    public void process(FlowValue node, FlowPostProcessor.OutputSink sink) {
        Pair<FlowValue, Integer> child;
        FlowValue firstAppend = this.getFirstAppend(node);
        if (firstAppend == null) {
            return;
        }
        ArrayList<FlowValue> appendCalls = new ArrayList<FlowValue>();
        FlowValue currentAppend = firstAppend;
        while (true) {
            appendCalls.add(currentAppend);
            Collection<Pair<FlowValue, Integer>> next = currentAppend.getNext();
            if (next.size() != 1) {
                return;
            }
            child = next.iterator().next();
            if (!this.isAppendCall(child)) break;
            currentAppend = child.getLeft();
        }
        if (!this.isToStringCall(child)) {
            return;
        }
        FlowValue toStringCall = child.getLeft();
        if (appendCalls.size() < 2) {
            return;
        }
        this.decorateConcat(appendCalls, toStringCall);
    }

    private void decorateConcat(List<FlowValue> appendCalls, FlowValue toStringCall) {
        FlowValue initialComponent = appendCalls.get(0).getInput(1);
        for (int i = 1; i < appendCalls.size() - 1; ++i) {
            appendCalls.get(i).decorate("stringConcatInfo", new StringConcatInfo(i == 1, true, initialComponent, toStringCall));
        }
        toStringCall.decorate("stringConcatInfo", new StringConcatInfo(appendCalls.size() == 2, false, initialComponent, toStringCall));
    }

    private FlowValue getFirstAppend(FlowValue node) {
        InstantiationInfo instantiation = (InstantiationInfo)node.getDecoration("instantiationInfo");
        if (instantiation == null || !instantiation.type.getInternalName().equals(STRING_BUILDER)) {
            return null;
        }
        if (!this.isEmptyInit(instantiation.initCall)) {
            return null;
        }
        if (node.getNext().size() != 1) {
            return null;
        }
        Pair<FlowValue, Integer> firstAppend = node.getNext().iterator().next();
        if (this.isAppendCall(firstAppend)) {
            return firstAppend.getLeft();
        }
        return null;
    }

    private boolean isEmptyInit(FlowValue call) {
        return ((MethodInsnNode)call.getInsn()).desc.equals("()V");
    }

    private boolean isAppendCall(Pair<FlowValue, Integer> child) {
        if (child.getRight() != 0) {
            return false;
        }
        AbstractInsnNode insn = child.getLeft().getInsn();
        if (insn.getOpcode() != 182) {
            return false;
        }
        MethodInsnNode call = (MethodInsnNode)insn;
        return call.owner.equals(STRING_BUILDER) && call.name.equals("append") && Type.getArgumentTypes((String)call.desc).length == 1;
    }

    private boolean isToStringCall(Pair<FlowValue, Integer> child) {
        if (child.getRight() != 0) {
            return false;
        }
        AbstractInsnNode insn = child.getLeft().getInsn();
        if (insn.getOpcode() != 182) {
            return false;
        }
        MethodInsnNode call = (MethodInsnNode)insn;
        return call.owner.equals(STRING_BUILDER) && call.name.equals("toString");
    }
}

