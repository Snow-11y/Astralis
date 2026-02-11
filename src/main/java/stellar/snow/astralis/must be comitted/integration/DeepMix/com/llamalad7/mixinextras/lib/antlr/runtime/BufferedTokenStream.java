package com.llamalad7.mixinextras.lib.antlr.runtime;

import com.llamalad7.mixinextras.lib.antlr.runtime.Token;
import com.llamalad7.mixinextras.lib.antlr.runtime.TokenSource;
import com.llamalad7.mixinextras.lib.antlr.runtime.TokenStream;
import com.llamalad7.mixinextras.lib.antlr.runtime.WritableToken;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.Interval;
import java.util.ArrayList;
import java.util.List;

public class BufferedTokenStream
implements TokenStream {
    protected TokenSource tokenSource;
    protected List<Token> tokens = new ArrayList<Token>(100);
    protected int p = -1;
    protected boolean fetchedEOF;

    public BufferedTokenStream(TokenSource tokenSource) {
        if (tokenSource == null) {
            throw new NullPointerException("tokenSource cannot be null");
        }
        this.tokenSource = tokenSource;
    }

    @Override
    public TokenSource getTokenSource() {
        return this.tokenSource;
    }

    @Override
    public int index() {
        return this.p;
    }

    @Override
    public int mark() {
        return 0;
    }

    @Override
    public void release(int marker) {
    }

    @Override
    public void seek(int index) {
        this.lazyInit();
        this.p = this.adjustSeekIndex(index);
    }

    @Override
    public int size() {
        return this.tokens.size();
    }

    @Override
    public void consume() {
        boolean skipEofCheck = this.p >= 0 ? (this.fetchedEOF ? this.p < this.tokens.size() - 1 : this.p < this.tokens.size()) : false;
        if (!skipEofCheck && this.LA(1) == -1) {
            throw new IllegalStateException("cannot consume EOF");
        }
        if (this.sync(this.p + 1)) {
            this.p = this.adjustSeekIndex(this.p + 1);
        }
    }

    protected boolean sync(int i) {
        assert (i >= 0);
        int n = i - this.tokens.size() + 1;
        if (n > 0) {
            int fetched = this.fetch(n);
            return fetched >= n;
        }
        return true;
    }

    protected int fetch(int n) {
        if (this.fetchedEOF) {
            return 0;
        }
        for (int i = 0; i < n; ++i) {
            Token t = this.tokenSource.nextToken();
            if (t instanceof WritableToken) {
                ((WritableToken)t).setTokenIndex(this.tokens.size());
            }
            this.tokens.add(t);
            if (t.getType() != -1) continue;
            this.fetchedEOF = true;
            return i + 1;
        }
        return n;
    }

    @Override
    public Token get(int i) {
        if (i < 0 || i >= this.tokens.size()) {
            throw new IndexOutOfBoundsException("token index " + i + " out of range 0.." + (this.tokens.size() - 1));
        }
        return this.tokens.get(i);
    }

    @Override
    public int LA(int i) {
        return this.LT(i).getType();
    }

    protected Token LB(int k) {
        if (this.p - k < 0) {
            return null;
        }
        return this.tokens.get(this.p - k);
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
        int i = this.p + k - 1;
        this.sync(i);
        if (i >= this.tokens.size()) {
            return this.tokens.get(this.tokens.size() - 1);
        }
        return this.tokens.get(i);
    }

    protected int adjustSeekIndex(int i) {
        return i;
    }

    protected final void lazyInit() {
        if (this.p == -1) {
            this.setup();
        }
    }

    protected void setup() {
        this.sync(0);
        this.p = this.adjustSeekIndex(0);
    }

    protected int nextTokenOnChannel(int i, int channel) {
        this.sync(i);
        if (i >= this.size()) {
            return this.size() - 1;
        }
        Token token = this.tokens.get(i);
        while (token.getChannel() != channel) {
            if (token.getType() == -1) {
                return i;
            }
            this.sync(++i);
            token = this.tokens.get(i);
        }
        return i;
    }

    protected int previousTokenOnChannel(int i, int channel) {
        this.sync(i);
        if (i >= this.size()) {
            return this.size() - 1;
        }
        while (i >= 0) {
            Token token = this.tokens.get(i);
            if (token.getType() == -1 || token.getChannel() == channel) {
                return i;
            }
            --i;
        }
        return i;
    }

    @Override
    public String getText(Interval interval) {
        Token t;
        int start = interval.a;
        int stop = interval.b;
        if (start < 0 || stop < 0) {
            return "";
        }
        this.sync(stop);
        if (stop >= this.tokens.size()) {
            stop = this.tokens.size() - 1;
        }
        StringBuilder buf = new StringBuilder();
        for (int i = start; i <= stop && (t = this.tokens.get(i)).getType() != -1; ++i) {
            buf.append(t.getText());
        }
        return buf.toString();
    }

    @Override
    public String getText(Token start, Token stop) {
        if (start != null && stop != null) {
            return this.getText(Interval.of(start.getTokenIndex(), stop.getTokenIndex()));
        }
        return "";
    }
}

