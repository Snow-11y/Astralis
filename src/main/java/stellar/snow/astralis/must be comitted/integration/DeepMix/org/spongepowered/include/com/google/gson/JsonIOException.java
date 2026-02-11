package org.spongepowered.include.com.google.gson;

import org.spongepowered.include.com.google.gson.JsonParseException;

public final class JsonIOException
extends JsonParseException {
    public JsonIOException(String msg) {
        super(msg);
    }

    public JsonIOException(Throwable cause) {
        super(cause);
    }
}

