package org.spongepowered.include.com.google.common.primitives;

public final class Chars {
    public static boolean contains(char[] array, char target) {
        for (char value : array) {
            if (value != target) continue;
            return true;
        }
        return false;
    }
}

