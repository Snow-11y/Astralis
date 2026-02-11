package org.spongepowered.asm.mixin.gen;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.spongepowered.asm.mixin.gen.AccessorGenerator;
import org.spongepowered.asm.mixin.gen.AccessorInfo;
import org.spongepowered.asm.util.Bytecode;

public abstract class AccessorGeneratorField
extends AccessorGenerator {
    protected final FieldNode targetField;
    protected final Type targetType;

    public AccessorGeneratorField(AccessorInfo info) {
        super(info, Bytecode.isStatic(info.getTargetField()));
        this.targetField = info.getTargetField();
        this.targetType = info.getTargetFieldType();
        this.checkModifiers();
    }
}

