package org.spongepowered.include.com.google.common.collect;

import java.util.Set;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.collect.Multimap;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

public interface SetMultimap<K, V>
extends Multimap<K, V> {
    @Override
    public Set<V> get(@Nullable K var1);

    @Override
    @CanIgnoreReturnValue
    public Set<V> removeAll(@Nullable Object var1);
}

