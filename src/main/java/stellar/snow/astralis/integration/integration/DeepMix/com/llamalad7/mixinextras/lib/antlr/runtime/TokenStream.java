package com.llamalad7.mixinextras.lib.antlr.runtime;

import com.llamalad7.mixinextras.lib.antlr.runtime.IntStream;
import com.llamalad7.mixinextras.lib.antlr.runtime.Token;
import com.llamalad7.mixinextras.lib.antlr.runtime.TokenSource;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.Interval;

public interface TokenStream
extends IntStream {
    public Token LT(int var1);

    public Token get(int var1);

    public TokenSource getTokenSource();

    public String getText(Interval var1);

    public String getText(Token var1, Token var2);
}

