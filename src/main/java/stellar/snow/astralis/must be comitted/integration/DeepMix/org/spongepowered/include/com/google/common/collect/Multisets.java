package org.spongepowered.include.com.google.common.collect;

import java.util.Collections;
import java.util.Spliterator;
import org.spongepowered.include.com.google.common.collect.CollectSpliterators;
import org.spongepowered.include.com.google.common.collect.Multiset;
import org.spongepowered.include.com.google.common.collect.Ordering;
import org.spongepowered.include.com.google.common.primitives.Ints;

public final class Multisets {
    private static final Ordering<Multiset.Entry<?>> DECREASING_COUNT_ORDERING = new Ordering<Multiset.Entry<?>>(){

        @Override
        public int compare(Multiset.Entry<?> entry1, Multiset.Entry<?> entry2) {
            return Ints.compare(entry2.getCount(), entry1.getCount());
        }
    };

    static <E> Spliterator<E> spliteratorImpl(Multiset<E> multiset) {
        Spliterator<Multiset.Entry<E>> entrySpliterator = multiset.entrySet().spliterator();
        return CollectSpliterators.flatMap(entrySpliterator, entry -> Collections.nCopies(entry.getCount(), entry.getElement()).spliterator(), 0x40 | entrySpliterator.characteristics() & 0x510, multiset.size());
    }
}

