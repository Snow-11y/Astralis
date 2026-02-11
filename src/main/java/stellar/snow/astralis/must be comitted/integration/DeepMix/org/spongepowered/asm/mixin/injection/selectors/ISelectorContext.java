package org.spongepowered.asm.mixin.injection.selectors;

import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.asm.IAnnotationHandle;

public interface ISelectorContext {
    public ISelectorContext getParent();

    public IMixinContext getMixin();

    public Object getMethod();

    public IAnnotationHandle getAnnotation();

    public IAnnotationHandle getSelectorAnnotation();

    public String getSelectorCoordinate(boolean var1);

    public String remap(String var1);

    public String getElementDescription();
}

