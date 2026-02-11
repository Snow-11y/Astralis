package com.llamalad7.mixinextras.expression.impl.pool;

import com.llamalad7.mixinextras.expression.impl.pool.SimpleMemberDefinition;
import org.objectweb.asm.tree.AbstractInsnNode;

class ArrayLengthDef
implements SimpleMemberDefinition {
    ArrayLengthDef() {
    }

    @Override
    public boolean matches(AbstractInsnNode insn) {
        return insn.getOpcode() == 190;
    }
}

