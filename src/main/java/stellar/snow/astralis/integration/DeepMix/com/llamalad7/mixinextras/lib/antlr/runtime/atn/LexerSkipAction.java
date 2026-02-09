package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.Lexer;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.LexerAction;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.LexerActionType;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.MurmurHash;

public final class LexerSkipAction
implements LexerAction {
    public static final LexerSkipAction INSTANCE = new LexerSkipAction();

    private LexerSkipAction() {
    }

    public LexerActionType getActionType() {
        return LexerActionType.SKIP;
    }

    @Override
    public boolean isPositionDependent() {
        return false;
    }

    @Override
    public void execute(Lexer lexer) {
        lexer.skip();
    }

    public int hashCode() {
        int hash = MurmurHash.initialize();
        hash = MurmurHash.update(hash, this.getActionType().ordinal());
        return MurmurHash.finish(hash, 1);
    }

    public boolean equals(Object obj) {
        return obj == this;
    }

    public String toString() {
        return "skip";
    }
}

