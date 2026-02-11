package com.llamalad7.mixinextras.injector.wrapoperation;

@FunctionalInterface
public interface Operation<R> {
    public R call(Object ... var1);
}

