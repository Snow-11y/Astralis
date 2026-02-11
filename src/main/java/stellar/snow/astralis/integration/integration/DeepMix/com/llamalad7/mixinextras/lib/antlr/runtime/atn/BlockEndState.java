package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNState;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.BlockStartState;

public final class BlockEndState
extends ATNState {
    public BlockStartState startState;

    @Override
    public int getStateType() {
        return 8;
    }
}

