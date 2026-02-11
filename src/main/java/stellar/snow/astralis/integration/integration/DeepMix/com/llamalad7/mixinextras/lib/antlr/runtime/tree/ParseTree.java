package com.llamalad7.mixinextras.lib.antlr.runtime.tree;

import com.llamalad7.mixinextras.lib.antlr.runtime.RuleContext;

public interface ParseTree {
    public void setParent(RuleContext var1);

    public String getText();
}

