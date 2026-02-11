package com.llamalad7.mixinextras.lib.antlr.runtime;

import com.llamalad7.mixinextras.lib.antlr.runtime.ANTLRErrorListener;
import com.llamalad7.mixinextras.lib.antlr.runtime.ANTLRErrorStrategy;
import com.llamalad7.mixinextras.lib.antlr.runtime.DefaultErrorStrategy;
import com.llamalad7.mixinextras.lib.antlr.runtime.IntStream;
import com.llamalad7.mixinextras.lib.antlr.runtime.ParserRuleContext;
import com.llamalad7.mixinextras.lib.antlr.runtime.RecognitionException;
import com.llamalad7.mixinextras.lib.antlr.runtime.Recognizer;
import com.llamalad7.mixinextras.lib.antlr.runtime.RuleContext;
import com.llamalad7.mixinextras.lib.antlr.runtime.Token;
import com.llamalad7.mixinextras.lib.antlr.runtime.TokenFactory;
import com.llamalad7.mixinextras.lib.antlr.runtime.TokenStream;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNSimulator;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ParserATNSimulator;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.IntegerStack;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.IntervalSet;
import com.llamalad7.mixinextras.lib.antlr.runtime.tree.ErrorNode;
import com.llamalad7.mixinextras.lib.antlr.runtime.tree.ErrorNodeImpl;
import com.llamalad7.mixinextras.lib.antlr.runtime.tree.ParseTreeListener;
import com.llamalad7.mixinextras.lib.antlr.runtime.tree.TerminalNode;
import com.llamalad7.mixinextras.lib.antlr.runtime.tree.TerminalNodeImpl;
import java.util.ArrayList;
import java.util.List;

