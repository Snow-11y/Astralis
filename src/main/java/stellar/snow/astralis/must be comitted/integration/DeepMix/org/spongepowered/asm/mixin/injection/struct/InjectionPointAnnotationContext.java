package org.spongepowered.asm.mixin.injection.struct;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.IInjectionPointContext;
import org.spongepowered.asm.mixin.injection.struct.SelectorAnnotationContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.asm.IAnnotationHandle;

public class InjectionPointAnnotationContext
extends SelectorAnnotationContext
implements IInjectionPointContext {
    private IInjectionPointContext parentContext;

    public InjectionPointAnnotationContext(IInjectionPointContext parent, IAnnotationHandle selectorAnnotation, String selectorCoordinate) {
        super(parent, selectorAnnotation, selectorCoordinate);
        this.parentContext = parent;
    }

    public InjectionPointAnnotationContext(IInjectionPointContext parent, AnnotationNode selectorAnnotation, String selectorCoordinate) {
        super(parent, Annotations.handleOf(selectorAnnotation), selectorCoordinate);
        this.parentContext = parent;
    }

    @Override
    public void addMessage(String format, Object ... args) {
        this.parentContext.addMessage(format, args);
    }

    @Override
    public MethodNode getMethod() {
        return this.parentContext.getMethod();
    }

    @Override
    public AnnotationNode getAnnotationNode() {
        return this.parentContext.getAnnotationNode();
    }

    @Override
    public IAnnotationHandle getAnnotation() {
        return this.parentContext.getAnnotation();
    }

    public String toString() {
        return String.format("%s->%s(%s)", this.parentContext, this.getSelectorAnnotation(), this.getSelectorCoordinate(false));
    }
}

