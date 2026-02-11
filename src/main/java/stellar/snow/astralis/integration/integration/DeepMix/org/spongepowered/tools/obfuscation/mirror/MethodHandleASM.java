package org.spongepowered.tools.obfuscation.mirror;

import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.tools.obfuscation.mirror.MethodHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeUtils;

public class MethodHandleASM
extends MethodHandle {
    private final MethodNode method;

    public MethodHandleASM(TypeHandle owner, MethodNode method) {
        super(owner, method.name, method.desc);
        this.method = method;
    }

    @Override
    public String getJavaSignature() {
        return TypeUtils.getJavaSignature(this.method.desc);
    }

    @Override
    public Bytecode.Visibility getVisibility() {
        return Bytecode.getVisibility(this.method);
    }
}

