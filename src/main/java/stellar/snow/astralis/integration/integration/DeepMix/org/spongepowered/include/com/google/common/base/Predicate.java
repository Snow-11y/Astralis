package org.spongepowered.include.com.google.common.base;

import javax.annotation.Nullable;
import org.spongepowered.include.com.google.errorprone.annotations.CanIgnoreReturnValue;

@FunctionalInterface
public interface Predicate<T>
extends java.util.function.Predicate<T> {
    @CanIgnoreReturnValue
    public boolean apply(@Nullable T var1);

    @Override
    default public boolean test(@Nullable T input) {
        return this.apply(input);
    }
}

