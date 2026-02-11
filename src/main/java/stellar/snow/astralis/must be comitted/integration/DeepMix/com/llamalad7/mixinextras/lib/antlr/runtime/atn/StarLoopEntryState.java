package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.DecisionState;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.StarLoopbackState;

public final class StarLoopEntryState
extends DecisionState {
    public StarLoopbackState loopBackState;
    public boolean isPrecedenceDecision;

    @Override
    public int getStateType() {
        return 10;
    }
}

