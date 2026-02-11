package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.RuleContext;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNState;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.ATNType;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.DecisionState;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.LL1Analyzer;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.LexerAction;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.RuleStartState;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.RuleStopState;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.RuleTransition;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.TokensStartState;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.IntervalSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ATN {
    public final List<ATNState> states = new ArrayList<ATNState>();
    public final List<DecisionState> decisionToState = new ArrayList<DecisionState>();
    public RuleStartState[] ruleToStartState;
    public RuleStopState[] ruleToStopState;
    public final Map<String, TokensStartState> modeNameToStartState = new LinkedHashMap<String, TokensStartState>();
    public final ATNType grammarType;
    public final int maxTokenType;
    public int[] ruleToTokenType;
    public LexerAction[] lexerActions;
    public final List<TokensStartState> modeToStartState = new ArrayList<TokensStartState>();

    public ATN(ATNType grammarType, int maxTokenType) {
        this.grammarType = grammarType;
        this.maxTokenType = maxTokenType;
    }

    public IntervalSet nextTokens(ATNState s, RuleContext ctx) {
        LL1Analyzer anal = new LL1Analyzer(this);
        IntervalSet next = anal.LOOK(s, ctx);
        return next;
    }

    public IntervalSet nextTokens(ATNState s) {
        if (s.nextTokenWithinRule != null) {
            return s.nextTokenWithinRule;
        }
        s.nextTokenWithinRule = this.nextTokens(s, null);
        s.nextTokenWithinRule.setReadonly(true);
        return s.nextTokenWithinRule;
    }

    public void addState(ATNState state) {
        if (state != null) {
            state.atn = this;
            state.stateNumber = this.states.size();
        }
        this.states.add(state);
    }

    public int defineDecisionState(DecisionState s) {
        this.decisionToState.add(s);
        s.decision = this.decisionToState.size() - 1;
        return s.decision;
    }

    public DecisionState getDecisionState(int decision) {
        if (!this.decisionToState.isEmpty()) {
            return this.decisionToState.get(decision);
        }
        return null;
    }

    public int getNumberOfDecisions() {
        return this.decisionToState.size();
    }

    public IntervalSet getExpectedTokens(int stateNumber, RuleContext context) {
        if (stateNumber < 0 || stateNumber >= this.states.size()) {
            throw new IllegalArgumentException("Invalid state number.");
        }
        RuleContext ctx = context;
        ATNState s = this.states.get(stateNumber);
        IntervalSet following = this.nextTokens(s);
        if (!following.contains(-2)) {
            return following;
        }
        IntervalSet expected = new IntervalSet(new int[0]);
        expected.addAll(following);
        expected.remove(-2);
        while (ctx != null && ctx.invokingState >= 0 && following.contains(-2)) {
            ATNState invokingState = this.states.get(ctx.invokingState);
            RuleTransition rt = (RuleTransition)invokingState.transition(0);
            following = this.nextTokens(rt.followState);
            expected.addAll(following);
            expected.remove(-2);
            ctx = ctx.parent;
        }
        if (following.contains(-2)) {
            expected.add(-1);
        }
        return expected;
    }
}

