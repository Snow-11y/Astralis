package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.EmptyPredictionContext;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.PredictionContext;
import java.util.HashMap;
import java.util.Map;

public class PredictionContextCache {
    protected final Map<PredictionContext, PredictionContext> cache = new HashMap<PredictionContext, PredictionContext>();

    public PredictionContext add(PredictionContext ctx) {
        if (ctx == EmptyPredictionContext.Instance) {
            return EmptyPredictionContext.Instance;
        }
        PredictionContext existing = this.cache.get(ctx);
        if (existing != null) {
            return existing;
        }
        this.cache.put(ctx, ctx);
        return ctx;
    }

    public PredictionContext get(PredictionContext ctx) {
        return this.cache.get(ctx);
    }
}

