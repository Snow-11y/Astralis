package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNConfigSet;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.ObjectEqualityComparator;

public class OrderedATNConfigSet
extends ATNConfigSet {
    public OrderedATNConfigSet() {
        this.configLookup = new LexerConfigHashSet();
    }

    public static class LexerConfigHashSet
    extends ATNConfigSet.AbstractConfigHashSet {
        public LexerConfigHashSet() {
            super(ObjectEqualityComparator.INSTANCE);
        }
    }
}

