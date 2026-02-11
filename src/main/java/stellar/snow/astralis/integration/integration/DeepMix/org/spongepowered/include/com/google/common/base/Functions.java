package org.spongepowered.include.com.google.common.base;

import org.spongepowered.include.com.google.common.base.Function;
import org.spongepowered.include.com.google.common.base.Preconditions;

public final class Functions {
    public static Function<Object, String> toStringFunction() {
        return ToStringFunction.INSTANCE;
    }

    private static enum ToStringFunction implements Function<Object, String>
    {
        INSTANCE;


        @Override
        public String apply(Object o) {
            Preconditions.checkNotNull(o);
            return o.toString();
        }

        public String toString() {
            return "Functions.toStringFunction()";
        }
    }
}

