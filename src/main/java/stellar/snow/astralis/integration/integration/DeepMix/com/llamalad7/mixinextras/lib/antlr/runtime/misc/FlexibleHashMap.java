package com.llamalad7.mixinextras.lib.antlr.runtime.misc;

import com.llamalad7.mixinextras.lib.antlr.runtime.misc.AbstractEqualityComparator;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.MurmurHash;
import com.llamalad7.mixinextras.lib.antlr.runtime.misc.ObjectEqualityComparator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class FlexibleHashMap<K, V>
implements Map<K, V> {
    protected final AbstractEqualityComparator<? super K> comparator;
    protected LinkedList<Entry<K, V>>[] buckets;
    protected int n = 0;
    protected int currentPrime = 1;
    protected int threshold;
    protected final int initialCapacity;
    protected final int initialBucketCapacity;

    public FlexibleHashMap(AbstractEqualityComparator<? super K> comparator) {
        this(comparator, 16, 8);
    }

    public FlexibleHashMap(AbstractEqualityComparator<? super K> comparator, int initialCapacity, int initialBucketCapacity) {
        if (comparator == null) {
            comparator = ObjectEqualityComparator.INSTANCE;
        }
        this.comparator = comparator;
        this.initialCapacity = initialCapacity;
        this.initialBucketCapacity = initialBucketCapacity;
        this.threshold = (int)Math.floor((double)initialCapacity * 0.75);
        this.buckets = FlexibleHashMap.createEntryListArray(initialBucketCapacity);
    }

    private static <K, V> LinkedList<Entry<K, V>>[] createEntryListArray(int length) {
        LinkedList[] result = new LinkedList[length];
        return result;
    }

    protected int getBucket(K key) {
        int hash = this.comparator.hashCode(key);
        int b = hash & this.buckets.length - 1;
        return b;
    }

    @Override
    public V get(Object key) {
        Object typedKey = key;
        if (key == null) {
            return null;
        }
        int b = this.getBucket(typedKey);
        LinkedList<Entry<K, V>> bucket = this.buckets[b];
        if (bucket == null) {
            return null;
        }
        for (Entry entry : bucket) {
            if (!this.comparator.equals(entry.key, typedKey)) continue;
            return entry.value;
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        int b;
        LinkedList<Entry<K, V>> bucket;
        if (key == null) {
            return null;
        }
        if (this.n > this.threshold) {
            this.expand();
        }
        if ((bucket = this.buckets[b = this.getBucket(key)]) == null) {
            this.buckets[b] = new LinkedList();
            bucket = this.buckets[b];
        }
        for (Entry entry : bucket) {
            if (!this.comparator.equals(entry.key, key)) continue;
            Object prev = entry.value;
            entry.value = value;
            ++this.n;
            return prev;
        }
        bucket.add(new Entry<K, V>(key, value));
        ++this.n;
        return null;
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        ArrayList a = new ArrayList(this.size());
        for (LinkedList<Entry<K, V>> bucket : this.buckets) {
            if (bucket == null) continue;
            for (Entry entry : bucket) {
                a.add(entry.value);
            }
        }
        return a;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        int hash = MurmurHash.initialize();
        for (LinkedList<Entry<K, V>> bucket : this.buckets) {
            Entry e;
            if (bucket == null) continue;
            Iterator iterator = bucket.iterator();
            while (iterator.hasNext() && (e = (Entry)iterator.next()) != null) {
                hash = MurmurHash.update(hash, this.comparator.hashCode(e.key));
            }
        }
        hash = MurmurHash.finish(hash, this.size());
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException();
    }

    protected void expand() {
        LinkedList<Entry<K, V>>[] old = this.buckets;
        this.currentPrime += 4;
        int newCapacity = this.buckets.length * 2;
        LinkedList<Entry<K, V>>[] newTable = FlexibleHashMap.createEntryListArray(newCapacity);
        this.buckets = newTable;
        this.threshold = (int)((double)newCapacity * 0.75);
        int oldSize = this.size();
        for (LinkedList<Entry<K, V>> bucket : old) {
            Entry e;
            if (bucket == null) continue;
            Iterator iterator = bucket.iterator();
            while (iterator.hasNext() && (e = (Entry)iterator.next()) != null) {
                this.put(e.key, e.value);
            }
        }
        this.n = oldSize;
    }

    @Override
    public int size() {
        return this.n;
    }

    @Override
    public boolean isEmpty() {
        return this.n == 0;
    }

    @Override
    public void clear() {
        this.buckets = FlexibleHashMap.createEntryListArray(this.initialCapacity);
        this.n = 0;
        this.threshold = (int)Math.floor((double)this.initialCapacity * 0.75);
    }

    public String toString() {
        if (this.size() == 0) {
            return "{}";
        }
        StringBuilder buf = new StringBuilder();
        buf.append('{');
        boolean first = true;
        for (LinkedList<Entry<K, V>> bucket : this.buckets) {
            Entry e;
            if (bucket == null) continue;
            Iterator iterator = bucket.iterator();
            while (iterator.hasNext() && (e = (Entry)iterator.next()) != null) {
                if (first) {
                    first = false;
                } else {
                    buf.append(", ");
                }
                buf.append(e.toString());
            }
        }
        buf.append('}');
        return buf.toString();
    }

    public static class Entry<K, V> {
        public final K key;
        public V value;

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public String toString() {
            return this.key.toString() + ":" + this.value.toString();
        }
    }
}

