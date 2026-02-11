package com.llamalad7.mixinextras.lib.antlr.runtime;

import com.llamalad7.mixinextras.lib.antlr.runtime.ANTLRErrorListener;
import com.llamalad7.mixinextras.lib.antlr.runtime.ConsoleErrorListener;
import com.llamalad7.mixinextras.lib.antlr.runtime.ProxyErrorListener;
import com.llamalad7.mixinextras.lib.antlr.runtime.RuleContext;
import com.llamalad7.mixinextras.lib.antlr.runtime.Vocabulary;
import com.llamalad7.mixinextras.lib.antlr.runtime.VocabularyImpl;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATN;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNSimulator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class Recognizer<Symbol, ATNInterpreter extends ATNSimulator> {
    private static final Map<Vocabulary, Map<String, Integer>> tokenTypeMapCache = new WeakHashMap<Vocabulary, Map<String, Integer>>();
    private static final Map<String[], Map<String, Integer>> ruleIndexMapCache = new WeakHashMap<String[], Map<String, Integer>>();
    private List<ANTLRErrorListener> _listeners = new CopyOnWriteArrayList<ANTLRErrorListener>(){
        {
            this.add(ConsoleErrorListener.INSTANCE);
        }
    };
    protected ATNInterpreter _interp;
    private int _stateNumber = -1;

    @Deprecated
    public abstract String[] getTokenNames();

    public abstract String[] getRuleNames();

    public Vocabulary getVocabulary() {
        return VocabularyImpl.fromTokenNames(this.getTokenNames());
    }

    public abstract ATN getATN();

    public ATNInterpreter getInterpreter() {
        return this._interp;
    }

    public void addErrorListener(ANTLRErrorListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener cannot be null.");
        }
        this._listeners.add(listener);
    }

    public void removeErrorListeners() {
        this._listeners.clear();
    }

    public List<? extends ANTLRErrorListener> getErrorListeners() {
        return this._listeners;
    }

    public ANTLRErrorListener getErrorListenerDispatch() {
        return new ProxyErrorListener(this.getErrorListeners());
    }

    public boolean sempred(RuleContext _localctx, int ruleIndex, int actionIndex) {
        return true;
    }

    public boolean precpred(RuleContext localctx, int precedence) {
        return true;
    }

    public void action(RuleContext _localctx, int ruleIndex, int actionIndex) {
    }

    public final int getState() {
        return this._stateNumber;
    }

    public final void setState(int atnState) {
        this._stateNumber = atnState;
    }
}

