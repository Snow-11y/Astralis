package org.spongepowered.include.com.google.common.collect;

import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Function;
import org.spongepowered.include.com.google.common.base.Objects;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.base.Predicate;
import org.spongepowered.include.com.google.common.base.Predicates;
import org.spongepowered.include.com.google.common.collect.AbstractIndexedListIterator;
import org.spongepowered.include.com.google.common.collect.CollectPreconditions;
import org.spongepowered.include.com.google.common.collect.Collections2;
import org.spongepowered.include.com.google.common.collect.MultitransformedIterator;
import org.spongepowered.include.com.google.common.collect.TransformedIterator;
import org.spongepowered.include.com.google.common.collect.UnmodifiableIterator;
import org.spongepowered.include.com.google.common.collect.UnmodifiableListIterator;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

public final class Iterators {
    static final UnmodifiableListIterator<Object> EMPTY_LIST_ITERATOR = new UnmodifiableListIterator<Object>(){

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }

        @Override
        public Object previous() {
            throw new NoSuchElementException();
        }

        @Override
        public int nextIndex() {
            return 0;
        }

        @Override
        public int previousIndex() {
            return -1;
        }
    };
    private static final Iterator<Object> EMPTY_MODIFIABLE_ITERATOR = new Iterator<Object>(){

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            CollectPreconditions.checkRemove(false);
        }
    };

    static <T> UnmodifiableIterator<T> emptyIterator() {
        return Iterators.emptyListIterator();
    }

    static <T> UnmodifiableListIterator<T> emptyListIterator() {
        return EMPTY_LIST_ITERATOR;
    }

    public static <T> UnmodifiableIterator<T> unmodifiableIterator(final Iterator<? extends T> iterator) {
        Preconditions.checkNotNull(iterator);
        if (iterator instanceof UnmodifiableIterator) {
            UnmodifiableIterator result = (UnmodifiableIterator)iterator;
            return result;
        }
        return new UnmodifiableIterator<T>(){

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next();
            }
        };
    }

    public static boolean contains(Iterator<?> iterator, @Nullable Object element) {
        return Iterators.any(iterator, Predicates.equalTo(element));
    }

    @CanIgnoreReturnValue
    public static boolean removeAll(Iterator<?> removeFrom, Collection<?> elementsToRemove) {
        return Iterators.removeIf(removeFrom, Predicates.in(elementsToRemove));
    }

    @CanIgnoreReturnValue
    public static <T> boolean removeIf(Iterator<T> removeFrom, Predicate<? super T> predicate) {
        Preconditions.checkNotNull(predicate);
        boolean modified = false;
        while (removeFrom.hasNext()) {
            if (!predicate.apply(removeFrom.next())) continue;
            removeFrom.remove();
            modified = true;
        }
        return modified;
    }

    public static boolean elementsEqual(Iterator<?> iterator1, Iterator<?> iterator2) {
        while (iterator1.hasNext()) {
            Object o2;
            if (!iterator2.hasNext()) {
                return false;
            }
            Object o1 = iterator1.next();
            if (Objects.equal(o1, o2 = iterator2.next())) continue;
            return false;
        }
        return !iterator2.hasNext();
    }

    public static String toString(Iterator<?> iterator) {
        return Collections2.STANDARD_JOINER.appendTo(new StringBuilder().append('['), iterator).append(']').toString();
    }

    public static <T> Iterator<T> concat(Iterator<? extends Iterator<? extends T>> inputs) {
        return new ConcatenatedIterator(inputs);
    }

    public static <T> boolean any(Iterator<T> iterator, Predicate<? super T> predicate) {
        return Iterators.indexOf(iterator, predicate) != -1;
    }

    public static <T> int indexOf(Iterator<T> iterator, Predicate<? super T> predicate) {
        Preconditions.checkNotNull(predicate, "predicate");
        int i = 0;
        while (iterator.hasNext()) {
            T current = iterator.next();
            if (predicate.apply(current)) {
                return i;
            }
            ++i;
        }
        return -1;
    }

    public static <F, T> Iterator<T> transform(Iterator<F> fromIterator, final Function<? super F, ? extends T> function) {
        Preconditions.checkNotNull(function);
        return new TransformedIterator<F, T>(fromIterator){

            @Override
            T transform(F from) {
                return function.apply(from);
            }
        };
    }

    @Nullable
    public static <T> T getNext(Iterator<? extends T> iterator, @Nullable T defaultValue) {
        return iterator.hasNext() ? iterator.next() : defaultValue;
    }

    @Nullable
    static <T> T pollNext(Iterator<T> iterator) {
        if (iterator.hasNext()) {
            T result = iterator.next();
            iterator.remove();
            return result;
        }
        return null;
    }

    static void clear(Iterator<?> iterator) {
        Preconditions.checkNotNull(iterator);
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    @SafeVarargs
    public static <T> UnmodifiableIterator<T> forArray(T ... array) {
        return Iterators.forArray(array, 0, array.length, 0);
    }

    static <T> UnmodifiableListIterator<T> forArray(final T[] array, final int offset, int length, int index) {
        Preconditions.checkArgument(length >= 0);
        int end = offset + length;
        Preconditions.checkPositionIndexes(offset, end, array.length);
        Preconditions.checkPositionIndex(index, length);
        if (length == 0) {
            return Iterators.emptyListIterator();
        }
        return new AbstractIndexedListIterator<T>(length, index){

            @Override
            protected T get(int index) {
                return array[offset + index];
            }
        };
    }

    public static <T> UnmodifiableIterator<T> singletonIterator(final @Nullable T value) {
        return new UnmodifiableIterator<T>(){
            boolean done;

            @Override
            public boolean hasNext() {
                return !this.done;
            }

            @Override
            public T next() {
                if (this.done) {
                    throw new NoSuchElementException();
                }
                this.done = true;
                return value;
            }
        };
    }

    static <T> ListIterator<T> cast(Iterator<T> iterator) {
        return (ListIterator)iterator;
    }

    private static class ConcatenatedIterator<T>
    extends MultitransformedIterator<Iterator<? extends T>, T> {
        public ConcatenatedIterator(Iterator<? extends Iterator<? extends T>> iterators) {
            super(ConcatenatedIterator.getComponentIterators(iterators));
        }

        @Override
        Iterator<? extends T> transform(Iterator<? extends T> iterator) {
            return iterator;
        }

        private static <T> Iterator<Iterator<? extends T>> getComponentIterators(Iterator<? extends Iterator<? extends T>> iterators) {
            return new MultitransformedIterator<Iterator<? extends T>, Iterator<? extends T>>(iterators){

                @Override
                Iterator<? extends Iterator<? extends T>> transform(Iterator<? extends T> iterator) {
                    if (iterator instanceof ConcatenatedIterator) {
                        ConcatenatedIterator concatIterator = (ConcatenatedIterator)iterator;
                        return ConcatenatedIterator.getComponentIterators(concatIterator.backingIterator);
                    }
                    return Iterators.singletonIterator(iterator);
                }
            };
        }
    }
}

