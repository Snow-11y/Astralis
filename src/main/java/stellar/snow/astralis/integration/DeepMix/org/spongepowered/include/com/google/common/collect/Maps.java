package org.spongepowered.include.com.google.common.collect;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Function;
import org.spongepowered.include.com.google.common.base.Joiner;
import org.spongepowered.include.com.google.common.base.Objects;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.collect.CollectPreconditions;
import org.spongepowered.include.com.google.common.collect.Collections2;
import org.spongepowered.include.com.google.common.collect.ImmutableEntry;
import org.spongepowered.include.com.google.common.collect.Iterators;
import org.spongepowered.include.com.google.common.collect.Sets;
import org.spongepowered.include.com.google.j2objc.annotations.Weak;

public final class Maps {
    static final Joiner.MapJoiner STANDARD_JOINER = Collections2.STANDARD_JOINER.withKeyValueSeparator("=");

    static <K> Function<Map.Entry<K, ?>, K> keyFunction() {
        return EntryFunction.KEY;
    }

    static <V> Function<Map.Entry<?, V>, V> valueFunction() {
        return EntryFunction.VALUE;
    }

    static <K, V> Iterator<K> keyIterator(Iterator<Map.Entry<K, V>> entryIterator) {
        return Iterators.transform(entryIterator, Maps.keyFunction());
    }

    static <K, V> Iterator<V> valueIterator(Iterator<Map.Entry<K, V>> entryIterator) {
        return Iterators.transform(entryIterator, Maps.valueFunction());
    }

    static int capacity(int expectedSize) {
        if (expectedSize < 3) {
            CollectPreconditions.checkNonnegative(expectedSize, "expectedSize");
            return expectedSize + 1;
        }
        if (expectedSize < 0x40000000) {
            return (int)((float)expectedSize / 0.75f + 1.0f);
        }
        return Integer.MAX_VALUE;
    }

    public static <K extends Comparable, V> TreeMap<K, V> newTreeMap() {
        return new TreeMap();
    }

    public static <K, V> Map.Entry<K, V> immutableEntry(@Nullable K key, @Nullable V value) {
        return new ImmutableEntry<K, V>(key, value);
    }

    static <V> V safeGet(Map<?, V> map, @Nullable Object key) {
        Preconditions.checkNotNull(map);
        try {
            return map.get(key);
        }
        catch (ClassCastException e) {
            return null;
        }
        catch (NullPointerException e) {
            return null;
        }
    }

    static boolean safeContainsKey(Map<?, ?> map, Object key) {
        Preconditions.checkNotNull(map);
        try {
            return map.containsKey(key);
        }
        catch (ClassCastException e) {
            return false;
        }
        catch (NullPointerException e) {
            return false;
        }
    }

    static <V> V safeRemove(Map<?, V> map, Object key) {
        Preconditions.checkNotNull(map);
        try {
            return map.remove(key);
        }
        catch (ClassCastException e) {
            return null;
        }
        catch (NullPointerException e) {
            return null;
        }
    }

    static boolean equalsImpl(Map<?, ?> map, Object object) {
        if (map == object) {
            return true;
        }
        if (object instanceof Map) {
            Map o = (Map)object;
            return map.entrySet().equals(o.entrySet());
        }
        return false;
    }

    static String toStringImpl(Map<?, ?> map) {
        StringBuilder sb = Collections2.newStringBuilderForCollection(map.size()).append('{');
        STANDARD_JOINER.appendTo(sb, map);
        return sb.append('}').toString();
    }

    @Nullable
    static <K> K keyOrNull(@Nullable Map.Entry<K, ?> entry) {
        return entry == null ? null : (K)entry.getKey();
    }

    @Nullable
    static <V> V valueOrNull(@Nullable Map.Entry<?, V> entry) {
        return entry == null ? null : (V)entry.getValue();
    }

    static abstract class EntrySet<K, V>
    extends Sets.ImprovedAbstractSet<Map.Entry<K, V>> {
        EntrySet() {
        }

        abstract Map<K, V> map();

        @Override
        public int size() {
            return this.map().size();
        }

        @Override
        public void clear() {
            this.map().clear();
        }

        @Override
        public boolean contains(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry entry = (Map.Entry)o;
                Object key = entry.getKey();
                V value = Maps.safeGet(this.map(), key);
                return Objects.equal(value, entry.getValue()) && (value != null || this.map().containsKey(key));
            }
            return false;
        }

        @Override
        public boolean isEmpty() {
            return this.map().isEmpty();
        }

