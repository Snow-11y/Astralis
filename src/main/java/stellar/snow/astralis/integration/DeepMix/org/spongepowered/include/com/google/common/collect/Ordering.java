package org.spongepowered.include.com.google.common.collect;

import java.util.Comparator;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Function;
import org.spongepowered.include.com.google.common.collect.ByFunctionOrdering;
import org.spongepowered.include.com.google.common.collect.ComparatorOrdering;
import org.spongepowered.include.com.google.common.collect.ReverseOrdering;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

public abstract class Ordering<T>
implements Comparator<T> {
    public static <T> Ordering<T> from(Comparator<T> comparator) {
        return comparator instanceof Ordering ? (Ordering<T>)comparator : new ComparatorOrdering<T>(comparator);
    }

    protected Ordering() {
    }

    public <S extends T> Ordering<S> reverse() {
        return new ReverseOrdering(this);
    }

    public <F> Ordering<F> onResultOf(Function<F, ? extends T> function) {
        return new ByFunctionOrdering<F, T>(function, this);
    }

    @Override
    @CanIgnoreReturnValue
    public abstract int compare(@Nullable T var1, @Nullable T var2);
}

