package org.spongepowered.include.com.google.common.base;

import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.Platform;
import org.spongepowered.include.com.google.common.base.Preconditions;

public final class Strings {
    public static String nullToEmpty(@Nullable String string) {
        return string == null ? "" : string;
    }

    @Nullable
    public static String emptyToNull(@Nullable String string) {
        return Strings.isNullOrEmpty(string) ? null : string;
    }

    public static boolean isNullOrEmpty(@Nullable String string) {
        return Platform.stringIsNullOrEmpty(string);
    }

    public static String padStart(String string, int minLength, char padChar) {
        Preconditions.checkNotNull(string);
        if (string.length() >= minLength) {
            return string;
        }
        StringBuilder sb = new StringBuilder(minLength);
        for (int i = string.length(); i < minLength; ++i) {
            sb.append(padChar);
        }
        sb.append(string);
        return sb.toString();
    }

    public static String repeat(String string, int count) {
        int n;
        Preconditions.checkNotNull(string);
        if (count <= 1) {
            Preconditions.checkArgument(count >= 0, "invalid count: %s", count);
            return count == 0 ? "" : string;
        }
        int len = string.length();
        long longSize = (long)len * (long)count;
        int size = (int)longSize;
        if ((long)size != longSize) {
            throw new ArrayIndexOutOfBoundsException("Required array size too large: " + longSize);
        }
        char[] array = new char[size];
        string.getChars(0, len, array, 0);
        for (n = len; n < size - n; n <<= 1) {
            System.arraycopy(array, 0, array, n, n);
        }
        System.arraycopy(array, 0, array, n, size - n);
        return new String(array);
    }
}

