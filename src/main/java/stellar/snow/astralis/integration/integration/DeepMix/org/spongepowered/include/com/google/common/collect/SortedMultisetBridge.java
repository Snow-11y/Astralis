package org.spongepowered.include.com.google.common.collect;

import java.util.SortedSet;
import org.spongepowered.include.com.google.common.collect.Multiset;

interface SortedMultisetBridge<E>
extends Multiset<E> {
    @Override
    public SortedSet<E> elementSet();
}

