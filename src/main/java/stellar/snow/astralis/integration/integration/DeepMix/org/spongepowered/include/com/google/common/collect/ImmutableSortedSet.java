package org.spongepowered.include.com.google.common.collect;

import java.util.Comparator;
import java.util.NavigableSet;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.collect.DescendingImmutableSortedSet;
import org.spongepowered.include.com.google.common.collect.ImmutableSortedSetFauxverideShim;
import org.spongepowered.include.com.google.common.collect.Iterables;
import org.spongepowered.include.com.google.common.collect.Iterators;
import org.spongepowered.include.com.google.common.collect.SortedIterable;
import org.spongepowered.include.com.google.common.collect.UnmodifiableIterator;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.spongepowered.include.com.google.errorprone.annotations.concurrent.LazyInit;

public abstract class ImmutableSortedSet<E>
extends ImmutableSortedSetFauxverideShim<E>
implements NavigableSet<E>,
SortedIterable<E> {
    final transient Comparator<? super E> comparator;
    @LazyInit
    transient ImmutableSortedSet<E> descendingSet;

    ImmutableSortedSet(Comparator<? super E> comparator) {
        this.comparator = comparator;
    }

    @Override
    public Comparator<? super E> comparator() {
        return this.comparator;
    }

    @Override
    public abstract UnmodifiableIterator<E> iterator();

    @Override
    public ImmutableSortedSet<E> headSet(E toElement) {
        return this.headSet((Object)toElement, false);
    }

    @Override
    public ImmutableSortedSet<E> headSet(E toElement, boolean inclusive) {
        return this.headSetImpl(Preconditions.checkNotNull(toElement), inclusive);
    }

    @Override
    public ImmutableSortedSet<E> subSet(E fromElement, E toElement) {
        return this.subSet((Object)fromElement, true, (Object)toElement, false);
    }

    @Override
    public ImmutableSortedSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        Preconditions.checkNotNull(fromElement);
        Preconditions.checkNotNull(toElement);
        Preconditions.checkArgument(this.comparator.compare(fromElement, toElement) <= 0);
        return this.subSetImpl(fromElement, fromInclusive, toElement, toInclusive);
    }

    @Override
    public ImmutableSortedSet<E> tailSet(E fromElement) {
        return this.tailSet((Object)fromElement, true);
    }

    @Override
    public ImmutableSortedSet<E> tailSet(E fromElement, boolean inclusive) {
        return this.tailSetImpl(Preconditions.checkNotNull(fromElement), inclusive);
    }

    abstract ImmutableSortedSet<E> headSetImpl(E var1, boolean var2);

    abstract ImmutableSortedSet<E> subSetImpl(E var1, boolean var2, E var3, boolean var4);

    abstract ImmutableSortedSet<E> tailSetImpl(E var1, boolean var2);

    @Override
    public E lower(E e) {
        return Iterators.getNext(((ImmutableSortedSet)this.headSet((Object)e, false)).descendingIterator(), null);
    }

    @Override
    public E floor(E e) {
        return Iterators.getNext(((ImmutableSortedSet)this.headSet((Object)e, true)).descendingIterator(), null);
    }

    @Override
    public E ceiling(E e) {
        return Iterables.getFirst(this.tailSet((Object)e, true), null);
    }

    @Override
    public E higher(E e) {
        return Iterables.getFirst(this.tailSet((Object)e, false), null);
    }

    @Override
    public E first() {
        return this.iterator().next();
    }

    @Override
    public E last() {
        return this.descendingIterator().next();
    }

    @Override
    @Deprecated
    @CanIgnoreReturnValue
    public final E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    @CanIgnoreReturnValue
    public final E pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImmutableSortedSet<E> descendingSet() {
        ImmutableSortedSet<E> result = this.descendingSet;
        if (result == null) {
            result = this.descendingSet = this.createDescendingSet();
            result.descendingSet = this;
        }
        return result;
    }

    ImmutableSortedSet<E> createDescendingSet() {
        return new DescendingImmutableSortedSet(this);
    }

    @Override
    public Spliterator<E> spliterator() {
        return new Spliterators.AbstractSpliterator<E>(this.size(), 1365){
            final UnmodifiableIterator<E> iterator;
            {
                this.iterator = ImmutableSortedSet.this.iterator();
            }

            @Override
            public boolean tryAdvance(Consumer<? super E> action) {
                if (this.iterator.hasNext()) {
                    action.accept(this.iterator.next());
                    return true;
                }
                return false;
            }

            @Override
            public Comparator<? super E> getComparator() {
                return ImmutableSortedSet.this.comparator;
            }
        };
    }

    @Override
    public abstract UnmodifiableIterator<E> descendingIterator();
}

