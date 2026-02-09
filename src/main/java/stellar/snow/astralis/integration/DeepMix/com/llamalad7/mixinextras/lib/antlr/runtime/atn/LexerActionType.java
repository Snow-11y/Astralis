package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

public final class LexerActionType
extends Enum<LexerActionType> {
    public static final /* enum */ LexerActionType CHANNEL = new LexerActionType();
    public static final /* enum */ LexerActionType CUSTOM = new LexerActionType();
    public static final /* enum */ LexerActionType MODE = new LexerActionType();
    public static final /* enum */ LexerActionType MORE = new LexerActionType();
    public static final /* enum */ LexerActionType POP_MODE = new LexerActionType();
    public static final /* enum */ LexerActionType PUSH_MODE = new LexerActionType();
    public static final /* enum */ LexerActionType SKIP = new LexerActionType();
    public static final /* enum */ LexerActionType TYPE = new LexerActionType();
    private static final /* synthetic */ LexerActionType[] $VALUES;

    public static LexerActionType[] values() {
        return (LexerActionType[])$VALUES.clone();
    }

    static {
        $VALUES = new LexerActionType[]{CHANNEL, CUSTOM, MODE, MORE, POP_MODE, PUSH_MODE, SKIP, TYPE};
    }
}

