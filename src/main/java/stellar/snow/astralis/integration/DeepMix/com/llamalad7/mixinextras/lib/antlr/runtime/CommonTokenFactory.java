package com.llamalad7.mixinextras.lib.antlr.runtime;

import com.llamalad7.mixinextras.lib.antlr.runtime.CharStream;
import com.llamalad7.mixinextras.lib.antlr.runtime.CommonToken;
import com.llamalad7.mixinextras.lib.antlr.runtime.TokenFactory;
import com.llamalad7.mixinextras.lib.antlr.runtime.TokenSource;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.Interval;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.Pair;

public class CommonTokenFactory
implements TokenFactory<CommonToken> {
    public static final TokenFactory<CommonToken> DEFAULT = new CommonTokenFactory();
    protected final boolean copyText;

    public CommonTokenFactory(boolean copyText) {
        this.copyText = copyText;
    }

    public CommonTokenFactory() {
        this(false);
    }

    @Override
    public CommonToken create(Pair<TokenSource, CharStream> source, int type, String text, int channel, int start, int stop, int line, int charPositionInLine) {
        CommonToken t = new CommonToken(source, type, channel, start, stop);
        t.setLine(line);
        t.setCharPositionInLine(charPositionInLine);
        if (text != null) {
            t.setText(text);
        } else if (this.copyText && source.b != null) {
            t.setText(((CharStream)source.b).getText(Interval.of(start, stop)));
        }
        return t;
    }
}

