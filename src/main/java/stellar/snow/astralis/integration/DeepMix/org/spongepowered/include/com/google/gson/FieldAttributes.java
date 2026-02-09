package org.spongepowered.include.com.google.gson;

import java.lang.reflect.Field;
import org.spongepowered.include.com.google.gson.internal.$Gson$Preconditions;

public final class FieldAttributes {
    private final Field field;

    public FieldAttributes(Field f) {
        $Gson$Preconditions.checkNotNull(f);
        this.field = f;
    }
}

