package org.spongepowered.include.com.google.common.collect;

import java.util.Collection;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.collect.Multisets;

public interface Multiset<E>
extends Collection<E> {
    @Override
    public int size();

    public Set<E> elementSet();

    public Set<Entry<E>> entrySet();

    @Override
    default public void forEach(Consumer<? super E> action) {
        Preconditions.checkNotNull(action);
        this.entrySet().forEach((? super T entry) -> {
            Object elem = entry.getElement();
            int count = entry.getCount();
            for (int i = 0; i < count; ++i) {
                action.accept((Object)elem);
            }
        });
    }

    @Override
    default public Spliterator<E> spliterator() {
        return Multisets.spliteratorImpl(this);
    }

    public static interface Entry<E> {
        public E getElement();

        public int getCount();
    }
}

