package org.spongepowered.include.com.google.common.collect;

import java.util.NoSuchElementException;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.collect.UnmodifiableListIterator;

abstract class AbstractIndexedListIterator<E>
extends UnmodifiableListIterator<E> {
    private final int size;
    private int position;

    protected abstract E get(int var1);

    protected AbstractIndexedListIterator(int size, int position) {
        Preconditions.checkPositionIndex(position, size);
        this.size = size;
        this.position = position;
    }

    @Override
    public final boolean hasNext() {
        return this.position < this.size;
    }

    @Override
    public final E next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }
        return this.get(this.position++);
    }

    @Override
    public final int nextIndex() {
        return this.position;
    }

    @Override
    public final boolean hasPrevious() {
        return this.position > 0;
    }

    @Override
    public final E previous() {
        if (!this.hasPrevious()) {
            throw new NoSuchElementException();
        }
        return this.get(--this.position);
    }

    @Override
    public final int previousIndex() {
        return this.position - 1;
    }
}

