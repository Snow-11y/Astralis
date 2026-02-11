package org.spongepowered.include.com.google.common.collect;

import java.util.Iterator;
import org.spongepowered.include.com.google.common.base.Optional;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.include.com.google.common.collect.Iterables;
import org.spongepowered.include.com.google.common.collect.Iterators;

public abstract class FluentIterable<E>
implements Iterable<E> {
    private final Optional<Iterable<E>> iterableDelegate = Optional.absent();

    protected FluentIterable() {
    }

    private Iterable<E> getDelegate() {
        return this.iterableDelegate.or(this);
    }

    public static <T> FluentIterable<T> concat(Iterable<? extends T> a, Iterable<? extends T> b) {
        return FluentIterable.concat(ImmutableList.of(a, b));
    }

    public static <T> FluentIterable<T> concat(final Iterable<? extends Iterable<? extends T>> inputs) {
        Preconditions.checkNotNull(inputs);
        return new FluentIterable<T>(){

            @Override
            public Iterator<T> iterator() {
                return Iterators.concat(Iterables.transform(inputs, Iterables.toIterator()).iterator());
            }
        };
    }

    public String toString() {
        return Iterables.toString(this.getDelegate());
    }
}

