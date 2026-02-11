package com.llamalad7.mixinextras.lib.antlr.runtime;

import com.llamalad7.mixinextras.lib.antlr.runtime.BufferedTokenStream;
import com.llamalad7.mixinextras.lib.antlr.runtime.Token;
import com.llamalad7.mixinextras.lib.antlr.runtime.TokenSource;

public class CommonTokenStream
extends BufferedTokenStream {
    protected int channel = 0;

    public CommonTokenStream(TokenSource tokenSource) {
        super(tokenSource);
    }

    @Override
    protected int adjustSeekIndex(int i) {
        return this.nextTokenOnChannel(i, this.channel);
    }

    @Override
    protected Token LB(int k) {
        if (k == 0 || this.p - k < 0) {
            return null;
        }
        int i = this.p;
        for (int n = 1; n <= k && i > 0; ++n) {
            i = this.previousTokenOnChannel(i - 1, this.channel);
        }
        if (i < 0) {
            return null;
        }
        return (Token)this.tokens.get(i);
    }

    @Override
    public Token LT(int k) {
        this.lazyInit();
        if (k == 0) {
            return null;
        }
        if (k < 0) {
            return this.LB(-k);
        }
        int i = this.p;
        for (int n = 1; n < k; ++n) {
            if (!this.sync(i + 1)) continue;
            i = this.nextTokenOnChannel(i + 1, this.channel);
        }
        return (Token)this.tokens.get(i);
    }
}

