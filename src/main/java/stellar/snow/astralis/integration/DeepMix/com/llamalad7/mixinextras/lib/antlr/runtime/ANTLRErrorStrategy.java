package com.llamalad7.mixinextras.lib.antlr.runtime;

import com.llamalad7.mixinextras.lib.antlr.runtime.Parser;
import com.llamalad7.mixinextras.lib.antlr.runtime.RecognitionException;
import com.llamalad7.mixinextras.lib.antlr.runtime.Token;

public interface ANTLRErrorStrategy {
    public void reset(Parser var1);

    public Token recoverInline(Parser var1) throws RecognitionException;

    public void recover(Parser var1, RecognitionException var2) throws RecognitionException;

    public void sync(Parser var1) throws RecognitionException;

    public boolean inErrorRecoveryMode(Parser var1);

    public void reportMatch(Parser var1);

    public void reportError(Parser var1, RecognitionException var2);
}

