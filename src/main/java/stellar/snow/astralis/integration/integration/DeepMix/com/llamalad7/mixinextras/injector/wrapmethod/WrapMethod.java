package com.llamalad7.mixinextras.injector.wrapmethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value={ElementType.METHOD})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface WrapMethod {
    public String[] method();

    public boolean remap() default true;

    public int require() default -1;

    public int expect() default 1;

    public int allow() default -1;
}

