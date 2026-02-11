package org.spongepowered.asm.mixin.injection.struct;

import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.util.asm.IAnnotationHandle;

public class SelectorAnnotationContext
implements ISelectorContext {
    private final ISelectorContext parent;
    private final IAnnotationHandle selectorAnnotation;
    private final String selectorCoordinate;

    public SelectorAnnotationContext(ISelectorContext parent, IAnnotationHandle selectorAnnotation, String selectorCoordinate) {
        this.parent = parent;
        this.selectorAnnotation = selectorAnnotation;
        this.selectorCoordinate = selectorCoordinate;
    }

    @Override
    public ISelectorContext getParent() {
        return this.parent;
    }

    @Override
    public IMixinContext getMixin() {
        return this.parent.getMixin();
    }

    @Override
    public Object getMethod() {
        return this.parent.getMethod();
    }

    @Override
    public IAnnotationHandle getAnnotation() {
        return this.parent.getAnnotation();
    }

    @Override
    public IAnnotationHandle getSelectorAnnotation() {
        return this.selectorAnnotation;
    }

    @Override
    public String getSelectorCoordinate(boolean leaf) {
        return this.selectorCoordinate;
    }

    @Override
    public String remap(String reference) {
        return this.parent.remap(reference);
    }

    @Override
    public String getElementDescription() {
        return String.format("%s in %s", this.selectorAnnotation, this.parent.getElementDescription());
    }
}

