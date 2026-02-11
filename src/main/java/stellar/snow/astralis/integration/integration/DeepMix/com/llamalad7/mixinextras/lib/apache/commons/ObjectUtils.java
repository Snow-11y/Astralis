package com.llamalad7.mixinextras.lib.apache.commons;

import java.io.Serializable;

public class ObjectUtils {
    public static final Null NULL = new Null();

    @Deprecated
    public static boolean equals(Object object1, Object object2) {
        if (object1 == object2) {
            return true;
        }
        if (object1 == null || object2 == null) {
            return false;
        }
        return object1.equals(object2);
    }

    public String toString() {
        return super.toString();
    }

    public static class Null
    implements Serializable {
        Null() {
        }
    }
}

