package com.llamalad7.mixinextras.lib.antlr.runtime.tree;

import com.llamalad7.mixinextras.lib.antlr.runtime.RuleContext;
import com.llamalad7.mixinextras.lib.antlr.runtime.Token;
import com.llamalad7.mixinextras.lib.antlr.runtime.tree.ParseTree;
import com.llamalad7.mixinextras.lib.antlr.runtime.tree.TerminalNode;

public class TerminalNodeImpl
implements TerminalNode {
    public Token symbol;
    public ParseTree parent;

    public TerminalNodeImpl(Token symbol) {
        this.symbol = symbol;
    }

    @Override
    public Token getSymbol() {
        return this.symbol;
    }

    @Override
    public void setParent(RuleContext parent) {
        this.parent = parent;
    }

    @Override
    public String getText() {
        return this.symbol.getText();
    }

    public String toString() {
        if (this.symbol.getType() == -1) {
            return "<EOF>";
        }
        return this.symbol.getText();
    }
}

