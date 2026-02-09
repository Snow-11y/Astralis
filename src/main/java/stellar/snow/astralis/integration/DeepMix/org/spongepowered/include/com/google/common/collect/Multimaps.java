package org.spongepowered.include.com.google.common.collect;

import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.collect.Multimap;

public final class Multimaps {
    static boolean equalsImpl(Multimap<?, ?> multimap, @Nullable Object object) {
        if (object == multimap) {
            return true;
        }
        if (object instanceof Multimap) {
            Multimap that = (Multimap)object;
            return multimap.asMap().equals(that.asMap());
        }
        return false;
    }
}

