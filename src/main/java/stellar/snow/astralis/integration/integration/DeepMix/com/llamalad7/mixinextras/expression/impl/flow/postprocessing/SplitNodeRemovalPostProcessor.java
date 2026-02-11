package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import com.llamalad7.mixinextras.expression.impl.flow.DummyFlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.FlowPostProcessor;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import java.util.ArrayList;

public class SplitNodeRemovalPostProcessor
implements FlowPostProcessor {
    @Override
    public void process(FlowValue node, FlowPostProcessor.OutputSink sink) {
        if (node.getNext().size() <= 1) {
            return;
        }
        DummyFlowValue successor = new DummyFlowValue(node.getType());
        for (Pair<FlowValue, Integer> next : new ArrayList<Pair<FlowValue, Integer>>(node.getNext())) {
            FlowValue target = next.getLeft();
            int index = next.getRight();
            target.setParent(index, successor);
        }
    }
}

