package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;

public interface FlowPostProcessor {
    public void process(FlowValue var1, OutputSink var2);

    public static interface OutputSink {
        public void markAsSynthetic(FlowValue var1);

        public void registerFlow(FlowValue ... var1);
    }
}

