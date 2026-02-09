package org.spongepowered.include.com.google.gson;

import org.spongepowered.include.com.google.gson.JsonParseException;

public final class JsonSyntaxException
extends JsonParseException {
    public JsonSyntaxException(String msg) {
        super(msg);
    }

    public JsonSyntaxException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public JsonSyntaxException(Throwable cause) {
        super(cause);
    }
}

