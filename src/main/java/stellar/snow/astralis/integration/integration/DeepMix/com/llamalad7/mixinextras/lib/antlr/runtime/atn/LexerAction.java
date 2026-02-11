package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.Lexer;

public interface LexerAction {
    public boolean isPositionDependent();

    public void execute(Lexer var1);
}

