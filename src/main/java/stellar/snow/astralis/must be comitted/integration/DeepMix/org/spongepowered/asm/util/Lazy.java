package org.spongepowered.asm.util;

import java.util.function.Supplier;

public final class Lazy {
    private Object value;

    public static Lazy of(Object value) {
        return new Lazy(value);
    }

    private Lazy(Object value) {
        this.value = value;
    }

    public <T> T get() {
        if (this.value instanceof Supplier) {
            this.value = ((Supplier)this.value).get();
        }
        return (T)this.value;
    }
}

