package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNState;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.Transition;

public final class WildcardTransition
extends Transition {
    public WildcardTransition(ATNState target) {
        super(target);
    }

    @Override
    public int getSerializationType() {
        return 9;
    }

    @Override
    public boolean matches(int symbol, int minVocabSymbol, int maxVocabSymbol) {
        return symbol >= minVocabSymbol && symbol <= maxVocabSymbol;
    }

    public String toString() {
        return ".";
    }
}