public abstract class Parser
extends Recognizer<Token, ParserATNSimulator> {
    protected ANTLRErrorStrategy _errHandler = new DefaultErrorStrategy();
    protected TokenStream _input;
    protected final IntegerStack _precedenceStack = new IntegerStack();
    protected ParserRuleContext _ctx;
    protected boolean _buildParseTrees;
    private TraceListener _tracer;
    protected List<ParseTreeListener> _parseListeners;
    protected int _syntaxErrors;
    protected boolean matchedEOF;

    public Parser(TokenStream input) {
        this._precedenceStack.push(0);
        this._buildParseTrees = true;
        this.setInputStream(input);
    }

    public void reset() {
        if (this.getInputStream() != null) {
            this.getInputStream().seek(0);
        }
        this._errHandler.reset(this);
        this._ctx = null;
        this._syntaxErrors = 0;
        this.matchedEOF = false;
        this.setTrace(false);
        this._precedenceStack.clear();
        this._precedenceStack.push(0);
        Object interpreter = this.getInterpreter();
        if (interpreter != null) {
            ((ATNSimulator)interpreter).reset();
        }
    }

    public Token match(int ttype) throws RecognitionException {
        Token t = this.getCurrentToken();
        if (t.getType() == ttype) {
            if (ttype == -1) {
                this.matchedEOF = true;
            }
            this._errHandler.reportMatch(this);
            this.consume();
        } else {
            t = this._errHandler.recoverInline(this);
            if (this._buildParseTrees && t.getTokenIndex() == -1) {
                this._ctx.addErrorNode(this.createErrorNode(this._ctx, t));
            }
        }
        return t;
    }

    public void addParseListener(ParseTreeListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        if (this._parseListeners == null) {
            this._parseListeners = new ArrayList<ParseTreeListener>();
        }
        this._parseListeners.add(listener);
    }

    public void removeParseListener(ParseTreeListener listener) {
        if (this._parseListeners != null && this._parseListeners.remove(listener) && this._parseListeners.isEmpty()) {
            this._parseListeners = null;
        }
    }

    protected void triggerEnterRuleEvent() {
        for (ParseTreeListener listener : this._parseListeners) {
            listener.enterEveryRule(this._ctx);
            this._ctx.enterRule(listener);
        }
    }

    protected void triggerExitRuleEvent() {
        for (int i = this._parseListeners.size() - 1; i >= 0; --i) {
            ParseTreeListener listener = this._parseListeners.get(i);
            this._ctx.exitRule(listener);
            listener.exitEveryRule(this._ctx);
        }
    }

    public TokenFactory<?> getTokenFactory() {
        return this._input.getTokenSource().getTokenFactory();
    }

    public TokenStream getInputStream() {
        return this.getTokenStream();
    }

    public final void setInputStream(IntStream input) {
        this.setTokenStream((TokenStream)input);
    }

    public TokenStream getTokenStream() {
        return this._input;
    }

    public void setTokenStream(TokenStream input) {
        this._input = null;
        this.reset();
        this._input = input;
    }

    public Token getCurrentToken() {
        return this._input.LT(1);
    }

    public void notifyErrorListeners(Token offendingToken, String msg, RecognitionException e) {
        ++this._syntaxErrors;
        int line = -1;
        int charPositionInLine = -1;
        line = offendingToken.getLine();
        charPositionInLine = offendingToken.getCharPositionInLine();
        ANTLRErrorListener listener = this.getErrorListenerDispatch();
        listener.syntaxError(this, offendingToken, line, charPositionInLine, msg, e);
    }

    public Token consume() {
        Token o;
        block4: {
            block5: {
                boolean hasListener;
                o = this.getCurrentToken();
                if (o.getType() != -1) {
                    this.getInputStream().consume();
                }
                boolean bl = hasListener = this._parseListeners != null && !this._parseListeners.isEmpty();
                if (!this._buildParseTrees && !hasListener) break block4;
                if (!this._errHandler.inErrorRecoveryMode(this)) break block5;
                ErrorNode node = this._ctx.addErrorNode(this.createErrorNode(this._ctx, o));
                if (this._parseListeners == null) break block4;
                for (ParseTreeListener listener : this._parseListeners) {
                    listener.visitErrorNode(node);
                }
                break block4;
            }
            TerminalNode node = this._ctx.addChild(this.createTerminalNode(this._ctx, o));
            if (this._parseListeners != null) {
                for (ParseTreeListener listener : this._parseListeners) {
                    listener.visitTerminal(node);
                }
            }
        }
        return o;
    }

    public TerminalNode createTerminalNode(ParserRuleContext parent, Token t) {
        return new TerminalNodeImpl(t);
    }

    public ErrorNode createErrorNode(ParserRuleContext parent, Token t) {
        return new ErrorNodeImpl(t);
    }

    protected void addContextToParseTree() {
        ParserRuleContext parent = (ParserRuleContext)this._ctx.parent;
        if (parent != null) {
            parent.addChild(this._ctx);
        }
    }

    public void enterRule(ParserRuleContext localctx, int state, int ruleIndex) {
        this.setState(state);
        this._ctx = localctx;
        this._ctx.start = this._input.LT(1);
        if (this._buildParseTrees) {
            this.addContextToParseTree();
        }
        if (this._parseListeners != null) {
            this.triggerEnterRuleEvent();
        }
    }

    public void exitRule() {
        this._ctx.stop = this.matchedEOF ? this._input.LT(1) : this._input.LT(-1);
        if (this._parseListeners != null) {
            this.triggerExitRuleEvent();
        }
        this.setState(this._ctx.invokingState);
        this._ctx = (ParserRuleContext)this._ctx.parent;
    }

    public void enterOuterAlt(ParserRuleContext localctx, int altNum) {
        ParserRuleContext parent;
        localctx.setAltNumber(altNum);
        if (this._buildParseTrees && this._ctx != localctx && (parent = (ParserRuleContext)this._ctx.parent) != null) {
            parent.removeLastChild();
            parent.addChild(localctx);
        }
        this._ctx = localctx;
    }

    public final int getPrecedence() {
        if (this._precedenceStack.isEmpty()) {
            return -1;
        }
        return this._precedenceStack.peek();
    }

    public void enterRecursionRule(ParserRuleContext localctx, int state, int ruleIndex, int precedence) {
        this.setState(state);
        this._precedenceStack.push(precedence);
        this._ctx = localctx;
        this._ctx.start = this._input.LT(1);
        if (this._parseListeners != null) {
            this.triggerEnterRuleEvent();
        }
    }

    public void pushNewRecursionContext(ParserRuleContext localctx, int state, int ruleIndex) {
        ParserRuleContext previous = this._ctx;
        previous.parent = localctx;
        previous.invokingState = state;
        previous.stop = this._input.LT(-1);
        this._ctx = localctx;
        this._ctx.start = previous.start;
        if (this._buildParseTrees) {
            this._ctx.addChild(previous);
        }
        if (this._parseListeners != null) {
            this.triggerEnterRuleEvent();
        }
    }

    public void unrollRecursionContexts(ParserRuleContext _parentctx) {
        this._precedenceStack.pop();
        this._ctx.stop = this._input.LT(-1);
        ParserRuleContext retctx = this._ctx;
        if (this._parseListeners != null) {
            while (this._ctx != _parentctx) {
                this.triggerExitRuleEvent();
                this._ctx = (ParserRuleContext)this._ctx.parent;
            }
        } else {
            this._ctx = _parentctx;
        }
        retctx.parent = _parentctx;
        if (this._buildParseTrees && _parentctx != null) {
            _parentctx.addChild(retctx);
        }
    }

    public ParserRuleContext getContext() {
        return this._ctx;
    }

    @Override
    public boolean precpred(RuleContext localctx, int precedence) {
        return precedence >= this._precedenceStack.peek();
    }

    public IntervalSet getExpectedTokens() {
        return this.getATN().getExpectedTokens(this.getState(), this.getContext());
    }

    public List<String> getRuleInvocationStack() {
        return this.getRuleInvocationStack(this._ctx);
    }

    public List<String> getRuleInvocationStack(RuleContext p) {
        String[] ruleNames = this.getRuleNames();
        ArrayList<String> stack = new ArrayList<String>();
        while (p != null) {
            int ruleIndex = p.getRuleIndex();
            if (ruleIndex < 0) {
                stack.add("n/a");
            } else {
                stack.add(ruleNames[ruleIndex]);
            }
            p = p.parent;
        }
        return stack;
    }

    public void setTrace(boolean trace) {
        if (!trace) {
            this.removeParseListener(this._tracer);
            this._tracer = null;
        } else {
            if (this._tracer != null) {
                this.removeParseListener(this._tracer);
            } else {
                this._tracer = new TraceListener();
            }
            this.addParseListener(this._tracer);
        }
    }

    public class TraceListener
    implements ParseTreeListener {
        @Override
        public void enterEveryRule(ParserRuleContext ctx) {
            System.out.println("enter   " + Parser.this.getRuleNames()[ctx.getRuleIndex()] + ", LT(1)=" + Parser.this._input.LT(1).getText());
        }

        @Override
        public void visitTerminal(TerminalNode node) {
            System.out.println("consume " + node.getSymbol() + " rule " + Parser.this.getRuleNames()[Parser.this._ctx.getRuleIndex()]);
        }

        @Override
        public void visitErrorNode(ErrorNode node) {
        }

        @Override
        public void exitEveryRule(ParserRuleContext ctx) {
            System.out.println("exit    " + Parser.this.getRuleNames()[ctx.getRuleIndex()] + ", LT(1)=" + Parser.this._input.LT(1).getText());
        }
    }
}

