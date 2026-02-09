package org.spongepowered.include.com.google.common.collect;

import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.collect.ImmutableMap;
import org.spongepowered.include.com.google.common.collect.ImmutableSet;
import org.spongepowered.include.com.google.common.collect.UnmodifiableIterator;
import org.spongepowered.include.com.google.j2objc.annotations.Weak;

final class ImmutableMapKeySet<K, V>
extends ImmutableSet.Indexed<K> {
    @Weak
    private final ImmutableMap<K, V> map;

    ImmutableMapKeySet(ImmutableMap<K, V> map) {
        this.map = map;
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public UnmodifiableIterator<K> iterator() {
        return this.map.keyIterator();
    }

    @Override
    public Spliterator<K> spliterator() {
        return this.map.keySpliterator();
    }

    @Override
    public boolean contains(@Nullable Object object) {
        return this.map.containsKey(object);
    }

    @Override
    K get(int index) {
        return ((Map.Entry)((ImmutableSet)this.map.entrySet()).asList().get(index)).getKey();
    }

    @Override
    public void forEach(Consumer<? super K> action) {
        Preconditions.checkNotNull(action);
        this.map.forEach((? super K k, ? super V v) -> action.accept((Object)k));
    }
}

