package org.spongepowered.include.com.google.common.collect;

import org.spongepowered.include.com.google.common.collect.BiMap;
import org.spongepowered.include.com.google.common.collect.ImmutableBiMapFauxverideShim;
import org.spongepowered.include.com.google.common.collect.ImmutableMap;
import org.spongepowered.include.com.google.common.collect.ImmutableSet;
import org.spongepowered.include.com.google.common.collect.RegularImmutableBiMap;
import org.spongepowered.include.com.google.common.collect.SingletonImmutableBiMap;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

public abstract class ImmutableBiMap<K, V>
extends ImmutableBiMapFauxverideShim<K, V>
implements BiMap<K, V> {
    public static <K, V> ImmutableBiMap<K, V> of() {
        return RegularImmutableBiMap.EMPTY;
    }

    public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1) {
        return new SingletonImmutableBiMap<K, V>(k1, v1);
    }

    ImmutableBiMap() {
    }

    @Override
    public abstract ImmutableBiMap<V, K> inverse();

    @Override
    public ImmutableSet<V> values() {
        return ((ImmutableMap)((Object)this.inverse())).keySet();
    }

    @Override
    @Deprecated
    @CanIgnoreReturnValue
    public V forcePut(K key, V value) {
        throw new UnsupportedOperationException();
    }
}

