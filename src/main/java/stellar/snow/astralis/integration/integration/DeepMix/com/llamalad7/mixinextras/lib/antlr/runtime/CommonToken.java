package com.llamalad7.mixinextras.lib.antlr.runtime;

import com.llamalad7.mixinextras.lib.antlr.runtime.CharStream;
import com.llamalad7.mixinextras.lib.antlr.runtime.Recognizer;
import com.llamalad7.mixinextras.lib.antlr.runtime.TokenSource;
import com.llamalad7.mixinextras.lib.antlr.runtime.WritableToken;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.Interval;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.Pair;
import java.io.Serializable;

public class CommonToken
implements WritableToken,
Serializable {
    protected static final Pair<TokenSource, CharStream> EMPTY_SOURCE = new Pair<Object, Object>(null, null);
    protected int type;
    protected int line;
    protected int charPositionInLine = -1;
    protected int channel = 0;
    protected Pair<TokenSource, CharStream> source;
    protected String text;
    protected int index = -1;
    protected int start;
    protected int stop;

    public CommonToken(Pair<TokenSource, CharStream> source, int type, int channel, int start, int stop) {
        this.source = source;
        this.type = type;
        this.channel = channel;
        this.start = start;
        this.stop = stop;
        if (source.a != null) {
            this.line = ((TokenSource)source.a).getLine();
            this.charPositionInLine = ((TokenSource)source.a).getCharPositionInLine();
        }
    }

    @Override
    public int getType() {
        return this.type;
    }

    public void setLine(int line) {
        this.line = line;
    }

    @Override
    public String getText() {
        if (this.text != null) {
            return this.text;
        }
        CharStream input = this.getInputStream();
        if (input == null) {
            return null;
        }
        int n = input.size();
        if (this.start < n && this.stop < n) {
            return input.getText(Interval.of(this.start, this.stop));
        }
        return "<EOF>";
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public int getLine() {
        return this.line;
    }

    @Override
    public int getCharPositionInLine() {
        return this.charPositionInLine;
    }

    public void setCharPositionInLine(int charPositionInLine) {
        this.charPositionInLine = charPositionInLine;
    }

    @Override
    public int getChannel() {
        return this.channel;
    }

    @Override
    public int getStartIndex() {
        return this.start;
    }

    @Override
    public int getStopIndex() {
        return this.stop;
    }

    @Override
    public int getTokenIndex() {
        return this.index;
    }

    @Override
    public void setTokenIndex(int index) {
        this.index = index;
    }

    @Override
    public TokenSource getTokenSource() {
        return (TokenSource)this.source.a;
    }

    public CharStream getInputStream() {
        return (CharStream)this.source.b;
    }

    public String toString() {
        return this.toString(null);
    }

    public String toString(Recognizer<?, ?> r) {
        String txt;
        String channelStr = "";
        if (this.channel > 0) {
            channelStr = ",channel=" + this.channel;
        }
        if ((txt = this.getText()) != null) {
            txt = txt.replace("\n", "\\n");
            txt = txt.replace("\r", "\\r");
            txt = txt.replace("\t", "\\t");
        } else {
            txt = "<no text>";
        }
        String typeString = String.valueOf(this.type);
        if (r != null) {
            typeString = r.getVocabulary().getDisplayName(this.type);
        }
        return "[@" + this.getTokenIndex() + "," + this.start + ":" + this.stop + "='" + txt + "',<" + typeString + ">" + channelStr + "," + this.line + ":" + this.getCharPositionInLine() + "]";
    }
}

