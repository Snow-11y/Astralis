package org.spongepowered.include.com.google.common.collect;

import java.util.Set;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.collect.ForwardingCollection;

public abstract class ForwardingSet<E>
extends ForwardingCollection<E>
implements Set<E> {
    protected ForwardingSet() {
    }

    @Override
    protected abstract Set<E> delegate();

    @Override
    public boolean equals(@Nullable Object object) {
        return object == this || this.delegate().equals(object);
    }

    @Override
    public int hashCode() {
        return this.delegate().hashCode();
    }
}

