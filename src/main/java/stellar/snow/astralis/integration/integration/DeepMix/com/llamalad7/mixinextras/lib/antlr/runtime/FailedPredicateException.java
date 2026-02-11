package com.llamalad7.mixinextras.lib.antlr.runtime;

import com.llamalad7.mixinextras.lib.antlr.runtime.Parser;
import com.llamalad7.mixinextras.lib.antlr.runtime.RecognitionException;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNState;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.AbstractPredicateTransition;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ParserATNSimulator;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.PredicateTransition;
import java.util.Locale;

public class FailedPredicateException
extends RecognitionException {
    private final int ruleIndex;
    private final int predicateIndex;
    private final String predicate;

    public FailedPredicateException(Parser recognizer, String predicate) {
        this(recognizer, predicate, null);
    }

    public FailedPredicateException(Parser recognizer, String predicate, String message) {
        super(FailedPredicateException.formatMessage(predicate, message), recognizer, recognizer.getInputStream(), recognizer._ctx);
        ATNState s = ((ParserATNSimulator)recognizer.getInterpreter()).atn.states.get(recognizer.getState());
        AbstractPredicateTransition trans = (AbstractPredicateTransition)s.transition(0);
        if (trans instanceof PredicateTransition) {
            this.ruleIndex = ((PredicateTransition)trans).ruleIndex;
            this.predicateIndex = ((PredicateTransition)trans).predIndex;
        } else {
            this.ruleIndex = 0;
            this.predicateIndex = 0;
        }
        this.predicate = predicate;
        this.setOffendingToken(recognizer.getCurrentToken());
    }

    private static String formatMessage(String predicate, String message) {
        if (message != null) {
            return message;
        }
        return String.format(Locale.getDefault(), "failed predicate: {%s}?", predicate);
    }
}

