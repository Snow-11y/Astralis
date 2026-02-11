package org.spongepowered.include.com.google.common.collect;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.collect.AbstractMapBasedMultimap;
import org.spongepowered.include.com.google.common.collect.ImmutableSet;
import org.spongepowered.include.com.google.common.collect.SetMultimap;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

abstract class AbstractSetMultimap<K, V>
extends AbstractMapBasedMultimap<K, V>
implements SetMultimap<K, V> {
    protected AbstractSetMultimap(Map<K, Collection<V>> map) {
        super(map);
    }

    @Override
    abstract Set<V> createCollection();

    @Override
    Set<V> createUnmodifiableEmptyCollection() {
        return ImmutableSet.of();
    }

    @Override
    public Set<V> get(@Nullable K key) {
        return (Set)super.get(key);
    }

    @Override
    @CanIgnoreReturnValue
    public Set<V> removeAll(@Nullable Object key) {
        return (Set)super.removeAll(key);
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        return super.asMap();
    }

    @Override
    @CanIgnoreReturnValue
    public boolean put(@Nullable K key, @Nullable V value) {
        return super.put(key, value);
    }

    @Override
    public boolean equals(@Nullable Object object) {
        return super.equals(object);
    }
}

