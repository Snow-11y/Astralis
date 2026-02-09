package org.spongepowered.asm.mixin.injection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.spongepowered.asm.mixin.injection.At;

@Target(value={})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface Slice {
    public String id() default "";

    public At from() default @At(value="HEAD");

    public At to() default @At(value="TAIL");
}

