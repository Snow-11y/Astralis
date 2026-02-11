package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.Lexer;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.LexerAction;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.LexerActionType;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.MurmurHash;

public final class LexerCustomAction
implements LexerAction {
    private final int ruleIndex;
    private final int actionIndex;

    public LexerCustomAction(int ruleIndex, int actionIndex) {
        this.ruleIndex = ruleIndex;
        this.actionIndex = actionIndex;
    }

    public LexerActionType getActionType() {
        return LexerActionType.CUSTOM;
    }

    @Override
    public boolean isPositionDependent() {
        return true;
    }

    @Override
    public void execute(Lexer lexer) {
        lexer.action(null, this.ruleIndex, this.actionIndex);
    }

    public int hashCode() {
        int hash = MurmurHash.initialize();
        hash = MurmurHash.update(hash, this.getActionType().ordinal());
        hash = MurmurHash.update(hash, this.ruleIndex);
        hash = MurmurHash.update(hash, this.actionIndex);
        return MurmurHash.finish(hash, 3);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof LexerCustomAction)) {
            return false;
        }
        LexerCustomAction other = (LexerCustomAction)obj;
        return this.ruleIndex == other.ruleIndex && this.actionIndex == other.actionIndex;
    }
}

