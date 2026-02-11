package org.spongepowered.asm.mixin.transformer.ext;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

public interface ITargetClassContext {
    public ClassInfo getClassInfo();

    public ClassNode getClassNode();
}

