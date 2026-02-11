package org.spongepowered.asm.mixin.injection.code;

import org.objectweb.asm.tree.AbstractInsnNode;

public interface IInsnListEx {
    public String getTargetName();

    public String getTargetDesc();

    public String getTargetSignature();

    public int getTargetAccess();

    public boolean isTargetStatic();

    public boolean isTargetConstructor();

    public boolean isTargetStaticInitialiser();

    public AbstractInsnNode getSpecialNode(SpecialNodeType var1);

    public boolean hasDecoration(String var1);

    public <V> V getDecoration(String var1);

    public <V> V getDecoration(String var1, V var2);

    public static enum SpecialNodeType {
        DELEGATE_CTOR,
        INITIALISER_INJECTION_POINT,
        CTOR_BODY;

    }
}

