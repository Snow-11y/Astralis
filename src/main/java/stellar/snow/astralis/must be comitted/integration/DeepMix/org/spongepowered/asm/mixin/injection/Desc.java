package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.spongepowered.asm.mixin.injection.Descriptors;
import org.spongepowered.asm.mixin.injection.Next;

@Target(value={ElementType.TYPE, ElementType.METHOD})
@Retention(value=RetentionPolicy.RUNTIME)
@Repeatable(value=Descriptors.class)
public @interface Desc {
    public String id() default "";

    public Class<?> owner() default void.class;

    public String value();

    public Class<?> ret() default void.class;

    public Class<?>[] args() default {};

    public Next[] next() default {};

    public int min() default 0;

    public int max() default 0x7FFFFFFF;
}

