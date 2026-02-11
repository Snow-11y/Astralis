package com.llamalad7.mixinextras.lib.antlr.runtime;

import com.llamalad7.mixinextras.lib.antlr.runtime.CharStream;
import com.llamalad7.mixinextras.lib.antlr.runtime.Token;
import com.llamalad7.mixinextras.lib.antlr.runtime.TokenSource;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.Pair;

public interface TokenFactory<Symbol extends Token> {
    public Symbol create(Pair<TokenSource, CharStream> var1, int var2, String var3, int var4, int var5, int var6, int var7, int var8);
}

