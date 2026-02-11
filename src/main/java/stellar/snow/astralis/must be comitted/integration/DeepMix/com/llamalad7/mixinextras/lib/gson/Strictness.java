package com.llamalad7.mixinextras.lib.gson;

public final class Strictness
extends Enum<Strictness> {
    public static final /* enum */ Strictness LENIENT = new Strictness();
    public static final /* enum */ Strictness LEGACY_STRICT = new Strictness();
    public static final /* enum */ Strictness STRICT = new Strictness();
    private static final /* synthetic */ Strictness[] $VALUES;

    static {
        $VALUES = new Strictness[]{LENIENT, LEGACY_STRICT, STRICT};
    }
}

