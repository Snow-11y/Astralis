package com.llamalad7.mixinextras.expression;

import com.llamalad7.mixinextras.expression.Expression;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value={ElementType.METHOD})
@Retention(value=RetentionPolicy.CLASS)
public @interface Expressions {
    public Expression[] value();
}

