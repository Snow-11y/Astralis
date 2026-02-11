package org.spongepowered.include.com.google.common.collect;

import java.util.NavigableSet;
import org.spongepowered.include.com.google.common.collect.SortedIterable;
import org.spongepowered.include.com.google.common.collect.SortedMultisetBridge;

public interface SortedMultiset<E>
extends SortedIterable<E>,
SortedMultisetBridge<E> {
    @Override
    public NavigableSet<E> elementSet();
}

