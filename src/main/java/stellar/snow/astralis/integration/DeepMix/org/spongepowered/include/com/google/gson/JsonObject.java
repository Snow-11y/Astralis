package org.spongepowered.include.com.google.gson;

import java.util.Map;
import java.util.Set;
import org.spongepowered.include.com.google.gson.JsonElement;
import org.spongepowered.include.com.google.gson.JsonNull;
import org.spongepowered.include.com.google.gson.internal.LinkedTreeMap;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public final class JsonObject
extends JsonElement {
    private final LinkedTreeMap<String, JsonElement> members = new LinkedTreeMap();

    public void add(String property, JsonElement value) {
        if (value == null) {
            value = JsonNull.INSTANCE;
        }
        this.members.put(property, value);
    }

    public Set<Map.Entry<String, JsonElement>> entrySet() {
        return this.members.entrySet();
    }

    public boolean equals(Object o) {
        return o == this || o instanceof JsonObject && ((JsonObject)o).members.equals(this.members);
    }

    public int hashCode() {
        return this.members.hashCode();
    }
}

