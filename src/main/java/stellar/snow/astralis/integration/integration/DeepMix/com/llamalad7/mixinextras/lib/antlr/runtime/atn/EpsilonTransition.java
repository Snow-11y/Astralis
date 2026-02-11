package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNState;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.Transition;

public final class EpsilonTransition
extends Transition {
    private final int outermostPrecedenceReturn;

    public EpsilonTransition(ATNState target) {
        this(target, -1);
    }

    public EpsilonTransition(ATNState target, int outermostPrecedenceReturn) {
        super(target);
        this.outermostPrecedenceReturn = outermostPrecedenceReturn;
    }

    public int outermostPrecedenceReturn() {
        return this.outermostPrecedenceReturn;
    }

    @Override
    public int getSerializationType() {
        return 1;
    }

    @Override
    public boolean isEpsilon() {
        return true;
    }

    @Override
    public boolean matches(int symbol, int minVocabSymbol, int maxVocabSymbol) {
        return false;
    }

    public String toString() {
        return "epsilon";
    }
}

