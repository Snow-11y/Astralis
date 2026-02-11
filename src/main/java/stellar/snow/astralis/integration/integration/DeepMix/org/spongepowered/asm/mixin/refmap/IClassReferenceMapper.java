package org.spongepowered.asm.mixin.refmap;

public interface IClassReferenceMapper {
    public String remapClassName(String var1, String var2);

    public String remapClassNameWithContext(String var1, String var2, String var3);
}

