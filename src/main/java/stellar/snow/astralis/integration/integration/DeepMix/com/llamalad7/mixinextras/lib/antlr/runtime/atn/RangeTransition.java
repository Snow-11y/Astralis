package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNState;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.Transition;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.IntervalSet;

public final class RangeTransition
extends Transition {
    public final int from;
    public final int to;

    public RangeTransition(ATNState target, int from, int to) {
        super(target);
        this.from = from;
        this.to = to;
    }

    @Override
    public int getSerializationType() {
        return 2;
    }

    @Override
    public IntervalSet label() {
        return IntervalSet.of(this.from, this.to);
    }

    @Override
    public boolean matches(int symbol, int minVocabSymbol, int maxVocabSymbol) {
        return symbol >= this.from && symbol <= this.to;
    }

    public String toString() {
        return new StringBuilder("'").appendCodePoint(this.from).append("'..'").appendCodePoint(this.to).append("'").toString();
    }
}

