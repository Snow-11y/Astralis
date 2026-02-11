package org.spongepowered.asm.mixin.transformer.ext;

import org.objectweb.asm.tree.ClassNode;

public interface IHotSwap {
    public void registerMixinClass(String var1);

    public void registerTargetClass(String var1, ClassNode var2);
}

