package org.spongepowered.include.com.google.common.collect;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Function;
import org.spongepowered.include.com.google.common.base.Objects;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.collect.Iterators;
import org.spongepowered.include.com.google.common.collect.TransformedListIterator;

public final class Lists {
    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList();
    }

    public static <F, T> List<T> transform(List<F> fromList, Function<? super F, ? extends T> function) {
        return fromList instanceof RandomAccess ? new TransformingRandomAccessList<F, T>(fromList, function) : new TransformingSequentialList<F, T>(fromList, function);
    }

    static boolean equalsImpl(List<?> thisList, @Nullable Object other) {
        if (other == Preconditions.checkNotNull(thisList)) {
            return true;
        }
        if (!(other instanceof List)) {
            return false;
        }
        List otherList = (List)other;
        int size = thisList.size();
        if (size != otherList.size()) {
            return false;
        }
        if (thisList instanceof RandomAccess && otherList instanceof RandomAccess) {
            for (int i = 0; i < size; ++i) {
                if (Objects.equal(thisList.get(i), otherList.get(i))) continue;
                return false;
            }
            return true;
        }
        return Iterators.elementsEqual(thisList.iterator(), otherList.iterator());
    }

    static int indexOfImpl(List<?> list, @Nullable Object element) {
        if (list instanceof RandomAccess) {
            return Lists.indexOfRandomAccess(list, element);
        }
        ListIterator<?> listIterator = list.listIterator();
        while (listIterator.hasNext()) {
            if (!Objects.equal(element, listIterator.next())) continue;
            return listIterator.previousIndex();
        }
        return -1;
    }

    private static int indexOfRandomAccess(List<?> list, @Nullable Object element) {
        int size = list.size();
        if (element == null) {
            for (int i = 0; i < size; ++i) {
                if (list.get(i) != null) continue;
                return i;
            }
        } else {
            for (int i = 0; i < size; ++i) {
                if (!element.equals(list.get(i))) continue;
                return i;
            }
        }
        return -1;
    }

    static int lastIndexOfImpl(List<?> list, @Nullable Object element) {
        if (list instanceof RandomAccess) {
            return Lists.lastIndexOfRandomAccess(list, element);
        }
        ListIterator<?> listIterator = list.listIterator(list.size());
        while (listIterator.hasPrevious()) {
            if (!Objects.equal(element, listIterator.previous())) continue;
            return listIterator.nextIndex();
        }
        return -1;
    }

    private static int lastIndexOfRandomAccess(List<?> list, @Nullable Object element) {
        if (element == null) {
            for (int i = list.size() - 1; i >= 0; --i) {
                if (list.get(i) != null) continue;
                return i;
            }
        } else {
            for (int i = list.size() - 1; i >= 0; --i) {
                if (!element.equals(list.get(i))) continue;
                return i;
            }
        }
        return -1;
    }

    private static class TransformingRandomAccessList<F, T>
    extends AbstractList<T>
    implements Serializable,
    RandomAccess {
        final List<F> fromList;
        final Function<? super F, ? extends T> function;

        TransformingRandomAccessList(List<F> fromList, Function<? super F, ? extends T> function) {
            this.fromList = Preconditions.checkNotNull(fromList);
            this.function = Preconditions.checkNotNull(function);
        }

        @Override
        public void clear() {
            this.fromList.clear();
        }

        @Override
        public T get(int index) {
            return this.function.apply(this.fromList.get(index));
        }

        @Override
        public Iterator<T> iterator() {
            return this.listIterator();
        }

        @Override
        public ListIterator<T> listIterator(int index) {
            return new TransformedListIterator<F, T>(this.fromList.listIterator(index)){

                @Override
                T transform(F from) {
                    return function.apply(from);
                }
            };
        }

        @Override
        public boolean isEmpty() {
            return this.fromList.isEmpty();
        }

        @Override
        public boolean removeIf(Predicate<? super T> filter) {
            Preconditions.checkNotNull(filter);
            return this.fromList.removeIf((? super E element) -> filter.test((T)this.function.apply(element)));
        }

        @Override
        public T remove(int index) {
            return this.function.apply(this.fromList.remove(index));
        }

        @Override
        public int size() {
            return this.fromList.size();
        }
    }

    private static class TransformingSequentialList<F, T>
    extends AbstractSequentialList<T>
    implements Serializable {
        final List<F> fromList;
        final Function<? super F, ? extends T> function;

        TransformingSequentialList(List<F> fromList, Function<? super F, ? extends T> function) {
            this.fromList = Preconditions.checkNotNull(fromList);
            this.function = Preconditions.checkNotNull(function);
        }

        @Override
        public void clear() {
            this.fromList.clear();
        }

        @Override
        public int size() {
            return this.fromList.size();
        }

        @Override
        public ListIterator<T> listIterator(int index) {
            return new TransformedListIterator<F, T>(this.fromList.listIterator(index)){

                @Override
                T transform(F from) {
                    return function.apply(from);
                }
            };
        }

        @Override
        public boolean removeIf(Predicate<? super T> filter) {
            Preconditions.checkNotNull(filter);
            return this.fromList.removeIf((? super E element) -> filter.test((T)this.function.apply(element)));
        }
    }
}

