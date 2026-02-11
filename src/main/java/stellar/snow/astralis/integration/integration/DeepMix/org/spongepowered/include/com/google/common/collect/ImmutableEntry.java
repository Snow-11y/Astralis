package org.spongepowered.include.com.google.common.collect;

import java.io.Serializable;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.collect.AbstractMapEntry;

class ImmutableEntry<K, V>
extends AbstractMapEntry<K, V>
implements Serializable {
    final K key;
    final V value;

    ImmutableEntry(@Nullable K key, @Nullable V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    @Nullable
    public final K getKey() {
        return this.key;
    }

    @Override
    @Nullable
    public final V getValue() {
        return this.value;
    }

    @Override
    public final V setValue(V value) {
        throw new UnsupportedOperationException();
    }
}

