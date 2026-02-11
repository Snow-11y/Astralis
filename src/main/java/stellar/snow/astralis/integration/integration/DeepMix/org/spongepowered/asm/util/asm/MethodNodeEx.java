package org.spongepowered.asm.util.asm;

import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.util.asm.ASM;

public class MethodNodeEx
extends MethodNode {
    private final IMixinInfo owner;
    private final String originalName;

    public MethodNodeEx(int access, String name, String descriptor, String signature, String[] exceptions, IMixinInfo owner) {
        super(ASM.API_VERSION, access, name, descriptor, signature, exceptions);
        this.originalName = name;
        this.owner = owner;
    }

    public String toString() {
        return String.format("%s%s", this.originalName, this.desc);
    }

    public String getQualifiedName() {
        return String.format("%s::%s", this.owner.getName(), this.originalName);
    }

    public String getOriginalName() {
        return this.originalName;
    }

    public IMixinInfo getOwner() {
        return this.owner;
    }

    public static String getName(MethodNode method) {
        return method instanceof MethodNodeEx ? ((MethodNodeEx)method).getOriginalName() : method.name;
    }
}

