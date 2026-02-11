package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;

public enum MethodCallType {
    NORMAL,
    SUPER,
    STATIC;


    public boolean matches(FlowValue node) {
        return node.getDecoration("methodCallType") == this;
    }
}

