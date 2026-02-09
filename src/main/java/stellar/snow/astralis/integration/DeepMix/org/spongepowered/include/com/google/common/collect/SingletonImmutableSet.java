package org.spongepowered.include.com.google.common.collect;

import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.include.com.google.common.collect.ImmutableSet;
import org.spongepowered.include.com.google.common.collect.Iterators;
import org.spongepowered.include.com.google.common.collect.UnmodifiableIterator;
import org.spongepowered.include.com.google.errorprone.annotations.concurrent.LazyInit;

final class SingletonImmutableSet<E>
extends ImmutableSet<E> {
    final transient E element;
    @LazyInit
    private transient int cachedHashCode;

    SingletonImmutableSet(E element) {
        this.element = Preconditions.checkNotNull(element);
    }

    SingletonImmutableSet(E element, int hashCode) {
        this.element = element;
        this.cachedHashCode = hashCode;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean contains(Object target) {
        return this.element.equals(target);
    }

    @Override
    public UnmodifiableIterator<E> iterator() {
        return Iterators.singletonIterator(this.element);
    }

    @Override
    ImmutableList<E> createAsList() {
        return ImmutableList.of(this.element);
    }

    @Override
    int copyIntoArray(Object[] dst, int offset) {
        dst[offset] = this.element;
        return offset + 1;
    }

    @Override
    public final int hashCode() {
        int code = this.cachedHashCode;
        if (code == 0) {
            this.cachedHashCode = code = this.element.hashCode();
        }
        return code;
    }

    @Override
    boolean isHashCodeFast() {
        return this.cachedHashCode != 0;
    }

    @Override
    public String toString() {
        return '[' + this.element.toString() + ']';
    }
}

