package com.llamalad7.mixinextras.lib.antlr.runtime.atn;

public final class ATNType
extends Enum<ATNType> {
    public static final /* enum */ ATNType LEXER = new ATNType();
    public static final /* enum */ ATNType PARSER = new ATNType();
    private static final /* synthetic */ ATNType[] $VALUES;

    public static ATNType[] values() {
        return (ATNType[])$VALUES.clone();
    }

    static {
        $VALUES = new ATNType[]{LEXER, PARSER};
    }
}

