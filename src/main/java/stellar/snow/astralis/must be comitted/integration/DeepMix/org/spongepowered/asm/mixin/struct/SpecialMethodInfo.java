package org.spongepowered.asm.mixin.struct;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.struct.AnnotatedMethodInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

public class SpecialMethodInfo
extends AnnotatedMethodInfo {
    protected final ClassNode classNode;
    protected final MixinTargetContext mixin;

    public SpecialMethodInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
        this.mixin = mixin;
        this.classNode = mixin.getTargetClassNode();
    }

    @Deprecated
    public final ClassNode getClassNode() {
        return this.classNode;
    }

    public final ClassNode getTargetClassNode() {
        return this.classNode;
    }

    public final ClassInfo getTargetClassInfo() {
        return this.mixin.getTargetClassInfo();
    }

    public final ClassInfo getClassInfo() {
        return this.mixin.getClassInfo();
    }

    @Override
    public String getMethodName() {
        return this.methodName;
    }
}

