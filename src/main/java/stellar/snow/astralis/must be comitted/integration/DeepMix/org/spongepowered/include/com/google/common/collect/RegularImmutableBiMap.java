package org.spongepowered.include.com.google.common.collect;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.collect.Hashing;
import org.spongepowered.include.com.google.common.collect.ImmutableAsList;
import org.spongepowered.include.com.google.common.collect.ImmutableBiMap;
import org.spongepowered.include.com.google.common.collect.ImmutableCollection;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.include.com.google.common.collect.ImmutableMap;
import org.spongepowered.include.com.google.common.collect.ImmutableMapEntry;
import org.spongepowered.include.com.google.common.collect.ImmutableMapEntrySet;
import org.spongepowered.include.com.google.common.collect.ImmutableSet;
import org.spongepowered.include.com.google.common.collect.Maps;
import org.spongepowered.include.com.google.common.collect.RegularImmutableMap;
import org.spongepowered.include.com.google.common.collect.UnmodifiableIterator;
import org.spongepowered.include.com.google.errorprone.annotations.concurrent.LazyInit;
import org.spongepowered.include.com.google.j2objc.annotations.RetainedWith;

class RegularImmutableBiMap<K, V>
extends ImmutableBiMap<K, V> {
    static final RegularImmutableBiMap<Object, Object> EMPTY = new RegularImmutableBiMap(null, null, ImmutableMap.EMPTY_ENTRY_ARRAY, 0, 0);
    private final transient ImmutableMapEntry<K, V>[] keyTable;
    private final transient ImmutableMapEntry<K, V>[] valueTable;
    private final transient Map.Entry<K, V>[] entries;
    private final transient int mask;
    private final transient int hashCode;
    @LazyInit
    @RetainedWith
    private transient ImmutableBiMap<V, K> inverse;

    private RegularImmutableBiMap(ImmutableMapEntry<K, V>[] keyTable, ImmutableMapEntry<K, V>[] valueTable, Map.Entry<K, V>[] entries, int mask, int hashCode) {
        this.keyTable = keyTable;
        this.valueTable = valueTable;
        this.entries = entries;
        this.mask = mask;
        this.hashCode = hashCode;
    }

    @Override
    @Nullable
    public V get(@Nullable Object key) {
        return this.keyTable == null ? null : (V)RegularImmutableMap.get(key, this.keyTable, this.mask);
    }

    @Override
    ImmutableSet<Map.Entry<K, V>> createEntrySet() {
        return this.isEmpty() ? ImmutableSet.of() : new ImmutableMapEntrySet.RegularEntrySet<K, V>(this, this.entries);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Preconditions.checkNotNull(action);
        for (Map.Entry<K, V> entry : this.entries) {
            action.accept(entry.getKey(), entry.getValue());
        }
    }

    @Override
    boolean isHashCodeFast() {
        return true;
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public int size() {
        return this.entries.length;
    }

    @Override
    public ImmutableBiMap<V, K> inverse() {
        if (this.isEmpty()) {
            return ImmutableBiMap.of();
        }
        Inverse result = this.inverse;
        return result == null ? (this.inverse = new Inverse()) : result;
    }

    private final class Inverse
    extends ImmutableBiMap<V, K> {
        private Inverse() {
        }

        @Override
        public int size() {
            return this.inverse().size();
        }

        @Override
        public ImmutableBiMap<K, V> inverse() {
            return RegularImmutableBiMap.this;
        }

        @Override
        public void forEach(BiConsumer<? super V, ? super K> action) {
            Preconditions.checkNotNull(action);
            RegularImmutableBiMap.this.forEach((? super K k, ? super V v) -> action.accept((Object)v, (Object)k));
        }

        @Override
        public K get(@Nullable Object value) {
            if (value == null || RegularImmutableBiMap.this.valueTable == null) {
                return null;
            }
            int bucket = Hashing.smear(value.hashCode()) & RegularImmutableBiMap.this.mask;
            for (ImmutableMapEntry entry = RegularImmutableBiMap.this.valueTable[bucket]; entry != null; entry = entry.getNextInValueBucket()) {
                if (!value.equals(entry.getValue())) continue;
                return entry.getKey();
            }
            return null;
        }

        @Override
        ImmutableSet<Map.Entry<V, K>> createEntrySet() {
            return new InverseEntrySet();
        }

        final class InverseEntrySet
        extends ImmutableMapEntrySet<V, K> {
            InverseEntrySet() {
            }

            @Override
            ImmutableMap<V, K> map() {
                return Inverse.this;
            }

            @Override
            boolean isHashCodeFast() {
                return true;
            }

            @Override
            public int hashCode() {
                return RegularImmutableBiMap.this.hashCode;
            }

            @Override
            public UnmodifiableIterator<Map.Entry<V, K>> iterator() {
                return this.asList().iterator();
            }

            @Override
            public void forEach(Consumer<? super Map.Entry<V, K>> action) {
                this.asList().forEach(action);
            }

            @Override
            ImmutableList<Map.Entry<V, K>> createAsList() {
                return new ImmutableAsList<Map.Entry<V, K>>(){

                    @Override
                    public Map.Entry<V, K> get(int index) {
                        Map.Entry entry = RegularImmutableBiMap.this.entries[index];
                        return Maps.immutableEntry(entry.getValue(), entry.getKey());
                    }

                    @Override
                    ImmutableCollection<Map.Entry<V, K>> delegateCollection() {
                        return InverseEntrySet.this;
                    }
                };
            }
        }
    }
}

