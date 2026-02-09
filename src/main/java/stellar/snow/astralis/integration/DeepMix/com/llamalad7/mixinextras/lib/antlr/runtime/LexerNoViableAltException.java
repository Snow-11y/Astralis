package com.llamalad7.mixinextras.lib.antlr.runtime;

import com.llamalad7.mixinextras.lib.antlr.runtime.CharStream;
import com.llamalad7.mixinextras.lib.antlr.runtime.Lexer;
import com.llamalad7.mixinextras.lib.antlr.runtime.RecognitionException;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNConfigSet;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.Interval;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.Utils;
import java.util.Locale;

public class LexerNoViableAltException
extends RecognitionException {
    private final int startIndex;
    private final ATNConfigSet deadEndConfigs;

    public LexerNoViableAltException(Lexer lexer, CharStream input, int startIndex, ATNConfigSet deadEndConfigs) {
        super(lexer, input, null);
        this.startIndex = startIndex;
        this.deadEndConfigs = deadEndConfigs;
    }

    @Override
    public CharStream getInputStream() {
        return (CharStream)super.getInputStream();
    }

    @Override
    public String toString() {
        String symbol = "";
        if (this.startIndex >= 0 && this.startIndex < this.getInputStream().size()) {
            symbol = this.getInputStream().getText(Interval.of(this.startIndex, this.startIndex));
            symbol = Utils.escapeWhitespace(symbol, false);
        }
        return String.format(Locale.getDefault(), "%s('%s')", LexerNoViableAltException.class.getSimpleName(), symbol);
    }
}

