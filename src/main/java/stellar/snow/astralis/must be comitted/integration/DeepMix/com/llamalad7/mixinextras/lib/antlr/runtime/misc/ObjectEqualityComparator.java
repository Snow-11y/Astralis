package com.llamalad7.mixinextras.lib.antlr.runtime.misc;

import com.llamalad7.mixinextras.lib.antlr.runtime.misc.AbstractEqualityComparator;

public final class ObjectEqualityComparator
extends AbstractEqualityComparator<Object> {
    public static final ObjectEqualityComparator INSTANCE = new ObjectEqualityComparator();

    @Override
    public int hashCode(Object obj) {
        if (obj == null) {
            return 0;
        }
        return obj.hashCode();
    }

    @Override
    public boolean equals(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}