        @Override
        public boolean remove(Object o) {
            if (this.contains(o)) {
                Map.Entry entry = (Map.Entry)o;
                return this.map().keySet().remove(entry.getKey());
            }
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            try {
                return super.removeAll(Preconditions.checkNotNull(c));
            }
            catch (UnsupportedOperationException e) {
                return Sets.removeAllImpl(this, c.iterator());
            }
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            try {
                return super.retainAll(Preconditions.checkNotNull(c));
            }
            catch (UnsupportedOperationException e) {
                HashSet keys = Sets.newHashSetWithExpectedSize(c.size());
                for (Object o : c) {
                    if (!this.contains(o)) continue;
                    Map.Entry entry = (Map.Entry)o;
                    keys.add(entry.getKey());
                }
                return this.map().keySet().retainAll(keys);
            }
        }
    }

    static class Values<K, V>
    extends AbstractCollection<V> {
        @Weak
        final Map<K, V> map;

        Values(Map<K, V> map) {
            this.map = Preconditions.checkNotNull(map);
        }

        final Map<K, V> map() {
            return this.map;
        }

        @Override
        public Iterator<V> iterator() {
            return Maps.valueIterator(this.map().entrySet().iterator());
        }

        @Override
        public void forEach(Consumer<? super V> action) {
            Preconditions.checkNotNull(action);
            this.map.forEach((? super K k, ? super V v) -> action.accept((Object)v));
        }

        @Override
        public boolean remove(Object o) {
            try {
                return super.remove(o);
            }
            catch (UnsupportedOperationException e) {
                for (Map.Entry<K, V> entry : this.map().entrySet()) {
                    if (!Objects.equal(o, entry.getValue())) continue;
                    this.map().remove(entry.getKey());
                    return true;
                }
                return false;
            }
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            try {
                return super.removeAll(Preconditions.checkNotNull(c));
            }
            catch (UnsupportedOperationException e) {
                HashSet<K> toRemove = Sets.newHashSet();
                for (Map.Entry<K, V> entry : this.map().entrySet()) {
                    if (!c.contains(entry.getValue())) continue;
                    toRemove.add(entry.getKey());
                }
                return this.map().keySet().removeAll(toRemove);
            }
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            try {
                return super.retainAll(Preconditions.checkNotNull(c));
            }
            catch (UnsupportedOperationException e) {
                HashSet<K> toRetain = Sets.newHashSet();
                for (Map.Entry<K, V> entry : this.map().entrySet()) {
                    if (!c.contains(entry.getValue())) continue;
                    toRetain.add(entry.getKey());
                }
                return this.map().keySet().retainAll(toRetain);
            }
        }

        @Override
        public int size() {
            return this.map().size();
        }

        @Override
        public boolean isEmpty() {
            return this.map().isEmpty();
        }

        @Override
        public boolean contains(@Nullable Object o) {
            return this.map().containsValue(o);
        }

        @Override
        public void clear() {
            this.map().clear();
        }
    }

    static class KeySet<K, V>
    extends Sets.ImprovedAbstractSet<K> {
        @Weak
        final Map<K, V> map;

        KeySet(Map<K, V> map) {
            this.map = Preconditions.checkNotNull(map);
        }

        Map<K, V> map() {
            return this.map;
        }

        @Override
        public Iterator<K> iterator() {
            return Maps.keyIterator(this.map().entrySet().iterator());
        }

        @Override
        public void forEach(Consumer<? super K> action) {
            Preconditions.checkNotNull(action);
            this.map.forEach((? super K k, ? super V v) -> action.accept((Object)k));
        }

        @Override
        public int size() {
            return this.map().size();
        }

        @Override
        public boolean isEmpty() {
            return this.map().isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return this.map().containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            if (this.contains(o)) {
                this.map().remove(o);
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            this.map().clear();
        }
    }

    static abstract class IteratorBasedAbstractMap<K, V>
    extends AbstractMap<K, V> {
        IteratorBasedAbstractMap() {
        }

        @Override
        public abstract int size();

        abstract Iterator<Map.Entry<K, V>> entryIterator();

        Spliterator<Map.Entry<K, V>> entrySpliterator() {
            return Spliterators.spliterator(this.entryIterator(), (long)this.size(), 65);
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return new EntrySet<K, V>(){

                @Override
                Map<K, V> map() {
                    return this;
                }

                @Override
                public Iterator<Map.Entry<K, V>> iterator() {
                    return this.entryIterator();
                }

                @Override
                public Spliterator<Map.Entry<K, V>> spliterator() {
                    return this.entrySpliterator();
                }

                @Override
                public void forEach(Consumer<? super Map.Entry<K, V>> action) {
                    this.forEachEntry(action);
                }
            };
        }

        void forEachEntry(Consumer<? super Map.Entry<K, V>> action) {
            this.entryIterator().forEachRemaining(action);
        }

        @Override
        public void clear() {
            Iterators.clear(this.entryIterator());
        }
    }

    static abstract class ViewCachingAbstractMap<K, V>
    extends AbstractMap<K, V> {
        private transient Set<Map.Entry<K, V>> entrySet;
        private transient Set<K> keySet;
        private transient Collection<V> values;

        ViewCachingAbstractMap() {
        }

        abstract Set<Map.Entry<K, V>> createEntrySet();

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            Set<Map.Entry<K, V>> result = this.entrySet;
            return result == null ? (this.entrySet = this.createEntrySet()) : result;
        }

        @Override
        public Set<K> keySet() {
            Set<K> result = this.keySet;
            return result == null ? (this.keySet = this.createKeySet()) : result;
        }

        Set<K> createKeySet() {
            return new KeySet(this);
        }

        @Override
        public Collection<V> values() {
            Collection<V> result = this.values;
            return result == null ? (this.values = this.createValues()) : result;
        }

        Collection<V> createValues() {
            return new Values(this);
        }
    }

    private static enum EntryFunction implements Function<Map.Entry<?, ?>, Object>
    {
        KEY{

            @Override
            @Nullable
            public Object apply(Map.Entry<?, ?> entry) {
                return entry.getKey();
            }
        }
        ,
        VALUE{

            @Override
            @Nullable
            public Object apply(Map.Entry<?, ?> entry) {
                return entry.getValue();
            }
        };

    }
}

