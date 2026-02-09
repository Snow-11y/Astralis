package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATN;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNConfigSet;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.PredictionContext;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.PredictionContextCache;
import com.llamalad7.mixinextras.lib.antlr.runtime.dfa.DFAState;
import java.util.IdentityHashMap;

public abstract class ATNSimulator {
    public static final DFAState ERROR = new DFAState(new ATNConfigSet());
    public final ATN atn;
    protected final PredictionContextCache sharedContextCache;

    public ATNSimulator(ATN atn, PredictionContextCache sharedContextCache) {
        this.atn = atn;
        this.sharedContextCache = sharedContextCache;
    }

    public abstract void reset();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public PredictionContext getCachedContext(PredictionContext context) {
        if (this.sharedContextCache == null) {
            return context;
        }
        PredictionContextCache predictionContextCache = this.sharedContextCache;
        synchronized (predictionContextCache) {
            IdentityHashMap<PredictionContext, PredictionContext> visited = new IdentityHashMap<PredictionContext, PredictionContext>();
            return PredictionContext.getCachedContext(context, this.sharedContextCache, visited);
        }
    }

    static {
        ATNSimulator.ERROR.stateNumber = Integer.MAX_VALUE;
    }
}

