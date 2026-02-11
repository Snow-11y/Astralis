package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.ArrayCreationInfo;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.FlowPostProcessor;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class NewArrayPostProcessor
implements FlowPostProcessor {
    private final Comparator<Pair<FlowValue, Integer>> insnIndexComparator = Comparator.comparingInt(o -> method.instructions.indexOf(((FlowValue)o.getLeft()).getInsn()));

    public NewArrayPostProcessor(MethodNode method) {
    }

    @Override
    public void process(FlowValue node, FlowPostProcessor.OutputSink sink) {
        AbstractInsnNode insn = node.getInsn();
        if (insn.getOpcode() == 189 || insn.getOpcode() == 188) {
            List<FlowValue> stores = this.getCreationStores(node);
            if (stores == null || stores.isEmpty()) {
                return;
            }
            sink.markAsSynthetic(node.getInput(0));
            for (FlowValue store : stores) {
                sink.markAsSynthetic(store);
                sink.markAsSynthetic(store.getInput(1));
            }
            node.decorate("mixinextras_persistent_arrayCreationInfo", new ArrayCreationInfo(stores.get(stores.size() - 1)));
            node.setParents((FlowValue[])stores.stream().map(it -> it.getInput(2)).toArray(FlowValue[]::new));
        }
    }

    private List<FlowValue> getCreationStores(FlowValue array) {
        Integer size = this.getIntConstant(array.getInput(0));
        if (size == null) {
            return null;
        }
        List sortedNext = array.getNext().stream().filter(it -> !((FlowValue)it.getLeft()).isComplex()).sorted(this.insnIndexComparator).map(Pair::getLeft).collect(Collectors.toList());
        if (sortedNext.size() < size) {
            return null;
        }
        ArrayList<FlowValue> stores = new ArrayList<FlowValue>(size);
        for (int i = 0; i < size; ++i) {
            FlowValue store = (FlowValue)sortedNext.get(i);
            if (!this.isStore(array, store, i)) {
                return null;
            }
            stores.add(store);
        }
        return stores;
    }

    private Integer getIntConstant(FlowValue node) {
        if (node.isComplex()) {
            return null;
        }
        Object cst = ExpressionASMUtils.getConstant(node.getInsn());
        if (!(cst instanceof Integer)) {
            return null;
        }
        return (int)((Integer)cst);
    }

    private boolean isStore(FlowValue array, FlowValue store, int index) {
        int opcode = store.getInsn().getOpcode();
        if (opcode < 79 || opcode > 86) {
            return false;
        }
        if (store.getInput(0) != array) {
            return false;
        }
        return Objects.equals(index, this.getIntConstant(store.getInput(1)));
    }
}

