package org.spongepowered.include.com.google.common.collect;

import java.util.Collection;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Joiner;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.collect.CollectPreconditions;

public final class Collections2 {
    static final Joiner STANDARD_JOINER = Joiner.on(", ").useForNull("null");

    static boolean safeContains(Collection<?> collection, @Nullable Object object) {
        Preconditions.checkNotNull(collection);
        try {
            return collection.contains(object);
        }
        catch (ClassCastException e) {
            return false;
        }
        catch (NullPointerException e) {
            return false;
        }
    }

    static StringBuilder newStringBuilderForCollection(int size) {
        CollectPreconditions.checkNonnegative(size, "size");
        return new StringBuilder((int)Math.min((long)size * 8L, 0x40000000L));
    }
}

