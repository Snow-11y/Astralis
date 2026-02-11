package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNState;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ActionTransition;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.AtomTransition;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.EpsilonTransition;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.NotSetTransition;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.PrecedencePredicateTransition;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.PredicateTransition;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.RangeTransition;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.RuleTransition;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.SetTransition;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.WildcardTransition;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.IntervalSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Transition {
    public static final List<String> serializationNames = Collections.unmodifiableList(Arrays.asList("INVALID", "EPSILON", "RANGE", "RULE", "PREDICATE", "ATOM", "ACTION", "SET", "NOT_SET", "WILDCARD", "PRECEDENCE"));
    public static final Map<Class<? extends Transition>, Integer> serializationTypes = Collections.unmodifiableMap(new HashMap<Class<? extends Transition>, Integer>(){
        {
            this.put(EpsilonTransition.class, 1);
            this.put(RangeTransition.class, 2);
            this.put(RuleTransition.class, 3);
            this.put(PredicateTransition.class, 4);
            this.put(AtomTransition.class, 5);
            this.put(ActionTransition.class, 6);
            this.put(SetTransition.class, 7);
            this.put(NotSetTransition.class, 8);
            this.put(WildcardTransition.class, 9);
            this.put(PrecedencePredicateTransition.class, 10);
        }
    });
    public ATNState target;

    protected Transition(ATNState target) {
        if (target == null) {
            throw new NullPointerException("target cannot be null.");
        }
        this.target = target;
    }

    public abstract int getSerializationType();

    public boolean isEpsilon() {
        return false;
    }

    public IntervalSet label() {
        return null;
    }

    public abstract boolean matches(int var1, int var2, int var3);
}

