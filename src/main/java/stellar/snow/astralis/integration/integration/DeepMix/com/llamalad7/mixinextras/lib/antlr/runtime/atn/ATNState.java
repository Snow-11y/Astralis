package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATN;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.Transition;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.IntervalSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class ATNState {
    public static final List<String> serializationNames = Collections.unmodifiableList(Arrays.asList("INVALID", "BASIC", "RULE_START", "BLOCK_START", "PLUS_BLOCK_START", "STAR_BLOCK_START", "TOKEN_START", "RULE_STOP", "BLOCK_END", "STAR_LOOP_BACK", "STAR_LOOP_ENTRY", "PLUS_LOOP_BACK", "LOOP_END"));
    public ATN atn = null;
    public int stateNumber = -1;
    public int ruleIndex;
    public boolean epsilonOnlyTransitions = false;
    protected final List<Transition> transitions = new ArrayList<Transition>(4);
    public IntervalSet nextTokenWithinRule;

    public int hashCode() {
        return this.stateNumber;
    }

    public boolean equals(Object o) {
        if (o instanceof ATNState) {
            return this.stateNumber == ((ATNState)o).stateNumber;
        }
        return false;
    }

    public String toString() {
        return String.valueOf(this.stateNumber);
    }

    public int getNumberOfTransitions() {
        return this.transitions.size();
    }

    public void addTransition(Transition e) {
        this.addTransition(this.transitions.size(), e);
    }

    public void addTransition(int index, Transition e) {
        if (this.transitions.isEmpty()) {
            this.epsilonOnlyTransitions = e.isEpsilon();
        } else if (this.epsilonOnlyTransitions != e.isEpsilon()) {
            System.err.format(Locale.getDefault(), "ATN state %d has both epsilon and non-epsilon transitions.\n", this.stateNumber);
            this.epsilonOnlyTransitions = false;
        }
        boolean alreadyPresent = false;
        for (Transition t : this.transitions) {
            if (t.target.stateNumber != e.target.stateNumber) continue;
            if (t.label() != null && e.label() != null && t.label().equals(e.label())) {
                alreadyPresent = true;
                break;
            }
            if (!t.isEpsilon() || !e.isEpsilon()) continue;
            alreadyPresent = true;
            break;
        }
        if (!alreadyPresent) {
            this.transitions.add(index, e);
        }
    }

    public Transition transition(int i) {
        return this.transitions.get(i);
    }

    public Transition removeTransition(int index) {
        return this.transitions.remove(index);
    }

    public abstract int getStateType();

    public final boolean onlyHasEpsilonTransitions() {
        return this.epsilonOnlyTransitions;
    }
}

