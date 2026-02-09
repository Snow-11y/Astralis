package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.DecisionState;

public final class PlusLoopbackState
extends DecisionState {
    @Override
    public int getStateType() {
        return 11;
    }
}

