package org.spongepowered.include.com.google.common.collect;

import java.util.Map;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.collect.CollectPreconditions;
import org.spongepowered.include.com.google.common.collect.ImmutableBiMap;
import org.spongepowered.include.com.google.common.collect.ImmutableSet;
import org.spongepowered.include.com.google.common.collect.Maps;
import org.spongepowered.include.com.google.errorprone.annotations.concurrent.LazyInit;
import org.spongepowered.include.com.google.j2objc.annotations.RetainedWith;

final class SingletonImmutableBiMap<K, V>
extends ImmutableBiMap<K, V> {
    final transient K singleKey;
    final transient V singleValue;
    @LazyInit
    @RetainedWith
    transient ImmutableBiMap<V, K> inverse;

    SingletonImmutableBiMap(K singleKey, V singleValue) {
        CollectPreconditions.checkEntryNotNull(singleKey, singleValue);
        this.singleKey = singleKey;
        this.singleValue = singleValue;
    }

    private SingletonImmutableBiMap(K singleKey, V singleValue, ImmutableBiMap<V, K> inverse) {
        this.singleKey = singleKey;
        this.singleValue = singleValue;
        this.inverse = inverse;
    }

    @Override
    public V get(@Nullable Object key) {
        return this.singleKey.equals(key) ? (V)this.singleValue : null;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Preconditions.checkNotNull(action).accept(this.singleKey, this.singleValue);
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        return this.singleKey.equals(key);
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        return this.singleValue.equals(value);
    }

    @Override
    ImmutableSet<Map.Entry<K, V>> createEntrySet() {
        return ImmutableSet.of(Maps.immutableEntry(this.singleKey, this.singleValue));
    }

    @Override
    ImmutableSet<K> createKeySet() {
        return ImmutableSet.of(this.singleKey);
    }

    @Override
    public ImmutableBiMap<V, K> inverse() {
        ImmutableBiMap<V, K> result = this.inverse;
        if (result == null) {
            this.inverse = new SingletonImmutableBiMap<K, V>(this.singleValue, this.singleKey, this);
            return this.inverse;
        }
        return result;
    }
}

