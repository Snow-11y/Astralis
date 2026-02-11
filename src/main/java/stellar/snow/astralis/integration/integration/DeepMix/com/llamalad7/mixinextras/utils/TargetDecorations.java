package com.llamalad7.mixinextras.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.spongepowered.asm.mixin.injection.struct.Target;

public class TargetDecorations {
    private static final Map<Target, Map<String, Object>> IMPL = new WeakHashMap<Target, Map<String, Object>>();

    public static boolean has(Target target, String key) {
        return IMPL.containsKey(target) && IMPL.get(target).containsKey(key);
    }

    public static <T> T get(Target target, String key) {
        return (T)(IMPL.containsKey(target) ? IMPL.get(target).get(key) : null);
    }

    public static <T> T getOrPut(Target target, String key, Supplier<T> supplier) {
        return (T)IMPL.computeIfAbsent(target, k -> new HashMap()).computeIfAbsent(key, k -> supplier.get());
    }

    public static void put(Target target, String key, Object value) {
        IMPL.computeIfAbsent(target, k -> new HashMap()).put(key, value);
    }

    public static <T> void modify(Target target, String key, UnaryOperator<T> operator) {
        IMPL.computeIfAbsent(target, k -> new HashMap()).put(key, operator.apply(TargetDecorations.get(target, key)));
    }

    public static void remove(Target target, String key) {
        if (IMPL.containsKey(target)) {
            IMPL.get(target).remove(key);
        }
    }
}

