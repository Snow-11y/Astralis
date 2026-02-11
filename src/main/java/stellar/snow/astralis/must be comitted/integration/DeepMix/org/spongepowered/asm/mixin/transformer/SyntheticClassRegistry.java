package org.spongepowered.asm.mixin.transformer;

import java.util.HashMap;
import java.util.Map;
import org.spongepowered.asm.mixin.throwables.MixinError;
import org.spongepowered.asm.service.ISyntheticClassInfo;
import org.spongepowered.asm.service.ISyntheticClassRegistry;

class SyntheticClassRegistry
implements ISyntheticClassRegistry {
    private final Map<String, ISyntheticClassInfo> classes = new HashMap<String, ISyntheticClassInfo>();

    SyntheticClassRegistry() {
    }

    @Override
    public ISyntheticClassInfo findSyntheticClass(String name) {
        if (name == null) {
            return null;
        }
        return this.classes.get(name.replace('.', '/'));
    }

    void registerSyntheticClass(ISyntheticClassInfo sci) {
        String name = sci.getName();
        ISyntheticClassInfo info = this.classes.get(name);
        if (info != null) {
            if (info == sci) {
                return;
            }
            throw new MixinError("Synthetic class with name " + name + " was already registered by " + info.getMixin() + ". Duplicate being registered by " + sci.getMixin());
        }
        this.classes.put(name, sci);
    }
}

