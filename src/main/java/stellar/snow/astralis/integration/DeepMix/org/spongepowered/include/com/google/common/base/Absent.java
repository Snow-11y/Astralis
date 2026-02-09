package org.spongepowered.include.com.google.common.base;

import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Optional;
import org.spongepowered.include.com.google.common.base.Preconditions;

final class Absent<T>
extends Optional<T> {
    static final Absent<Object> INSTANCE = new Absent();

    static <T> Optional<T> withType() {
        return INSTANCE;
    }

    private Absent() {
    }

    @Override
    public T or(T defaultValue) {
        return Preconditions.checkNotNull(defaultValue, "use Optional.orNull() instead of Optional.or(null)");
    }

    public boolean equals(@Nullable Object object) {
        return object == this;
    }

    public int hashCode() {
        return 2040732332;
    }

    public String toString() {
        return "Optional.absent()";
    }
}

