package com.llamalad7.mixinextras.lib.antlr.runtime.misc;

public interface EqualityComparator<T> {
    public int hashCode(T var1);

    public boolean equals(T var1, T var2);
}

