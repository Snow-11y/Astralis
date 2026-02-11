package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.BlockEndState;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.DecisionState;

public abstract class BlockStartState
extends DecisionState {
    public BlockEndState endState;
}

