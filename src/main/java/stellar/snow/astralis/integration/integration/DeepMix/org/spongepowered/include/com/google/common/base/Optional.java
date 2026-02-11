package org.spongepowered.include.com.google.common.base;

import java.io.Serializable;
import org.spongepowered.include.com.google.common.base.Absent;

public abstract class Optional<T>
implements Serializable {
    public static <T> Optional<T> absent() {
        return Absent.withType();
    }

    Optional() {
    }

    public abstract T or(T var1);
}

