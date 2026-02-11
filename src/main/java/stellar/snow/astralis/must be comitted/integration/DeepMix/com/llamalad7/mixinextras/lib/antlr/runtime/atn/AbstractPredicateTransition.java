package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNState;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.Transition;

public abstract class AbstractPredicateTransition
extends Transition {
    public AbstractPredicateTransition(ATNState target) {
        super(target);
    }
}

