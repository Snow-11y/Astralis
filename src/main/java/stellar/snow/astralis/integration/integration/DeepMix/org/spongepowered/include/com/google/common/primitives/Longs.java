package org.spongepowered.include.com.google.common.primitives;

import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;

public final class Longs {
    private static final byte[] asciiDigits = Longs.createAsciiDigits();

    private static byte[] createAsciiDigits() {
        int i;
        byte[] result = new byte[128];
        Arrays.fill(result, (byte)-1);
        for (i = 0; i <= 9; ++i) {
            result[48 + i] = (byte)i;
        }
        for (i = 0; i <= 26; ++i) {
            result[65 + i] = (byte)(10 + i);
            result[97 + i] = (byte)(10 + i);
        }
        return result;
    }

    private static int digit(char c) {
        return c < '\u0080' ? asciiDigits[c] : -1;
    }

    @Nullable
    @CheckForNull
    public static Long tryParse(String string) {
        return Longs.tryParse(string, 10);
    }

    @Nullable
    @CheckForNull
    public static Long tryParse(String string, int radix) {
        int digit;
        int index;
        if (Preconditions.checkNotNull(string).isEmpty()) {
            return null;
        }
        if (radix < 2 || radix > 36) {
            throw new IllegalArgumentException("radix must be between MIN_RADIX and MAX_RADIX but was " + radix);
        }
        boolean negative = string.charAt(0) == '-';
        int n = index = negative ? 1 : 0;
        if (index == string.length()) {
            return null;
        }
        if ((digit = Longs.digit(string.charAt(index++))) < 0 || digit >= radix) {
            return null;
        }
        long accum = -digit;
        long cap = Long.MIN_VALUE / (long)radix;
        while (index < string.length()) {
            if ((digit = Longs.digit(string.charAt(index++))) < 0 || digit >= radix || accum < cap) {
                return null;
            }
            if ((accum *= (long)radix) < Long.MIN_VALUE + (long)digit) {
                return null;
            }
            accum -= (long)digit;
        }
        if (negative) {
            return accum;
        }
        if (accum == Long.MIN_VALUE) {
            return null;
        }
        return -accum;
    }
}

