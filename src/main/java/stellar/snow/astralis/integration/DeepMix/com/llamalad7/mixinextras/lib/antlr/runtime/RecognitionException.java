package com.llamalad7.mixinextras.lib.antlr.runtime;

import com.llamalad7.mixinextras.lib.antlr.runtime.IntStream;
import com.llamalad7.mixinextras.lib.antlr.runtime.ParserRuleContext;
import com.llamalad7.mixinextras.lib.antlr.runtime.Recognizer;
import com.llamalad7.mixinextras.lib.antlr.runtime.RuleContext;
import com.llamalad7.mixinextras.lib.antlr.runtime.Token;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.IntervalSet;

public class RecognitionException
extends RuntimeException {
    private final Recognizer<?, ?> recognizer;
    private final RuleContext ctx;
    private final IntStream input;
    private Token offendingToken;
    private int offendingState = -1;

    public RecognitionException(Recognizer<?, ?> recognizer, IntStream input, ParserRuleContext ctx) {
        this.recognizer = recognizer;
        this.input = input;
        this.ctx = ctx;
        if (recognizer != null) {
            this.offendingState = recognizer.getState();
        }
    }

    public RecognitionException(String message, Recognizer<?, ?> recognizer, IntStream input, ParserRuleContext ctx) {
        super(message);
        this.recognizer = recognizer;
        this.input = input;
        this.ctx = ctx;
        if (recognizer != null) {
            this.offendingState = recognizer.getState();
        }
    }

    protected final void setOffendingState(int offendingState) {
        this.offendingState = offendingState;
    }

    public IntervalSet getExpectedTokens() {
        if (this.recognizer != null) {
            return this.recognizer.getATN().getExpectedTokens(this.offendingState, this.ctx);
        }
        return null;
    }

    public IntStream getInputStream() {
        return this.input;
    }

    public Token getOffendingToken() {
        return this.offendingToken;
    }

    protected final void setOffendingToken(Token offendingToken) {
        this.offendingToken = offendingToken;
    }
}

