package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.Lexer;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.LexerAction;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.LexerActionType;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.MurmurHash;

public class LexerTypeAction
implements LexerAction {
    private final int type;

    public LexerTypeAction(int type) {
        this.type = type;
    }

    public LexerActionType getActionType() {
        return LexerActionType.TYPE;
    }

    @Override
    public boolean isPositionDependent() {
        return false;
    }

    @Override
    public void execute(Lexer lexer) {
        lexer.setType(this.type);
    }

    public int hashCode() {
        int hash = MurmurHash.initialize();
        hash = MurmurHash.update(hash, this.getActionType().ordinal());
        hash = MurmurHash.update(hash, this.type);
        return MurmurHash.finish(hash, 2);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof LexerTypeAction)) {
            return false;
        }
        return this.type == ((LexerTypeAction)obj).type;
    }

    public String toString() {
        return String.format("type(%d)", this.type);
    }
}

