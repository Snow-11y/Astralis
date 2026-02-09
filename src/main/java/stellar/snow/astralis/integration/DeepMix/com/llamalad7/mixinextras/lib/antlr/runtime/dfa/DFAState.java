package com.llamalad7.mixinextras.lib.antlr.runtime.dfa;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNConfigSet;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.LexerActionExecutor;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.SemanticContext;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.MurmurHash;
import java.util.Arrays;

public class DFAState {
    public int stateNumber = -1;
    public ATNConfigSet configs = new ATNConfigSet();
    public DFAState[] edges;
    public boolean isAcceptState = false;
    public int prediction;
    public LexerActionExecutor lexerActionExecutor;
    public boolean requiresFullContext;
    public PredPrediction[] predicates;

    public DFAState() {
    }

    public DFAState(ATNConfigSet configs) {
        this.configs = configs;
    }

    public int hashCode() {
        int hash = MurmurHash.initialize(7);
        hash = MurmurHash.update(hash, this.configs.hashCode());
        hash = MurmurHash.finish(hash, 1);
        return hash;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DFAState)) {
            return false;
        }
        DFAState other = (DFAState)o;
        boolean sameSet = this.configs.equals(other.configs);
        return sameSet;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.stateNumber).append(":").append(this.configs);
        if (this.isAcceptState) {
            buf.append("=>");
            if (this.predicates != null) {
                buf.append(Arrays.toString(this.predicates));
            } else {
                buf.append(this.prediction);
            }
        }
        return buf.toString();
    }

    public static class PredPrediction {
        public SemanticContext pred;
        public int alt;

        public PredPrediction(SemanticContext pred, int alt) {
            this.alt = alt;
            this.pred = pred;
        }

        public String toString() {
            return "(" + this.pred + ", " + this.alt + ")";
        }
    }
}

