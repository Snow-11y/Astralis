package org.spongepowered.include.com.google.common.base;

import java.util.Arrays;
import javax.annotation.Nullable;
import org.spongepowered.include.com.google.common.base.ExtraObjectsMethodsForWeb;

public final class Objects
extends ExtraObjectsMethodsForWeb {
    public static boolean equal(@Nullable Object a, @Nullable Object b) {
        return a == b || a != null && a.equals(b);
    }

    public static int hashCode(Object ... objects) {
        return Arrays.hashCode(objects);
    }
}

