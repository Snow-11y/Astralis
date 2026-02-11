package com.llamalad7.mixinextras.expression;

import com.llamalad7.mixinextras.expression.Definitions;
import com.llamalad7.mixinextras.sugar.Local;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value={ElementType.METHOD})
@Retention(value=RetentionPolicy.CLASS)
@Repeatable(value=Definitions.class)
public @interface Definition {
    public String id();

    public String[] method() default {};

    public String[] field() default {};

    public Class<?>[] type() default {};

    public Local[] local() default {};

    public boolean remap() default true;
}

