package com.llamalad7.mixinextras.lib.antlr.runtime.tree;

import com.llamalad7.mixinextras.lib.antlr.runtime.ParserRuleContext;
import com.llamalad7.mixinextras.lib.antlr.runtime.tree.ErrorNode;
import com.llamalad7.mixinextras.lib.antlr.runtime.tree.TerminalNode;

public interface ParseTreeListener {
    public void visitTerminal(TerminalNode var1);

    public void visitErrorNode(ErrorNode var1);

    public void enterEveryRule(ParserRuleContext var1);

    public void exitEveryRule(ParserRuleContext var1);
}

