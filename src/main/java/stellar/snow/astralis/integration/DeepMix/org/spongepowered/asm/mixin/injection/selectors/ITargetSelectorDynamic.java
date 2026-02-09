package org.spongepowered.asm.mixin.injection.selectors;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;

public interface ITargetSelectorDynamic
extends ITargetSelector {

    @Retention(value=RetentionPolicy.RUNTIME)
    @Target(value={ElementType.TYPE})
    public static @interface SelectorAnnotation {
        public Class<? extends Annotation> value();
    }

    @Retention(value=RetentionPolicy.RUNTIME)
    @Target(value={ElementType.TYPE})
    public static @interface SelectorId {
        public String namespace() default "";

        public String value();
    }
}

