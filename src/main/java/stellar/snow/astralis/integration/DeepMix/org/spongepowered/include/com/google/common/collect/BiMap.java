package org.spongepowered.include.com.google.common.collect;

import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

public interface BiMap<K, V>
extends Map<K, V> {
    @Override
    @Nullable
    @CanIgnoreReturnValue
    public V put(@Nullable K var1, @Nullable V var2);

    @Nullable
    @CanIgnoreReturnValue
    public V forcePut(@Nullable K var1, @Nullable V var2);

    @Override
    public Set<V> values();

    public BiMap<V, K> inverse();
}

