package com.llamalad7.mixinextras.lib.antlr.runtime;

import com.llamalad7.mixinextras.lib.antlr.runtime.TokenSource;

public interface Token {
    public String getText();

    public int getType();

    public int getLine();

    public int getCharPositionInLine();

    public int getChannel();

    public int getTokenIndex();

    public int getStartIndex();

    public int getStopIndex();

    public TokenSource getTokenSource();
}

