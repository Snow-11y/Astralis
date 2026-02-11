package org.spongepowered.include.com.google.common.collect;

import java.util.SortedSet;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.collect.SetMultimap;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

public interface SortedSetMultimap<K, V>
extends SetMultimap<K, V> {
    @Override
    public SortedSet<V> get(@Nullable K var1);

    @Override
    @CanIgnoreReturnValue
    public SortedSet<V> removeAll(@Nullable Object var1);
}

