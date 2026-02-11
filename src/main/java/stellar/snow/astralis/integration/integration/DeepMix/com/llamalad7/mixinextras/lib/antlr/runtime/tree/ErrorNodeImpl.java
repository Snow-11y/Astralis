package com.llamalad7.mixinextras.lib.antlr.runtime.tree;

import com.llamalad7.mixinextras.lib.antlr.runtime.Token;
import com.llamalad7.mixinextras.lib.antlr.runtime.tree.ErrorNode;
import com.llamalad7.mixinextras.lib.antlr.runtime.tree.TerminalNodeImpl;

public class ErrorNodeImpl
extends TerminalNodeImpl
implements ErrorNode {
    public ErrorNodeImpl(Token token) {
        super(token);
    }
}

