package com.llamalad7.mixinextras.expression;

import com.llamalad7.mixinextras.expression.Expressions;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value={ElementType.METHOD})
@Retention(value=RetentionPolicy.CLASS)
@Repeatable(value=Expressions.class)
public @interface Expression {
    public String[] value();

    public String id() default "";
}

