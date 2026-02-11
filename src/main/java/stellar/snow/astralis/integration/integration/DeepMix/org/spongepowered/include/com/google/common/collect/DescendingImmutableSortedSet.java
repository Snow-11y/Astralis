package org.spongepowered.include.com.google.common.collect;

import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.collect.ImmutableSortedSet;
import org.spongepowered.include.com.google.common.collect.Ordering;
import org.spongepowered.include.com.google.common.collect.UnmodifiableIterator;

class DescendingImmutableSortedSet<E>
extends ImmutableSortedSet<E> {
    private final ImmutableSortedSet<E> forward;

    DescendingImmutableSortedSet(ImmutableSortedSet<E> forward) {
        super(Ordering.from(forward.comparator()).reverse());
        this.forward = forward;
    }

    @Override
    public boolean contains(@Nullable Object object) {
        return this.forward.contains(object);
    }

    @Override
    public int size() {
        return this.forward.size();
    }

    @Override
    public UnmodifiableIterator<E> iterator() {
        return this.forward.descendingIterator();
    }

    @Override
    ImmutableSortedSet<E> headSetImpl(E toElement, boolean inclusive) {
        return ((ImmutableSortedSet)this.forward.tailSet((Object)toElement, inclusive)).descendingSet();
    }

    @Override
    ImmutableSortedSet<E> subSetImpl(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return ((ImmutableSortedSet)this.forward.subSet((Object)toElement, toInclusive, (Object)fromElement, fromInclusive)).descendingSet();
    }

    @Override
    ImmutableSortedSet<E> tailSetImpl(E fromElement, boolean inclusive) {
        return ((ImmutableSortedSet)this.forward.headSet((Object)fromElement, inclusive)).descendingSet();
    }

    @Override
    public ImmutableSortedSet<E> descendingSet() {
        return this.forward;
    }

    @Override
    public UnmodifiableIterator<E> descendingIterator() {
        return this.forward.iterator();
    }

    @Override
    ImmutableSortedSet<E> createDescendingSet() {
        throw new AssertionError((Object)"should never be called");
    }

    @Override
    public E lower(E element) {
        return this.forward.higher(element);
    }

    @Override
    public E floor(E element) {
        return this.forward.ceiling(element);
    }

    @Override
    public E ceiling(E element) {
        return this.forward.floor(element);
    }

    @Override
    public E higher(E element) {
        return this.forward.lower(element);
    }
}

