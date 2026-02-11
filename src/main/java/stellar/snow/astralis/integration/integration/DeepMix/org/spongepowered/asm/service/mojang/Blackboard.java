package org.spongepowered.asm.service.mojang;

import net.minecraft.launchwrapper.Launch;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

public class Blackboard
implements IGlobalPropertyService {
    public Blackboard() {
        Launch.classLoader.hashCode();
    }

    @Override
    public IPropertyKey resolveKey(String name) {
        return new Key(name);
    }

    @Override
    public final <T> T getProperty(IPropertyKey key) {
        return (T)Launch.blackboard.get(key.toString());
    }

    @Override
    public final void setProperty(IPropertyKey key, Object value) {
        Launch.blackboard.put(key.toString(), value);
    }

    @Override
    public final <T> T getProperty(IPropertyKey key, T defaultValue) {
        Object value = Launch.blackboard.get(key.toString());
        return (T)(value != null ? value : defaultValue);
    }

    @Override
    public final String getPropertyString(IPropertyKey key, String defaultValue) {
        Object value = Launch.blackboard.get(key.toString());
        return value != null ? value.toString() : defaultValue;
    }

    class Key
    implements IPropertyKey {
        private final String key;

        Key(String key) {
            this.key = key;
        }

        public String toString() {
            return this.key;
        }
    }
}

