package org.spongepowered.include.com.google.common.collect;

import java.lang.reflect.Array;

final class Platform {
    static <T> T[] newArray(T[] reference, int length) {
        Class<?> type = reference.getClass().getComponentType();
        Object[] result = (Object[])Array.newInstance(type, length);
        return result;
    }
}

