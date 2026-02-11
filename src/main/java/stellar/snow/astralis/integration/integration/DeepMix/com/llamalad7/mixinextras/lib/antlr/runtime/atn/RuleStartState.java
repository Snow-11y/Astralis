package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNState;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.RuleStopState;

public final class RuleStartState
extends ATNState {
    public RuleStopState stopState;
    public boolean isLeftRecursiveRule;

    @Override
    public int getStateType() {
        return 2;
    }
}

