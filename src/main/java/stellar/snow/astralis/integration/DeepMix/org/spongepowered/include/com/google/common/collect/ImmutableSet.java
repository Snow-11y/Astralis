package org.spongepowered.include.com.google.common.collect;

import java.util.Arrays;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.collect.CollectSpliterators;
import org.spongepowered.include.com.google.common.collect.Hashing;
import org.spongepowered.include.com.google.common.collect.ImmutableAsList;
import org.spongepowered.include.com.google.common.collect.ImmutableCollection;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.include.com.google.common.collect.ObjectArrays;
import org.spongepowered.include.com.google.common.collect.RegularImmutableAsList;
import org.spongepowered.include.com.google.common.collect.RegularImmutableSet;
import org.spongepowered.include.com.google.common.collect.Sets;
import org.spongepowered.include.com.google.common.collect.SingletonImmutableSet;
import org.spongepowered.include.com.google.common.collect.UnmodifiableIterator;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.spongepowered.include.com.google.errorprone.annotations.concurrent.LazyInit;
import org.spongepowered.include.com.google.j2objc.annotations.RetainedWith;

public abstract class ImmutableSet<E>
extends ImmutableCollection<E>
implements Set<E> {
    @LazyInit
    @RetainedWith
    private transient ImmutableList<E> asList;

    public static <E> ImmutableSet<E> of() {
        return RegularImmutableSet.EMPTY;
    }

    public static <E> ImmutableSet<E> of(E element) {
        return new SingletonImmutableSet<E>(element);
    }

    public static <E> ImmutableSet<E> of(E e1, E e2) {
        return ImmutableSet.construct(2, e1, e2);
    }

    public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4) {
        return ImmutableSet.construct(4, e1, e2, e3, e4);
    }

    @SafeVarargs
    public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E ... others) {
        int paramCount = 6;
        Object[] elements = new Object[6 + others.length];
        elements[0] = e1;
        elements[1] = e2;
        elements[2] = e3;
        elements[3] = e4;
        elements[4] = e5;
        elements[5] = e6;
        System.arraycopy(others, 0, elements, 6, others.length);
        return ImmutableSet.construct(elements.length, elements);
    }

    private static <E> ImmutableSet<E> construct(int n, Object ... elements) {
        switch (n) {
            case 0: {
                return ImmutableSet.of();
            }
            case 1: {
                Object elem = elements[0];
                return ImmutableSet.of(elem);
            }
        }
        int tableSize = ImmutableSet.chooseTableSize(n);
        Object[] table = new Object[tableSize];
        int mask = tableSize - 1;
        int hashCode = 0;
        int uniques = 0;
        block4: for (int i = 0; i < n; ++i) {
            Object element = ObjectArrays.checkElementNotNull(elements[i], i);
            int hash = element.hashCode();
            int j = Hashing.smear(hash);
            while (true) {
                int index;
                Object value;
                if ((value = table[index = j & mask]) == null) {
                    elements[uniques++] = element;
                    table[index] = element;
                    hashCode += hash;
                    continue block4;
                }
                if (value.equals(element)) continue block4;
                ++j;
            }
        }
        Arrays.fill(elements, uniques, n, null);
        if (uniques == 1) {
            Object element = elements[0];
            return new SingletonImmutableSet<Object>(element, hashCode);
        }
        if (tableSize != ImmutableSet.chooseTableSize(uniques)) {
            return ImmutableSet.construct(uniques, elements);
        }
        Object[] uniqueElements = uniques < elements.length ? Arrays.copyOf(elements, uniques) : elements;
        return new RegularImmutableSet(uniqueElements, hashCode, table, mask);
    }

    static int chooseTableSize(int setSize) {
        if (setSize < 0x2CCCCCCC) {
            int tableSize = Integer.highestOneBit(setSize - 1) << 1;
            while ((double)tableSize * 0.7 < (double)setSize) {
                tableSize <<= 1;
            }
            return tableSize;
        }
        Preconditions.checkArgument(setSize < 0x40000000, "collection too large");
        return 0x40000000;
    }

    public static <E> ImmutableSet<E> copyOf(E[] elements) {
        switch (elements.length) {
            case 0: {
                return ImmutableSet.of();
            }
            case 1: {
                return ImmutableSet.of(elements[0]);
            }
        }
        return ImmutableSet.construct(elements.length, (Object[])elements.clone());
    }

    ImmutableSet() {
    }

    boolean isHashCodeFast() {
        return false;
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof ImmutableSet && this.isHashCodeFast() && ((ImmutableSet)object).isHashCodeFast() && this.hashCode() != object.hashCode()) {
            return false;
        }
        return Sets.equalsImpl(this, object);
    }

    @Override
    public int hashCode() {
        return Sets.hashCodeImpl(this);
    }

    @Override
    public abstract UnmodifiableIterator<E> iterator();

    @Override
    public ImmutableList<E> asList() {
        ImmutableList<E> result = this.asList;
        return result == null ? (this.asList = this.createAsList()) : result;
    }

    ImmutableList<E> createAsList() {
        return new RegularImmutableAsList(this, this.toArray());
    }

    public static <E> Builder<E> builder() {
        return new Builder();
    }

    public static class Builder<E>
    extends ImmutableCollection.ArrayBasedBuilder<E> {
        public Builder() {
            this(4);
        }

        Builder(int capacity) {
            super(capacity);
        }

        @Override
        @CanIgnoreReturnValue
        public Builder<E> add(E element) {
            super.add((Object)element);
            return this;
        }

        @Override
        @CanIgnoreReturnValue
        public Builder<E> add(E ... elements) {
            super.add(elements);
            return this;
        }

        @Override
        @CanIgnoreReturnValue
        public Builder<E> addAll(Iterable<? extends E> elements) {
            super.addAll(elements);
            return this;
        }

        public ImmutableSet<E> build() {
            ImmutableSet result = ImmutableSet.construct(this.size, this.contents);
            this.size = result.size();
            return result;
        }
    }

    static abstract class Indexed<E>
    extends ImmutableSet<E> {
        Indexed() {
        }

        abstract E get(int var1);

        @Override
        public UnmodifiableIterator<E> iterator() {
            return this.asList().iterator();
        }

        @Override
        public Spliterator<E> spliterator() {
            return CollectSpliterators.indexed(this.size(), 1297, this::get);
        }

        @Override
        public void forEach(Consumer<? super E> consumer) {
            Preconditions.checkNotNull(consumer);
            int n = this.size();
            for (int i = 0; i < n; ++i) {
                consumer.accept(this.get(i));
            }
        }

        @Override
        ImmutableList<E> createAsList() {
            return new ImmutableAsList<E>(){

                @Override
                public E get(int index) {
                    return this.get(index);
                }

                @Override
                Indexed<E> delegateCollection() {
                    return this;
                }
            };
        }
    }
}

