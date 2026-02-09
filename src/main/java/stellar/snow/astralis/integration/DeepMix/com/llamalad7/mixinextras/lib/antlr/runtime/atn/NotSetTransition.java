package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNState;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.SetTransition;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.IntervalSet;

public final class NotSetTransition
extends SetTransition {
    public NotSetTransition(ATNState target, IntervalSet set) {
        super(target, set);
    }

    @Override
    public int getSerializationType() {
        return 8;
    }

    @Override
    public boolean matches(int symbol, int minVocabSymbol, int maxVocabSymbol) {
        return symbol >= minVocabSymbol && symbol <= maxVocabSymbol && !super.matches(symbol, minVocabSymbol, maxVocabSymbol);
    }

    @Override
    public String toString() {
        return '~' + super.toString();
    }
}

