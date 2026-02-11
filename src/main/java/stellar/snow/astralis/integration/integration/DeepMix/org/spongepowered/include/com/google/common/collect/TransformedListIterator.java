package org.spongepowered.include.com.google.common.collect;

import java.util.ListIterator;
import org.spongepowered.include.com.google.common.collect.Iterators;
import org.spongepowered.include.com.google.common.collect.TransformedIterator;

abstract class TransformedListIterator<F, T>
extends TransformedIterator<F, T>
implements ListIterator<T> {
    TransformedListIterator(ListIterator<? extends F> backingIterator) {
        super(backingIterator);
    }

    private ListIterator<? extends F> backingIterator() {
        return Iterators.cast(this.backingIterator);
    }

    @Override
    public final boolean hasPrevious() {
        return this.backingIterator().hasPrevious();
    }

    @Override
    public final T previous() {
        return this.transform(this.backingIterator().previous());
    }

    @Override
    public final int nextIndex() {
        return this.backingIterator().nextIndex();
    }

    @Override
    public final int previousIndex() {
        return this.backingIterator().previousIndex();
    }

    @Override
    public void set(T element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(T element) {
        throw new UnsupportedOperationException();
    }
}

