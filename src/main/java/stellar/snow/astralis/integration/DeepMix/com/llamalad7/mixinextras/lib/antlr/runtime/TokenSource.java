package com.llamalad7.mixinextras.lib.antlr.runtime;

import com.llamalad7.mixinextras.lib.antlr.runtime.CharStream;
import com.llamalad7.mixinextras.lib.antlr.runtime.Token;
import com.llamalad7.mixinextras.lib.antlr.runtime.TokenFactory;

public interface TokenSource {
    public Token nextToken();

    public int getLine();

    public int getCharPositionInLine();

    public CharStream getInputStream();

    public TokenFactory<?> getTokenFactory();
}

