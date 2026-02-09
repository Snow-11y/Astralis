package com.llamalad7.mixinextras.lib.antlr.runtime;

import com.llamalad7.mixinextras.lib.antlr.runtime.tree.ParseTree;
import com.llamalad7.mixinextras.lib.antlr.runtime.tree.RuleNode;
import java.util.List;

public class RuleContext
implements RuleNode {
    public RuleContext parent;
    public int invokingState = -1;

    public RuleContext() {
    }

    public RuleContext(RuleContext parent, int invokingState) {
        this.parent = parent;
        this.invokingState = invokingState;
    }

    public boolean isEmpty() {
        return this.invokingState == -1;
    }

    @Override
    public String getText() {
        if (this.getChildCount() == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < this.getChildCount(); ++i) {
            builder.append(this.getChild(i).getText());
        }
        return builder.toString();
    }

    public int getRuleIndex() {
        return -1;
    }

    public void setAltNumber(int altNumber) {
    }

    @Override
    public void setParent(RuleContext parent) {
        this.parent = parent;
    }

    public ParseTree getChild(int i) {
        return null;
    }

    public int getChildCount() {
        return 0;
    }

    public String toString() {
        return this.toString(null, null);
    }

    public String toString(List<String> ruleNames, RuleContext stop) {
        StringBuilder buf = new StringBuilder();
        RuleContext p = this;
        buf.append("[");
        while (p != null && p != stop) {
            if (ruleNames == null) {
                if (!p.isEmpty()) {
                    buf.append(p.invokingState);
                }
            } else {
                int ruleIndex = p.getRuleIndex();
                String ruleName = ruleIndex >= 0 && ruleIndex < ruleNames.size() ? ruleNames.get(ruleIndex) : Integer.toString(ruleIndex);
                buf.append(ruleName);
            }
            if (!(p.parent == null || ruleNames == null && p.parent.isEmpty())) {
                buf.append(" ");
            }
            p = p.parent;
        }
        buf.append("]");
        return buf.toString();
    }
}

