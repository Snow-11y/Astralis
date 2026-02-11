package org.spongepowered.include.com.google.gson.internal;

import java.io.IOException;
import org.spongepowered.include.com.google.gson.stream.JsonReader;

public abstract class JsonReaderInternalAccess {
    public static JsonReaderInternalAccess INSTANCE;

    public abstract void promoteNameToValue(JsonReader var1) throws IOException;
}

