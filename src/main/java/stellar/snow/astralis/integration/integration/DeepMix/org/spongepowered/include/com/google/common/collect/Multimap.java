package org.spongepowered.include.com.google.common.collect;

import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.spongepowered.include.com.google.errorprone.annotations.CompatibleWith;

public interface Multimap<K, V> {
    @CanIgnoreReturnValue
    public boolean put(@Nullable K var1, @Nullable V var2);

    @CanIgnoreReturnValue
    public Collection<V> removeAll(@Nullable @CompatibleWith(value="K") Object var1);

    public Collection<V> get(@Nullable K var1);

    public Map<K, Collection<V>> asMap();
}

