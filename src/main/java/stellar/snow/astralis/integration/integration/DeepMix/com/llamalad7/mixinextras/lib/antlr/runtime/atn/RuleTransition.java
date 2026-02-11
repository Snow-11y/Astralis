package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNState;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.RuleStartState;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.Transition;

public final class RuleTransition
extends Transition {
    public final int ruleIndex;
    public final int precedence;
    public ATNState followState;

    public RuleTransition(RuleStartState ruleStart, int ruleIndex, int precedence, ATNState followState) {
        super(ruleStart);
        this.ruleIndex = ruleIndex;
        this.precedence = precedence;
        this.followState = followState;
    }

    @Override
    public int getSerializationType() {
        return 3;
    }

    @Override
    public boolean isEpsilon() {
        return true;
    }

    @Override
    public boolean matches(int symbol, int minVocabSymbol, int maxVocabSymbol) {
        return false;
    }
}

