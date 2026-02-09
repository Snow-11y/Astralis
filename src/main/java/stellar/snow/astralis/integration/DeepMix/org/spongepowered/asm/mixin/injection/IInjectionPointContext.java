package org.spongepowered.asm.mixin.injection;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.util.IMessageSink;

public interface IInjectionPointContext
extends ISelectorContext,
IMessageSink {
    public MethodNode getMethod();

    public AnnotationNode getAnnotationNode();
}

