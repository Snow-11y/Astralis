package org.spongepowered.tools.obfuscation.mirror;

import org.objectweb.asm.tree.FieldNode;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.tools.obfuscation.mirror.FieldHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

public class FieldHandleASM
extends FieldHandle {
    private final FieldNode field;

    public FieldHandleASM(TypeHandle owner, FieldNode field) {
        super(owner, field.name, field.desc);
        this.field = field;
    }

    @Override
    public Bytecode.Visibility getVisibility() {
        return Bytecode.getVisibility(this.field);
    }
}

