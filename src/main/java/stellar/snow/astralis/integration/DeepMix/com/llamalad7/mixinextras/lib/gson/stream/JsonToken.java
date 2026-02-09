package com.llamalad7.mixinextras.lib.gson.stream;

public final class JsonToken
extends Enum<JsonToken> {
    public static final /* enum */ JsonToken BEGIN_ARRAY = new JsonToken();
    public static final /* enum */ JsonToken END_ARRAY = new JsonToken();
    public static final /* enum */ JsonToken BEGIN_OBJECT = new JsonToken();
    public static final /* enum */ JsonToken END_OBJECT = new JsonToken();
    public static final /* enum */ JsonToken NAME = new JsonToken();
    public static final /* enum */ JsonToken STRING = new JsonToken();
    public static final /* enum */ JsonToken NUMBER = new JsonToken();
    public static final /* enum */ JsonToken BOOLEAN = new JsonToken();
    public static final /* enum */ JsonToken NULL = new JsonToken();
    public static final /* enum */ JsonToken END_DOCUMENT = new JsonToken();
    private static final /* synthetic */ JsonToken[] $VALUES;

    static {
        $VALUES = new JsonToken[]{BEGIN_ARRAY, END_ARRAY, BEGIN_OBJECT, END_OBJECT, NAME, STRING, NUMBER, BOOLEAN, NULL, END_DOCUMENT};
    }
}

