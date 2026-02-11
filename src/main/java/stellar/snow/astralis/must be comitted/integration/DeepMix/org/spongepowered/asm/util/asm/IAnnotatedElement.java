package org.spongepowered.asm.util.asm;

import java.lang.annotation.Annotation;
import org.spongepowered.asm.util.asm.IAnnotationHandle;

public interface IAnnotatedElement {
    public IAnnotationHandle getAnnotation(Class<? extends Annotation> var1);
}

