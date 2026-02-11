package org.spongepowered.asm.mixin.transformer.ext;

import org.objectweb.asm.tree.ClassNode;

public interface IClassGenerator {
    public String getName();

    public boolean generate(String var1, ClassNode var2);
}

