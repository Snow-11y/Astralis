package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNState;

public abstract class DecisionState
extends ATNState {
    public int decision = -1;
    public boolean nonGreedy;
}

