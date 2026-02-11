package org.spongepowered.include.com.google.common.collect;

import java.io.Serializable;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;
import org.spongepowered.include.com.google.common.collect.Ordering;

final class ReverseOrdering<T>
extends Ordering<T>
implements Serializable {
    final Ordering<? super T> forwardOrder;

    ReverseOrdering(Ordering<? super T> forwardOrder) {
        this.forwardOrder = Preconditions.checkNotNull(forwardOrder);
    }

    @Override
    public int compare(T a, T b) {
        return this.forwardOrder.compare(b, a);
    }

    @Override
    public <S extends T> Ordering<S> reverse() {
        return this.forwardOrder;
    }

    public int hashCode() {
        return -this.forwardOrder.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof ReverseOrdering) {
            ReverseOrdering that = (ReverseOrdering)object;
            return this.forwardOrder.equals(that.forwardOrder);
        }
        return false;
    }

    public String toString() {
        return this.forwardOrder + ".reverse()";
    }
}

