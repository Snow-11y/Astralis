package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

import com.llamalad7.mixinextras.lib.antlr.runtime.Lexer;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.LexerAction;
import com.llamalad7.mixinextras.lib.antlr.runtime.atn.LexerActionType;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.MurmurHash;

public final class LexerChannelAction
implements LexerAction {
    private final int channel;

    public LexerChannelAction(int channel) {
        this.channel = channel;
    }

    public LexerActionType getActionType() {
        return LexerActionType.CHANNEL;
    }

    @Override
    public boolean isPositionDependent() {
        return false;
    }

    @Override
    public void execute(Lexer lexer) {
        lexer.setChannel(this.channel);
    }

    public int hashCode() {
        int hash = MurmurHash.initialize();
        hash = MurmurHash.update(hash, this.getActionType().ordinal());
        hash = MurmurHash.update(hash, this.channel);
        return MurmurHash.finish(hash, 2);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof LexerChannelAction)) {
            return false;
        }
        return this.channel == ((LexerChannelAction)obj).channel;
    }

    public String toString() {
        return String.format("channel(%d)", this.channel);
    }
}

