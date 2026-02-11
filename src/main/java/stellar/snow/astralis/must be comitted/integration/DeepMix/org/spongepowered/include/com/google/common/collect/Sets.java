package org.spongepowered.include.com.google.common.collect;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.collect.ForwardingSortedSet;
import org.spongepowered.include.com.google.common.collect.ImmutableSortedSet;
import org.spongepowered.include.com.google.common.collect.Iterators;
import org.spongepowered.include.com.google.common.collect.Maps;
import org.spongepowered.include.com.google.common.collect.Multiset;

public final class Sets {
    public static <E> HashSet<E> newHashSet() {
        return new HashSet();
    }

    public static <E> HashSet<E> newHashSet(E ... elements) {
        HashSet<E> set = Sets.newHashSetWithExpectedSize(elements.length);
        Collections.addAll(set, elements);
        return set;
    }

    public static <E> HashSet<E> newHashSetWithExpectedSize(int expectedSize) {
        return new HashSet(Maps.capacity(expectedSize));
    }

    static int hashCodeImpl(Set<?> s) {
        int hashCode = 0;
        for (Object o : s) {
            hashCode += o != null ? o.hashCode() : 0;
            hashCode = ~(~hashCode);
        }
        return hashCode;
    }

    static boolean equalsImpl(Set<?> s, @Nullable Object object) {
        if (s == object) {
            return true;
        }
        if (object instanceof Set) {
            Set o = (Set)object;
            try {
                return s.size() == o.size() && s.containsAll(o);
            }
            catch (NullPointerException ignored) {
                return false;
            }
            catch (ClassCastException ignored) {
                return false;
            }
        }
        return false;
    }

    public static <E> NavigableSet<E> unmodifiableNavigableSet(NavigableSet<E> set) {
        if (set instanceof ImmutableSortedSet || set instanceof UnmodifiableNavigableSet) {
            return set;
        }
        return new UnmodifiableNavigableSet<E>(set);
    }

    static boolean removeAllImpl(Set<?> set, Iterator<?> iterator) {
        boolean changed = false;
        while (iterator.hasNext()) {
            changed |= set.remove(iterator.next());
        }
        return changed;
    }

    static boolean removeAllImpl(Set<?> set, Collection<?> collection) {
        Preconditions.checkNotNull(collection);
        if (collection instanceof Multiset) {
            collection = ((Multiset)collection).elementSet();
        }
        if (collection instanceof Set && collection.size() > set.size()) {
            return Iterators.removeAll(set.iterator(), collection);
        }
        return Sets.removeAllImpl(set, collection.iterator());
    }

    static final class UnmodifiableNavigableSet<E>
    extends ForwardingSortedSet<E>
    implements Serializable,
    NavigableSet<E> {
        private final NavigableSet<E> delegate;
        private transient UnmodifiableNavigableSet<E> descendingSet;

        UnmodifiableNavigableSet(NavigableSet<E> delegate) {
            this.delegate = Preconditions.checkNotNull(delegate);
        }

        @Override
        protected SortedSet<E> delegate() {
            return Collections.unmodifiableSortedSet(this.delegate);
        }

        @Override
        public E lower(E e) {
            return this.delegate.lower(e);
        }

        @Override
        public E floor(E e) {
            return this.delegate.floor(e);
        }

        @Override
        public E ceiling(E e) {
            return this.delegate.ceiling(e);
        }

        @Override
        public E higher(E e) {
            return this.delegate.higher(e);
        }

        @Override
        public E pollFirst() {
            throw new UnsupportedOperationException();
        }

        @Override
        public E pollLast() {
            throw new UnsupportedOperationException();
        }

        @Override
        public NavigableSet<E> descendingSet() {
            UnmodifiableNavigableSet<E> result = this.descendingSet;
            if (result == null) {
                result = this.descendingSet = new UnmodifiableNavigableSet<E>(this.delegate.descendingSet());
                result.descendingSet = this;
            }
            return result;
        }

        @Override
        public Iterator<E> descendingIterator() {
            return Iterators.unmodifiableIterator(this.delegate.descendingIterator());
        }

        @Override
        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            return Sets.unmodifiableNavigableSet(this.delegate.subSet(fromElement, fromInclusive, toElement, toInclusive));
        }

        @Override
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return Sets.unmodifiableNavigableSet(this.delegate.headSet(toElement, inclusive));
        }

        @Override
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return Sets.unmodifiableNavigableSet(this.delegate.tailSet(fromElement, inclusive));
        }
    }

    static abstract class ImprovedAbstractSet<E>
    extends AbstractSet<E> {
        ImprovedAbstractSet() {
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return Sets.removeAllImpl(this, c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return super.retainAll(Preconditions.checkNotNull(c));
        }
    }
}

