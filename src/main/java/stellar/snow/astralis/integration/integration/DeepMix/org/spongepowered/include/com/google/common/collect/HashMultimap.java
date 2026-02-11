package org.spongepowered.include.com.google.common.collect;

import java.util.HashMap;
import java.util.Set;
import org.spongepowered.include.com.google.common.collect.AbstractSetMultimap;
import org.spongepowered.include.com.google.common.collect.Sets;

public final class HashMultimap<K, V>
extends AbstractSetMultimap<K, V> {
    transient int expectedValuesPerKey = 2;

    public static <K, V> HashMultimap<K, V> create() {
        return new HashMultimap<K, V>();
    }

    private HashMultimap() {
        super(new HashMap());
    }

    @Override
    Set<V> createCollection() {
        return Sets.newHashSetWithExpectedSize(this.expectedValuesPerKey);
    }
}

