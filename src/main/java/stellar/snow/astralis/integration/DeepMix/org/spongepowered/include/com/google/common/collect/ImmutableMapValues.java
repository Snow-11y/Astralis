package org.spongepowered.include.com.google.common.collect;

import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.collect.CollectSpliterators;
import org.spongepowered.include.com.google.common.collect.ImmutableAsList;
import org.spongepowered.include.com.google.common.collect.ImmutableCollection;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.include.com.google.common.collect.ImmutableMap;
import org.spongepowered.include.com.google.common.collect.ImmutableSet;
import org.spongepowered.include.com.google.common.collect.Iterators;
import org.spongepowered.include.com.google.common.collect.UnmodifiableIterator;
import org.spongepowered.include.com.google.j2objc.annotations.Weak;

final class ImmutableMapValues<K, V>
extends ImmutableCollection<V> {
    @Weak
    private final ImmutableMap<K, V> map;

    ImmutableMapValues(ImmutableMap<K, V> map) {
        this.map = map;
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public UnmodifiableIterator<V> iterator() {
        return new UnmodifiableIterator<V>(){
            final UnmodifiableIterator<Map.Entry<K, V>> entryItr;
            {
                this.entryItr = ((ImmutableSet)ImmutableMapValues.this.map.entrySet()).iterator();
            }

            @Override
            public boolean hasNext() {
                return this.entryItr.hasNext();
            }

            @Override
            public V next() {
                return ((Map.Entry)this.entryItr.next()).getValue();
            }
        };
    }

    @Override
    public Spliterator<V> spliterator() {
        return CollectSpliterators.map(((ImmutableCollection)((Object)this.map.entrySet())).spliterator(), Map.Entry::getValue);
    }

    @Override
    public boolean contains(@Nullable Object object) {
        return object != null && Iterators.contains(this.iterator(), object);
    }

    @Override
    public ImmutableList<V> asList() {
        final ImmutableList entryList = ((ImmutableSet)this.map.entrySet()).asList();
        return new ImmutableAsList<V>(){

            @Override
            public V get(int index) {
                return ((Map.Entry)entryList.get(index)).getValue();
            }

            @Override
            ImmutableCollection<V> delegateCollection() {
                return ImmutableMapValues.this;
            }
        };
    }

    @Override
    public void forEach(Consumer<? super V> action) {
        Preconditions.checkNotNull(action);
        this.map.forEach((? super K k, ? super V v) -> action.accept((Object)v));
    }
}

