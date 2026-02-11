package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.BlockStartState;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.PlusLoopbackState;

public final class PlusBlockStartState
extends BlockStartState {
    public PlusLoopbackState loopBackState;

    @Override
    public int getStateType() {
        return 4;
    }
}

