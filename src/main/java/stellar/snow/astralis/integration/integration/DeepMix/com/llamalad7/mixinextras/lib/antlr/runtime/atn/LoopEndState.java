package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNState;

public final class LoopEndState
extends ATNState {
    public ATNState loopBackState;

    @Override
    public int getStateType() {
        return 12;
    }
}

