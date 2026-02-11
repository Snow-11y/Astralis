package com.llamalad7.mixinextras.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.stream.Collectors;

interface InternalConstructor<T> {
    public T newInstance(Object ... var1);

    public static <T> InternalConstructor<T> of(Class<?> clazz, Class<?> ... argTypes) {
        Constructor<?> impl;
        try {
            impl = clazz.getDeclaredConstructor(argTypes);
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(String.format("Failed to find constructor %s(%s)! Please report to LlamaLad7!", clazz, Arrays.stream(argTypes).map(Class::getName).collect(Collectors.joining(", "))), e);
        }
        impl.setAccessible(true);
        return args -> {
            try {
                return impl.newInstance(args);
            }
            catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new RuntimeException(String.format("Failed to construct %s(%s) with args [%s]! Please report to LlamaLad7!", clazz, Arrays.stream(argTypes).map(Class::getName).collect(Collectors.joining(", ")), Arrays.stream(args).map(Object::toString).collect(Collectors.joining(", "))), e);
            }
        };
    }

    public static <T> InternalConstructor<T> of(String clazz, Class<?> ... argTypes) {
        try {
            return InternalConstructor.of(Class.forName(clazz), argTypes);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format("Failed to find class %s! Please report to LlamaLad7!", clazz), e);
        }
    }
}

