package org.spongepowered.include.com.google.common.collect;

import java.util.Spliterator;
import java.util.Spliterators;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.collect.Hashing;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.include.com.google.common.collect.ImmutableSet;
import org.spongepowered.include.com.google.common.collect.ObjectArrays;
import org.spongepowered.include.com.google.common.collect.RegularImmutableAsList;

final class RegularImmutableSet<E>
extends ImmutableSet.Indexed<E> {
    static final RegularImmutableSet<Object> EMPTY = new RegularImmutableSet(ObjectArrays.EMPTY_ARRAY, 0, null, 0);
    private final transient Object[] elements;
    final transient Object[] table;
    private final transient int mask;
    private final transient int hashCode;

    RegularImmutableSet(Object[] elements, int hashCode, Object[] table, int mask) {
        this.elements = elements;
        this.table = table;
        this.mask = mask;
        this.hashCode = hashCode;
    }

    @Override
    public boolean contains(@Nullable Object target) {
        Object[] table = this.table;
        if (target == null || table == null) {
            return false;
        }
        int i = Hashing.smearedHash(target);
        Object candidate;
        while ((candidate = table[i &= this.mask]) != null) {
            if (candidate.equals(target)) {
                return true;
            }
            ++i;
        }
        return false;
    }

    @Override
    public int size() {
        return this.elements.length;
    }

    @Override
    E get(int i) {
        return (E)this.elements[i];
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this.elements, 1297);
    }

    @Override
    int copyIntoArray(Object[] dst, int offset) {
        System.arraycopy(this.elements, 0, dst, offset, this.elements.length);
        return offset + this.elements.length;
    }

    @Override
    ImmutableList<E> createAsList() {
        return this.table == null ? ImmutableList.of() : new RegularImmutableAsList(this, this.elements);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    boolean isHashCodeFast() {
        return true;
    }
}

